package domain.user

import java.util.*

data class User(
    val id: UserId,
    val name: String,
) {
    companion object {
        fun createNew(
            name: String,
        ): User = User(
            id = UserId.generateId(),
            name = name,
        )
    }
}

data class UserId(val value: UUID) {
    companion object {
        fun generateId(): UserId = UserId(UUID.randomUUID())
    }
}