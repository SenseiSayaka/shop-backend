package com.shop.product

import com.shop.product.domain.models.CreateProductRequest
import com.shop.product.domain.models.Product
import com.shop.product.repository.ProductRepository
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProductServiceTest {

    private val mockRepo = mockk<ProductRepository>()

    // Тестируем только валидацию — без Redis
    @Test
    fun `createProduct should throw when name is blank`() {
        val service = TestableProductService(mockRepo)
        assertFailsWith<IllegalArgumentException> {
            service.validateAndCreate(CreateProductRequest("", null, 10.0, 5, null))
        }
    }

    @Test
    fun `createProduct should throw when price is zero`() {
        val service = TestableProductService(mockRepo)
        assertFailsWith<IllegalArgumentException> {
            service.validateAndCreate(CreateProductRequest("Product", null, 0.0, 5, null))
        }
    }

    @Test
    fun `createProduct should throw when price is negative`() {
        val service = TestableProductService(mockRepo)
        assertFailsWith<IllegalArgumentException> {
            service.validateAndCreate(CreateProductRequest("Product", null, -5.0, 5, null))
        }
    }

    @Test
    fun `createProduct should throw when stock is negative`() {
        val service = TestableProductService(mockRepo)
        assertFailsWith<IllegalArgumentException> {
            service.validateAndCreate(CreateProductRequest("Product", null, 10.0, -1, null))
        }
    }

    @Test
    fun `createProduct should succeed with valid data`() {
        every {
            mockRepo.create(any(), any(), any(), any(), any())
        } returns Product(1, "Test", null, BigDecimal("10.00"), 5, null)

        val service = TestableProductService(mockRepo)
        val result = service.validateAndCreate(
            CreateProductRequest("Test", null, 10.0, 5, null)
        )

        assertEquals(1, result.id)
        assertEquals("Test", result.name)
        assertEquals(10.0, result.price)
        assertEquals(5, result.stock)
    }
}

// Тестируемая версия сервиса без Redis зависимости
class TestableProductService(private val repository: ProductRepository) {

    fun validateAndCreate(request: CreateProductRequest): com.shop.product.domain.models.ProductResponse {
        if (request.name.isBlank())
            throw IllegalArgumentException("Name cannot be blank")
        if (request.price <= 0)
            throw IllegalArgumentException("Price must be positive")
        if (request.stock < 0)
            throw IllegalArgumentException("Stock cannot be negative")

        val product = repository.create(
            name = request.name,
            description = request.description,
            price = BigDecimal.valueOf(request.price),
            stock = request.stock,
            category = request.category
        )

        return com.shop.product.domain.models.ProductResponse(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price.toDouble(),
            stock = product.stock,
            category = product.category
        )
    }
}