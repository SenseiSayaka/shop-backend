package com.shop.order.repository

import com.shop.order.domain.models.OrderItemResponse
import com.shop.order.domain.models.OrderResponse
import com.shop.order.domain.tables.AuditLogs
import com.shop.order.domain.tables.OrderItems
import com.shop.order.domain.tables.Orders
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant

data class OrderStats(
    val total: Long,
    val revenue: BigDecimal,
    val pending: Long,
    val cancelled: Long
)

interface OrderRepository {
    fun create(userId: Int, items: List<Triple<Int, String, Pair<BigDecimal, Int>>>, total: BigDecimal): OrderResponse
    fun findByUserId(userId: Int): List<OrderResponse>
    fun findById(id: Int): OrderResponse?
    fun cancel(orderId: Int, userId: Int): Boolean
    fun logAudit(userId: Int?, action: String, entityType: String, entityId: Int?, details: String?)
    fun getStats(): OrderStats
}

class OrderRepositoryImpl : OrderRepository {

    override fun create(
        userId: Int,
        items: List<Triple<Int, String, Pair<BigDecimal, Int>>>,
        total: BigDecimal
    ): OrderResponse = transaction {
        val now = Instant.now()
        val orderId = Orders.insert {
            it[Orders.userId] = userId
            it[Orders.status] = "pending"
            it[Orders.totalAmount] = total
            it[Orders.createdAt] = now
            it[Orders.updatedAt] = now
        } get Orders.id

        val itemResponses = items.map { (productId, productName, priceAndQty) ->
            val (price, quantity) = priceAndQty
            OrderItems.insert {
                it[OrderItems.orderId] = orderId
                it[OrderItems.productId] = productId
                it[OrderItems.productName] = productName
                it[OrderItems.quantity] = quantity
                it[OrderItems.price] = price
            }
            OrderItemResponse(productId, productName, quantity, price.toDouble())
        }

        OrderResponse(orderId, userId, "pending", total.toDouble(), itemResponses)
    }

    override fun findByUserId(userId: Int): List<OrderResponse> = transaction {
        val orders = Orders.select { Orders.userId eq userId }.toList()
        orders.map { order ->
            val orderId = order[Orders.id]
            val items = OrderItems.select { OrderItems.orderId eq orderId }
                .map {
                    OrderItemResponse(
                        it[OrderItems.productId],
                        it[OrderItems.productName],
                        it[OrderItems.quantity],
                        it[OrderItems.price].toDouble()
                    )
                }
            OrderResponse(
                orderId,
                order[Orders.userId],
                order[Orders.status],
                order[Orders.totalAmount].toDouble(),
                items
            )
        }
    }

    override fun findById(id: Int): OrderResponse? = transaction {
        val order = Orders.select { Orders.id eq id }.singleOrNull() ?: return@transaction null
        val items = OrderItems.select { OrderItems.orderId eq id }
            .map {
                OrderItemResponse(
                    it[OrderItems.productId],
                    it[OrderItems.productName],
                    it[OrderItems.quantity],
                    it[OrderItems.price].toDouble()
                )
            }
        OrderResponse(id, order[Orders.userId], order[Orders.status], order[Orders.totalAmount].toDouble(), items)
    }

    override fun cancel(orderId: Int, userId: Int): Boolean = transaction {
        val order = Orders.select { (Orders.id eq orderId) and (Orders.userId eq userId) }
            .singleOrNull() ?: return@transaction false

        if (order[Orders.status] == "cancelled") return@transaction false

        Orders.update({ Orders.id eq orderId }) {
            it[status] = "cancelled"
            it[updatedAt] = Instant.now()
        } > 0
    }

    override fun logAudit(
        userId: Int?,
        action: String,
        entityType: String,
        entityId: Int?,
        details: String?
    ) {
        transaction {
            AuditLogs.insert {
                it[AuditLogs.userId] = userId
                it[AuditLogs.action] = action
                it[AuditLogs.entityType] = entityType
                it[AuditLogs.entityId] = entityId
                it[AuditLogs.details] = details
                it[createdAt] = Instant.now()
            }
        }
    }

    override fun getStats(): OrderStats = transaction {
        val total = Orders.selectAll().count()
        val revenue = Orders
            .slice(Orders.totalAmount.sum())
            .selectAll()
            .single()[Orders.totalAmount.sum()] ?: BigDecimal.ZERO
        val pending = Orders.select { Orders.status eq "pending" }.count()
        val cancelled = Orders.select { Orders.status eq "cancelled" }.count()
        OrderStats(total, revenue, pending, cancelled)
    }
}