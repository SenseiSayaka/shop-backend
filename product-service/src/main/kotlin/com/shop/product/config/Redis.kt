package com.shop.product.config

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.ktor.server.application.*

lateinit var redisConnection: StatefulRedisConnection<String, String>

fun Application.configureRedis() {
    val redisUrl = environment.config.property("redis.url").getString()
    val client = RedisClient.create(redisUrl)
    redisConnection = client.connect()
    log.info("Redis connected to $redisUrl")
}