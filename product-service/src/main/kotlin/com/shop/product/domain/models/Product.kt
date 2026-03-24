package com.shop.product.domain.models

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class ProductResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val price: Double,
    val stock: Int,
    val category: String?
)

@Serializable
data class CreateProductRequest(
    val name: String,
    val description: String? = null,
    val price: Double,
    val stock: Int,
    val category: String? = null
)

@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val stock: Int? = null,
    val category: String? = null
)

data class Product(
    val id: Int,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val stock: Int,
    val category: String?
)