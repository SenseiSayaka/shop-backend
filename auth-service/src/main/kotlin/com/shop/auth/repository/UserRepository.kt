package com.shop.auth.repository

import com.shop.auth.domain.models.User
import com.shop.auth.domain.tables.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

interface UserRepository {
    fun findByEmail(email: String): User?
    fun findById(id: Int): User?
    fun create(username: String, email: String, passwordHash: String, role: String): User
    fun existsByEmail(email: String): Boolean
    fun existsByUsername(username: String): Boolean
}

class UserRepositoryImpl : UserRepository {

    override fun findByEmail(email: String): User? = transaction {
        Users.select { Users.email eq email }
            .singleOrNull()
            ?.toUser()
    }

    override fun findById(id: Int): User? = transaction {
        Users.select { Users.id eq id }
            .singleOrNull()
            ?.toUser()
    }

    override fun create(
        username: String,
        email: String,
        passwordHash: String,
        role: String
    ): User = transaction {
        val id = Users.insert {
            it[Users.username] = username
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
            it[Users.role] = role
            it[createdAt] = Instant.now()
        } get Users.id

        User(id, username, email, passwordHash, role)
    }

    override fun existsByEmail(email: String): Boolean = transaction {
        Users.select { Users.email eq email }.count() > 0
    }

    override fun existsByUsername(username: String): Boolean = transaction {
        Users.select { Users.username eq username }.count() > 0
    }

    private fun ResultRow.toUser() = User(
        id = this[Users.id],
        username = this[Users.username],
        email = this[Users.email],
        passwordHash = this[Users.passwordHash],
        role = this[Users.role]
    )
}