package domain.user

import java.time.Instant
import java.util.*

data class User(
    val id: UserId,
    val email: String,
    val passwordHash: String,
    val name: String,
    val createdAt: Instant,
) {
    companion object {
        fun createNew(
            email: String,
            passwordHash: String,
            name: String,
        ): User = User(
            id = UserId.generateId(),
            email = email,
            passwordHash = passwordHash,
            name = name,
            createdAt = Instant.now(),
        )
    }
}

data class UserId(val value: UUID) {
    companion object {
        fun generateId(): UserId = UserId(UUID.randomUUID())
    }
}
