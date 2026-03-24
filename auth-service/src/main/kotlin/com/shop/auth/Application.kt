package com.shop.auth

import com.shop.auth.config.configureDatabase
import com.shop.auth.config.configureSecurity
import com.shop.auth.config.configureSerialization
import com.shop.auth.config.configureStatusPages
import com.shop.auth.plugins.configureSwagger
import com.shop.auth.routes.authRoutes
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

    routing {
        authRoutes()
    }
}