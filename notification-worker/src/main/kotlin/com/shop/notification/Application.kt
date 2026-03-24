package com.shop.notification

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private val json = Json { ignoreUnknownKeys = true }

const val ORDER_QUEUE = "order.events"

fun main() {
    val host = System.getenv("RABBITMQ_HOST") ?: "localhost"
    val port = System.getenv("RABBITMQ_PORT")?.toInt() ?: 5672
    val user = System.getenv("RABBITMQ_USER") ?: "guest"
    val password = System.getenv("RABBITMQ_PASSWORD") ?: "guest"

    val factory = ConnectionFactory().apply {
        this.host = host
        this.port = port
        username = user
        this.password = password
    }

    logger.info("Notification worker starting, connecting to RabbitMQ at $host:$port")

    val connection = factory.newConnection()
    val channel = connection.createChannel()

    channel.queueDeclare(ORDER_QUEUE, true, false, false, null)
    channel.basicQos(1)

    logger.info("Waiting for messages in queue: $ORDER_QUEUE")

    val deliverCallback = DeliverCallback { _, delivery ->
        val message = String(delivery.body)
        logger.info("Received message: $message")

        try {
            val payload = json.parseToJsonElement(message).jsonObject
            val event = payload["event"]?.jsonPrimitive?.content
            val orderId = payload["orderId"]?.jsonPrimitive?.content
            val userId = payload["userId"]?.jsonPrimitive?.content

            when (event) {
                "ORDER_CREATED" -> {
                    logger.info("Processing ORDER_CREATED: orderId=$orderId, userId=$userId")
                    sendEmailStub(userId, orderId, "Your order #$orderId has been created!")
                }
                "ORDER_CANCELLED" -> {
                    logger.info("Processing ORDER_CANCELLED: orderId=$orderId, userId=$userId")
                    sendEmailStub(userId, orderId, "Your order #$orderId has been cancelled.")
                }
                else -> logger.warn("Unknown event type: $event")
            }

            channel.basicAck(delivery.envelope.deliveryTag, false)
        } catch (e: Exception) {
            logger.error("Error processing message: ${e.message}", e)
            channel.basicNack(delivery.envelope.deliveryTag, false, true)
        }
    }

    channel.basicConsume(ORDER_QUEUE, false, deliverCallback) { _ -> }

    // Держим воркер живым
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down notification worker")
        channel.close()
        connection.close()
    })

    Thread.currentThread().join()
}

fun sendEmailStub(userId: String?, orderId: String?, message: String) {
    // Заглушка для email
    logger.info(
        """
        ========== EMAIL STUB ==========
        To: user_${userId}@example.com
        Subject: Order Update
        Body: $message
        Order ID: $orderId
        ================================
        """.trimIndent()
    )
}