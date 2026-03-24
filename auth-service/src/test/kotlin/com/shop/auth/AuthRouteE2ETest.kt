package com.shop.auth

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import org.junit.AfterClass
import org.junit.BeforeClass
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthRouteE2ETest {

    companion object {
        // Один контейнер на все тесты класса
        val postgres = PostgreSQLContainer("postgres:15-alpine")
            .apply {
                withDatabaseName("e2e_test")
                withUsername("test")
                withPassword("test")
            }

        @BeforeClass
        @JvmStatic
        fun startContainers() {
            postgres.start()
        }

        @AfterClass
        @JvmStatic
        fun stopContainers() {
            postgres.stop()
        }
    }

    // Общая конфигурация для всех тестов
    private fun testConfig() = MapApplicationConfig(
        "db.url" to postgres.jdbcUrl,
        "db.user" to postgres.username,
        "db.password" to postgres.password,
        "jwt.secret" to "test-secret-key-at-least-32-characters!!",
        "jwt.issuer" to "test-issuer",
        "jwt.audience" to "test-audience"
    )

    @Test
    fun `POST auth-register should return 201 with token`() = testApplication {
        environment {
            config = testConfig()
        }
        application {
            module()
        }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "username": "testuser",
                  "email": "test@example.com",
                  "password": "password123"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(body["token"])
        assertNotNull(body["userId"])
        assertEquals("user", body["role"]?.jsonPrimitive?.content)
    }

    @Test
    fun `POST auth-login should return 401 for wrong credentials`() = testApplication {
        environment {
            config = testConfig()
        }
        application {
            module()
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "email": "nobody@example.com",
                  "password": "wrongpassword"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}