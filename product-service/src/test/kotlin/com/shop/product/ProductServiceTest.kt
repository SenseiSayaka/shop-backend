package com.shop.product

import com.shop.product.domain.models.CreateProductRequest
import com.shop.product.domain.models.Product
import com.shop.product.repository.ProductRepository
import com.shop.product.service.ProductService
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.mockk.*
import com.shop.product.config.redisConnection as globalRedis
import kotlin.test.*
import java.math.BigDecimal

class ProductServiceTest {

    private val mockRepo = mockk<ProductRepository>()
    private val mockRedisConn = mockk<StatefulRedisConnection<String, String>>()
    private val mockRedisSync = mockk<RedisCommands<String, String>>()

    @BeforeTest
    fun setup() {
        every { mockRedisConn.sync() } returns mockRedisSync
        every { mockRedisSync.get(any()) } returns null
        every { mockRedisSync.setex(any(), any(), any()) } returns "OK"
        every { mockRedisSync.del(*anyVararg<String>()) } returns 1L
        // Подменяем глобальную переменную через reflection для теста
        val field = Class.forName("com.shop.product.config.RedisKt")
            .getDeclaredField("redisConnection")
        field.isAccessible = true
        field.set(null, mockRedisConn)
    }

    @Test
    fun `createProduct should throw when name is blank`() {
        val service = ProductService(mockRepo)
        assertFailsWith<IllegalArgumentException> {
            service.createProduct(CreateProductRequest("", null, 10.0, 5, null))
        }
    }

    @Test
    fun `createProduct should throw when price is negative`() {
        val service = ProductService(mockRepo)
        assertFailsWith<IllegalArgumentException> {
            service.createProduct(CreateProductRequest("Product", null, -1.0, 5, null))
        }
    }

    @Test
    fun `createProduct should succeed with valid data`() {
        every { mockRepo.create(any(), any(), any(), any(), any()) } returns
                Product(1, "Test", null, BigDecimal("10.00"), 5, null)

        val service = ProductService(mockRepo)
        val result = service.createProduct(
            CreateProductRequest("Test", null, 10.0, 5, null)
        )

        assertEquals(1, result.id)
        assertEquals("Test", result.name)
        assertEquals(10.0, result.price)
    }
}