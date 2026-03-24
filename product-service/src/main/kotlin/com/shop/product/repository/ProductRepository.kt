package com.shop.product.repository

import com.shop.product.domain.models.Product
import com.shop.product.domain.tables.Products
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant

interface ProductRepository {
    fun findAll(): List<Product>
    fun findById(id: Int): Product?
    fun create(name: String, description: String?, price: BigDecimal, stock: Int, category: String?): Product
    fun update(id: Int, name: String?, description: String?, price: BigDecimal?, stock: Int?, category: String?): Product?
    fun delete(id: Int): Boolean
    fun updateStock(id: Int, delta: Int): Boolean
}

class ProductRepositoryImpl : ProductRepository {

    override fun findAll(): List<Product> = transaction {
        Products.selectAll().map { it.toProduct() }
    }

    override fun findById(id: Int): Product? = transaction {
        Products.select { Products.id eq id }.singleOrNull()?.toProduct()
    }

    override fun create(
        name: String,
        description: String?,
        price: BigDecimal,
        stock: Int,
        category: String?
    ): Product = transaction {
        val now = Instant.now()
        val id = Products.insert {
            it[Products.name] = name
            it[Products.description] = description
            it[Products.price] = price
            it[Products.stock] = stock
            it[Products.category] = category
            it[createdAt] = now
            it[updatedAt] = now
        } get Products.id

        Product(id, name, description, price, stock, category)
    }

    override fun update(
        id: Int,
        name: String?,
        description: String?,
        price: BigDecimal?,
        stock: Int?,
        category: String?
    ): Product? = transaction {
        val updated = Products.update({ Products.id eq id }) { row ->
            name?.let { row[Products.name] = it }
            description?.let { row[Products.description] = it }
            price?.let { row[Products.price] = it }
            stock?.let { row[Products.stock] = it }
            category?.let { row[Products.category] = it }
            row[updatedAt] = Instant.now()
        }
        if (updated > 0) findById(id) else null
    }

    override fun delete(id: Int): Boolean = transaction {
        Products.deleteWhere { Products.id eq id } > 0
    }

    override fun updateStock(id: Int, delta: Int): Boolean = transaction {
        val product = findById(id) ?: return@transaction false
        val newStock = product.stock + delta
        if (newStock < 0) return@transaction false

        Products.update({ Products.id eq id }) {
            it[stock] = newStock
            it[updatedAt] = Instant.now()
        } > 0
    }

    private fun ResultRow.toProduct() = Product(
        id = this[Products.id],
        name = this[Products.name],
        description = this[Products.description],
        price = this[Products.price],
        stock = this[Products.stock],
        category = this[Products.category]
    )
}