package com.shop.auth

import com.shop.auth.domain.tables.Users
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.*

class AuthRouteE2ETest {

    companion object {
        val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("e2e_test")
            withUsername("test")
            withPassword("test")
            start()
        }
    }

    @BeforeTest
    fun setup() {
        val dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            driverClassName = "org.postgresql.Driver"
        })
        Database.connect(dataSource)
        Flyway.configure().dataSource(dataSource)
            .locations("classpath:db/migration").load().migrate()
    }

    @Test
    fun `POST auth-register should return 201 with token`() = testApplication {
        environment {
            config = mapOf(
                "db.url" to postgres.jdbcUrl,
                "db.user" to postgres.username,
                "db.password" to postgres.password,
                "jwt.secret" to "test-secret-key-at-least-32-chars!!",
                "jwt.issuer" to "test",
                "jwt.audience" to "test"
            ).toConfig()
        }

        application { module() }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"testuser","email":"test@example.com","password":"password123"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(body["token"])
    }

    @Test
    fun `POST auth-login should return 401 for wrong credentials`() = testApplication {
        environment {
            config = mapOf(
                "db.url" to postgres.jdbcUrl,
                "db.user" to postgres.username,
                "db.password" to postgres.password,
                "jwt.secret" to "test-secret-key-at-least-32-chars!!",
                "jwt.issuer" to "test",
                "jwt.audience" to "test"
            ).toConfig()
        }

        application { module() }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"nobody@example.com","password":"wrong"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}

fun Map<String, String>.toConfig(): io.ktor.server.config.MapApplicationConfig {
    val config = io.ktor.server.config.MapApplicationConfig()
    forEach { (k, v) -> config.put(k, v) }
    return config
}