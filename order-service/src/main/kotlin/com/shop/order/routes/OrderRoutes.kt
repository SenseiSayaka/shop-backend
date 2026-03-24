package com.shop.order.routes

import com.shop.order.domain.models.CreateOrderRequest
import com.shop.order.repository.OrderRepositoryImpl
import com.shop.order.service.OrderService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.orderRoutes() {
    val productServiceUrl = application.environment.config
        .property("product.service.url").getString()
    val service = OrderService(OrderRepositoryImpl(), productServiceUrl)

    authenticate("auth-jwt") {
        route("/orders") {
            post {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()
                val request = call.receive<CreateOrderRequest>()

                try {
                    val order = service.createOrder(userId, request)
                    call.respond(HttpStatusCode.Created, order)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: IllegalStateException) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to e.message))
                }
            }
            get("/health") {
                call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
            }

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()
                val orders = service.getUserOrders(userId)
                call.respond(HttpStatusCode.OK, orders)
            }

            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()
                val orderId = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ID")

                try {
                    if (service.cancelOrder(orderId, userId))
                        call.respond(HttpStatusCode.NoContent)
                    else
                        call.respond(HttpStatusCode.NotFound, "Order not found or already cancelled")
                } catch (e: NoSuchElementException) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
                } catch (e: SecurityException) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to e.message))
                }
            }
        }

        route("/stats") {
            get("/orders") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role != "admin")
                    return@get call.respond(HttpStatusCode.Forbidden, "Admin only")

                val stats = service.getStats()
                call.respond(HttpStatusCode.OK, stats)
            }
        }
    }
}