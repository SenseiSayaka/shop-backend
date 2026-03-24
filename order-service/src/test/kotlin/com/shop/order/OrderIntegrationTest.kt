package com.shop.order

import com.shop.order.domain.tables.AuditLogs
import com.shop.order.domain.tables.OrderItems
import com.shop.order.domain.tables.Orders
import com.shop.order.repository.OrderRepositoryImpl
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import kotlin.test.*

class OrderIntegrationTest {

    companion object {
        val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("testdb")
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
        transaction {
            SchemaUtils.create(Orders, OrderItems, AuditLogs)
        }
    }

    @Test
    fun `should create order and find it by userId`() {
        val repo = OrderRepositoryImpl()
        val items = listOf(
            Triple(1, "Product A", Pair(BigDecimal("10.00"), 2)),
            Triple(2, "Product B", Pair(BigDecimal("5.00"), 1))
        )

        val order = repo.create(userId = 1, items = items, total = BigDecimal("25.00"))

        assertEquals(1, order.userId)
        assertEquals("pending", order.status)
        assertEquals(25.0, order.totalAmount)
        assertEquals(2, order.items.size)

        val found = repo.findByUserId(1)
        assertTrue(found.isNotEmpty())
        assertEquals(order.id, found.first().id)
    }

    @Test
    fun `should cancel order successfully`() {
        val repo = OrderRepositoryImpl()
        val order = repo.create(
            userId = 2,
            items = listOf(Triple(1, "Item", Pair(BigDecimal("20.00"), 1))),
            total = BigDecimal("20.00")
        )

        val cancelled = repo.cancel(order.id, userId = 2)
        assertTrue(cancelled)

        val updated = repo.findById(order.id)
        assertEquals("cancelled", updated?.status)
    }
}