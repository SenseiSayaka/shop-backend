package com.shop.order

import com.shop.order.config.*
import com.shop.order.plugins.configureSwagger
import com.shop.order.routes.orderRoutes
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    configureDatabase()
    configureSecurity()
    configureStatusPages()
    configureSwagger()
    configureRabbitMQ()
    configureRedis()

    routing {
        orderRoutes()
    }
}