package com.shop.order.config

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import io.ktor.server.application.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

lateinit var rabbitChannel: Channel
lateinit var rabbitConnection: Connection

const val ORDER_QUEUE = "order.events"
const val STATS_QUEUE = "stats.events"

fun Application.configureRabbitMQ() {
    val host = environment.config.property("rabbitmq.host").getString()
    val port = environment.config.property("rabbitmq.port").getString().toInt()
    val user = environment.config.property("rabbitmq.user").getString()
    val password = environment.config.property("rabbitmq.password").getString()

    val factory = ConnectionFactory().apply {
        this.host = host
        this.port = port
        username = user
        this.password = password
    }

    rabbitConnection = factory.newConnection()
    rabbitChannel = rabbitConnection.createChannel()

    rabbitChannel.queueDeclare(ORDER_QUEUE, true, false, false, null)
    rabbitChannel.queueDeclare(STATS_QUEUE, true, false, false, null)

    log.info("RabbitMQ connected to $host:$port")
}