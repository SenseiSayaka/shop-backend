package com.shop.auth.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: Int,
    val role: String
)

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val passwordHash: String,
    val role: String
)