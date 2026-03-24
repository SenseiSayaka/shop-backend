package com.shop.order.domain.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Orders : Table("orders") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id")
    val status = varchar("status", 50).default("pending")
    val totalAmount = decimal("total_amount", 10, 2)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object OrderItems : Table("order_items") {
    val id = integer("id").autoIncrement()
    val orderId = integer("order_id").references(Orders.id)
    val productId = integer("product_id")
    val productName = varchar("product_name", 255)
    val quantity = integer("quantity")
    val price = decimal("price", 10, 2)

    override val primaryKey = PrimaryKey(id)
}

object AuditLogs : Table("audit_logs") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").nullable()
    val action = varchar("action", 100)
    val entityType = varchar("entity_type", 100)
    val entityId = integer("entity_id").nullable()
    val details = text("details").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}