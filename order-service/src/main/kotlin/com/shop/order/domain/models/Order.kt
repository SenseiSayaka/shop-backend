package com.shop.order.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderRequest(
    val items: List<OrderItemRequest>
)

@Serializable
data class OrderItemRequest(
    val productId: Int,
    val quantity: Int
)

@Serializable
data class OrderResponse(
    val id: Int,
    val userId: Int,
    val status: String,
    val totalAmount: Double,
    val items: List<OrderItemResponse>
)

@Serializable
data class OrderItemResponse(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val price: Double
)

@Serializable
data class ProductInfo(
    val id: Int,
    val name: String,
    val price: Double,
    val stock: Int
)

@Serializable
data class StatsResponse(
    val totalOrders: Long,
    val totalRevenue: Double,
    val pendingOrders: Long,
    val cancelledOrders: Long
)