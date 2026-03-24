package com.shop.order.service

import com.shop.order.config.ORDER_QUEUE
import com.shop.order.config.rabbitChannel
import com.shop.order.config.redisConnection
import com.shop.order.domain.models.*
import com.shop.order.repository.OrderRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

data class OrderStats(
    val total: Long,
    val revenue: BigDecimal,
    val pending: Long,
    val cancelled: Long
)

class OrderService(
    private val repository: OrderRepository,
    private val productServiceUrl: String
) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val CACHE_TTL = 300L

    fun createOrder(userId: Int, request: CreateOrderRequest): OrderResponse {
        if (request.items.isEmpty())
            throw IllegalArgumentException("Order must have at least one item")

        val productInfos = request.items.map { item ->
            runBlocking {
                httpClient.get("$productServiceUrl/products/${item.productId}")
                    .body<ProductInfo>()
            }
        }

        request.items.forEachIndexed { index, item ->
            val product = productInfos[index]
            if (product.stock < item.quantity)
                throw IllegalStateException("Insufficient stock for product ${product.name}")
        }

        val total = request.items.mapIndexed { index, item ->
            BigDecimal.valueOf(productInfos[index].price) * BigDecimal(item.quantity)
        }.reduce { acc, it -> acc + it }

        val orderItems = request.items.mapIndexed { index, item ->
            Triple(
                item.productId,
                productInfos[index].name,
                Pair(BigDecimal.valueOf(productInfos[index].price), item.quantity)
            )
        }

        val order = repository.create(userId, orderItems, total)

        repository.logAudit(
            userId = userId,
            action = "CREATE_ORDER",
            entityType = "order",
            entityId = order.id,
            details = "Order created with ${request.items.size} items, total: $total"
        )

        publishEvent("ORDER_CREATED", order.id, userId, total.toString())

        redisConnection.sync().setex(
            "order:${order.id}",
            CACHE_TTL,
            json.encodeToString(order)
        )

        return order
    }

    fun getUserOrders(userId: Int): List<OrderResponse> = repository.findByUserId(userId)

    fun cancelOrder(orderId: Int, userId: Int): Boolean {
        val order = repository.findById(orderId)
            ?: throw NoSuchElementException("Order not found")
        if (order.userId != userId) throw SecurityException("Access denied")

        val cancelled = repository.cancel(orderId, userId)
        if (cancelled) {
            repository.logAudit(userId, "CANCEL_ORDER", "order", orderId, "Order cancelled")
            publishEvent("ORDER_CANCELLED", orderId, userId, null)
            redisConnection.sync().del("order:$orderId")
        }
        return cancelled
    }

    fun getStats(): StatsResponse {
        val stats = repository.getStats()
        return StatsResponse(stats.total, stats.revenue.toDouble(), stats.pending, stats.cancelled)
    }

    private fun publishEvent(event: String, orderId: Int, userId: Int, extra: String?) {
        val payload = buildMap {
            put("event", event)
            put("orderId", orderId.toString())
            put("userId", userId.toString())
            extra?.let { put("extra", it) }
        }
        val message = json.encodeToString(payload)
        rabbitChannel.basicPublish("", ORDER_QUEUE, null, message.toByteArray())
        logger.info("Published $event for order $orderId")
    }
}