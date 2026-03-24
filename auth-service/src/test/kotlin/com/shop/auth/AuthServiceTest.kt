package com.shop.auth

import com.shop.auth.domain.exceptions.AuthException
import com.shop.auth.domain.exceptions.ValidationException
import com.shop.auth.domain.models.LoginRequest
import com.shop.auth.domain.models.RegisterRequest
import com.shop.auth.domain.models.User
import com.shop.auth.repository.UserRepository
import com.shop.auth.service.AuthService
import io.mockk.*
import kotlin.test.*

class AuthServiceTest {

    private val mockRepo = mockk<UserRepository>()
    private val service = AuthService(
        mockRepo,
        jwtSecret = "test-secret-key-at-least-32-chars!!",
        jwtIssuer = "test-issuer",
        jwtAudience = "test-audience"
    )

    @Test
    fun `register should throw ValidationException when email invalid`() {
        val request = RegisterRequest("user1", "invalid-email", "password123")
        assertFailsWith<ValidationException> {
            service.register(request)
        }
    }

    @Test
    fun `register should throw ValidationException when password too short`() {
        val request = RegisterRequest("user1", "user@test.com", "123")
        assertFailsWith<ValidationException> {
            service.register(request)
        }
    }

    @Test
    fun `register should throw ValidationException when username too short`() {
        val request = RegisterRequest("ab", "user@test.com", "password123")
        assertFailsWith<ValidationException> {
            service.register(request)
        }
    }

    @Test
    fun `register should succeed with valid data`() {
        every { mockRepo.existsByEmail(any()) } returns false
        every { mockRepo.existsByUsername(any()) } returns false
        every { mockRepo.create(any(), any(), any(), any()) } returns
                User(1, "user1", "user@test.com", "hash", "user")

        val result = service.register(RegisterRequest("user1", "user@test.com", "password123"))

        assertEquals(1, result.userId)
        assertEquals("user", result.role)
        assertTrue(result.token.isNotBlank())
    }

    @Test
    fun `login should throw AuthException when user not found`() {
        every { mockRepo.findByEmail(any()) } returns null

        assertFailsWith<AuthException> {
            service.login(LoginRequest("notexist@test.com", "password"))
        }
    }
}