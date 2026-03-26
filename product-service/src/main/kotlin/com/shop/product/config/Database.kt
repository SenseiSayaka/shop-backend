package com.shop.product.config

import com.shop.product.domain.tables.Products
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    val url = environment.config.property("db.url").getString()
    val user = environment.config.property("db.user").getString()
    val password = environment.config.property("db.password").getString()

    val hikariConfig = HikariConfig().apply {
        jdbcUrl = url
        username = user
        this.password = password
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    val dataSource = HikariDataSource(hikariConfig)

    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .validateOnMigrate(false)
        .outOfOrder(true)
        .load()
        .migrate()

    Database.connect(dataSource)

    transaction {
        SchemaUtils.createMissingTablesAndColumns(Products)
    }
}