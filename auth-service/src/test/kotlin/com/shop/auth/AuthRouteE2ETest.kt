package com.shop.auth

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * E2E тесты — используют testApplication (встроенный сервер Ktor)
 * БД поднимается через TestContainers внутри module()
 */
class AuthRouteE2ETest {

    companion object {
        // Стартуем контейнер один раз для всех тестов
        val postgres by lazy {
            org.testcontainers.containers.PostgreSQLContainer("postgres:15-alpine")
                .apply {
                    withDatabaseName("e2e_test")
                    withUsername("test")
                    withPassword("test")
                    start()
                }
        }
    }

    private fun buildConfig(): MapApplicationConfig {
        val pg = postgres // запускает контейнер если не запущен
        return MapApplicationConfig(
            "db.url" to pg.jdbcUrl,
            "db.user" to pg.username,
            "db.password" to pg.password,
            "jwt.secret" to "test-secret-key-at-least-32-characters!!",
            "jwt.issuer" to "test-issuer",
            "jwt.audience" to "test-audience"
        )
    }

    @Test
    fun `POST register returns 201 with token`() = testApplication {
        environment { config = buildConfig() }
        application { module() }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"e2euser","email":"e2e@test.com","password":"password123"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(json["token"])
        assertTrue(json["token"]!!.jsonPrimitive.content.isNotBlank())
    }

    @Test
    fun `POST login returns 401 for wrong password`() = testApplication {
        environment { config = buildConfig() }
        application { module() }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"nobody@test.com","password":"wrong"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST register returns 400 for duplicate email`() = testApplication {
        environment { config = buildConfig() }
        application { module() }

        // Первая регистрация
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"dup1","email":"dup@test.com","password":"password123"}""")
        }

        // Вторая с тем же email
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"dup2","email":"dup@test.com","password":"password123"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}