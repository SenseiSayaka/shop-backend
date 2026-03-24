package com.shop.product.service

import com.shop.product.config.redisConnection
import com.shop.product.domain.models.*
import com.shop.product.repository.ProductRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

class ProductService(private val repository: ProductRepository) {

    private val CACHE_TTL = 300L // 5 минут
    private val json = Json { ignoreUnknownKeys = true }

    fun getAllProducts(): List<ProductResponse> {
        val cacheKey = "products:all"
        val cached = redisConnection.sync().get(cacheKey)
        if (cached != null) {
            logger.info("Cache hit for $cacheKey")
            return json.decodeFromString(cached)
        }

        val products = repository.findAll().map { it.toResponse() }
        redisConnection.sync().setex(cacheKey, CACHE_TTL, json.encodeToString(products))
        return products
    }

    fun getProductById(id: Int): ProductResponse? {
        val cacheKey = "products:$id"
        val cached = redisConnection.sync().get(cacheKey)
        if (cached != null) {
            logger.info("Cache hit for $cacheKey")
            return json.decodeFromString(cached)
        }

        val product = repository.findById(id)?.toResponse() ?: return null
        redisConnection.sync().setex(cacheKey, CACHE_TTL, json.encodeToString(product))
        return product
    }

    fun createProduct(request: CreateProductRequest): ProductResponse {
        if (request.name.isBlank()) throw IllegalArgumentException("Name cannot be blank")
        if (request.price <= 0) throw IllegalArgumentException("Price must be positive")
        if (request.stock < 0) throw IllegalArgumentException("Stock cannot be negative")

        val product = repository.create(
            name = request.name,
            description = request.description,
            price = BigDecimal.valueOf(request.price),
            stock = request.stock,
            category = request.category
        )

        invalidateCache()
        return product.toResponse()
    }

    fun updateProduct(id: Int, request: UpdateProductRequest): ProductResponse? {
        val product = repository.update(
            id = id,
            name = request.name,
            description = request.description,
            price = request.price?.let { BigDecimal.valueOf(it) },
            stock = request.stock,
            category = request.category
        ) ?: return null

        invalidateProductCache(id)
        invalidateCache()
        return product.toResponse()
    }

    fun deleteProduct(id: Int): Boolean {
        val deleted = repository.delete(id)
        if (deleted) {
            invalidateProductCache(id)
            invalidateCache()
        }
        return deleted
    }

    private fun invalidateCache() {
        redisConnection.sync().del("products:all")
        logger.info("Invalidated products:all cache")
    }

    private fun invalidateProductCache(id: Int) {
        redisConnection.sync().del("products:$id")
        logger.info("Invalidated products:$id cache")
    }

    private fun Product.toResponse() = ProductResponse(
        id = id,
        name = name,
        description = description,
        price = price.toDouble(),
        stock = stock,
        category = category
    )
}