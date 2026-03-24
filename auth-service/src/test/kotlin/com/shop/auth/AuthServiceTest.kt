package com.shop.auth

import com.shop.auth.domain.exceptions.AuthException
import com.shop.auth.domain.exceptions.ValidationException
import com.shop.auth.domain.models.LoginRequest
import com.shop.auth.domain.models.RegisterRequest
import com.shop.auth.domain.models.User
import com.shop.auth.repository.UserRepository
import com.shop.auth.service.AuthService
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AuthServiceTest {

    private val mockRepo = mockk<UserRepository>()

    private val service = AuthService(
        userRepository = mockRepo,
        jwtSecret = "test-secret-key-at-least-32-characters!!",
        jwtIssuer = "test-issuer",
        jwtAudience = "test-audience"
    )

    // ─── Unit тесты (без БД) ───

    @Test
    fun `register should throw ValidationException when email invalid`() {
        assertFailsWith<ValidationException> {
            service.register(
                RegisterRequest("user1", "invalid-email", "password123")
            )
        }
    }

    @Test
    fun `register should throw ValidationException when password too short`() {
        assertFailsWith<ValidationException> {
            service.register(
                RegisterRequest("user1", "user@test.com", "123")
            )
        }
    }

    @Test
    fun `register should throw ValidationException when username too short`() {
        assertFailsWith<ValidationException> {
            service.register(
                RegisterRequest("ab", "user@test.com", "password123")
            )
        }
    }

    @Test
    fun `register should throw ValidationException when email already exists`() {
        every { mockRepo.existsByEmail("exists@test.com") } returns true

        assertFailsWith<ValidationException> {
            service.register(
                RegisterRequest("user1", "exists@test.com", "password123")
            )
        }
    }

    @Test
    fun `register should return token when data is valid`() {
        every { mockRepo.existsByEmail(any()) } returns false
        every { mockRepo.existsByUsername(any()) } returns false
        every { mockRepo.create(any(), any(), any(), any()) } returns
                User(1, "user1", "user@test.com", "hashedpwd", "user")

        val result = service.register(
            RegisterRequest("user1", "user@test.com", "password123")
        )

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