package com.shop.product.routes

import com.shop.product.domain.models.CreateProductRequest
import com.shop.product.domain.models.UpdateProductRequest
import com.shop.product.repository.ProductRepositoryImpl
import com.shop.product.service.ProductService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.productRoutes() {
    val service = ProductService(ProductRepositoryImpl())

    route("/products") {
        // Публичные маршруты
        get {
            val products = service.getAllProducts()
            call.respond(HttpStatusCode.OK, products)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID")

            val product = service.getProductById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Product not found")

            call.respond(HttpStatusCode.OK, product)
        }
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }

        // Админские маршруты
        authenticate("auth-jwt") {
            post {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                if (role != "admin")
                    return@post call.respond(HttpStatusCode.Forbidden, "Admin only")

                val request = call.receive<CreateProductRequest>()
                val product = service.createProduct(request)
                call.respond(HttpStatusCode.Created, product)
            }

            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                if (role != "admin")
                    return@put call.respond(HttpStatusCode.Forbidden, "Admin only")

                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID")

                val request = call.receive<UpdateProductRequest>()
                val product = service.updateProduct(id, request)
                    ?: return@put call.respond(HttpStatusCode.NotFound, "Product not found")

                call.respond(HttpStatusCode.OK, product)
            }

            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                if (role != "admin")
                    return@delete call.respond(HttpStatusCode.Forbidden, "Admin only")

                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ID")

                if (service.deleteProduct(id))
                    call.respond(HttpStatusCode.NoContent)
                else
                    call.respond(HttpStatusCode.NotFound, "Product not found")
            }
        }
    }
}