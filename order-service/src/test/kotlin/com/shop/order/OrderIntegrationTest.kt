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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OrderIntegrationTest {

    companion object {
        val postgres by lazy {
            PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
                withDatabaseName("order_test")
                withUsername("test")
                withPassword("test")
                start()
            }
        }

        var dbInitialized = false

        fun initDb() {
            if (dbInitialized) return
            val dataSource = HikariDataSource(HikariConfig().apply {
                jdbcUrl = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 5
                isAutoCommit = false
            })
            Database.connect(dataSource)
            transaction {
                SchemaUtils.create(Orders, OrderItems, AuditLogs)
            }
            dbInitialized = true
        }
    }

    @Test
    fun `should create order and retrieve by userId`() {
        initDb()
        val repo = OrderRepositoryImpl()

        val items = listOf(
            Triple(1, "Product A", Pair(BigDecimal("10.00"), 2)),
            Triple(2, "Product B", Pair(BigDecimal("5.00"), 1))
        )

        val order = repo.create(
            userId = 10,
            items = items,
            total = BigDecimal("25.00")
        )

        assertNotNull(order)
        assertEquals(10, order.userId)
        assertEquals("pending", order.status)
        assertEquals(25.0, order.totalAmount)
        assertEquals(2, order.items.size)

        val found = repo.findByUserId(10)
        assertTrue(found.isNotEmpty())
        assertEquals(order.id, found.first().id)
    }

    @Test
    fun `should cancel order and update status`() {
        initDb()
        val repo = OrderRepositoryImpl()

        val order = repo.create(
            userId = 20,
            items = listOf(Triple(1, "Item", Pair(BigDecimal("50.00"), 1))),
            total = BigDecimal("50.00")
        )

        val cancelled = repo.cancel(order.id, userId = 20)
        assertTrue(cancelled)

        val updated = repo.findById(order.id)
        assertEquals("cancelled", updated?.status)
    }
}