package com.shop.auth.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.shop.auth.domain.exceptions.AuthException
import com.shop.auth.domain.exceptions.ValidationException
import com.shop.auth.domain.models.AuthResponse
import com.shop.auth.domain.models.LoginRequest
import com.shop.auth.domain.models.RegisterRequest
import com.shop.auth.repository.UserRepository
import java.util.*

class AuthService(
    private val userRepository: UserRepository,
    private val jwtSecret: String,
    private val jwtIssuer: String,
    private val jwtAudience: String
) {

    fun register(request: RegisterRequest): AuthResponse {
        // Валидация
        if (request.username.length < 3)
            throw ValidationException("Username must be at least 3 characters")
        if (!request.email.contains("@"))
            throw ValidationException("Invalid email format")
        if (request.password.length < 6)
            throw ValidationException("Password must be at least 6 characters")

        if (userRepository.existsByEmail(request.email))
            throw ValidationException("Email already exists")
        if (userRepository.existsByUsername(request.username))
            throw ValidationException("Username already exists")

        val passwordHash = BCrypt.withDefaults()
            .hashToString(12, request.password.toCharArray())

        val user = userRepository.create(
            username = request.username,
            email = request.email,
            passwordHash = passwordHash,
            role = "user"
        )

        val token = generateToken(user.id, user.role)
        return AuthResponse(token, user.id, user.role)
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw AuthException("Invalid credentials")

        val result = BCrypt.verifyer().verify(
            request.password.toCharArray(),
            user.passwordHash
        )

        if (!result.verified)
            throw AuthException("Invalid credentials")

        val token = generateToken(user.id, user.role)
        return AuthResponse(token, user.id, user.role)
    }

    private fun generateToken(userId: Int, role: String): String =
        JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("userId", userId)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000))
            .sign(Algorithm.HMAC256(jwtSecret))
}