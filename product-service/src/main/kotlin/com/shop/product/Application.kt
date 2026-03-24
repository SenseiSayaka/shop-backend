package com.shop.product

import com.shop.product.config.*
import com.shop.product.plugins.configureSwagger
import com.shop.product.routes.productRoutes
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
    configureRedis()

    routing {
        productRoutes()
    }
}