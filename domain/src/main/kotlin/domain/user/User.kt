package domain.user

import domain.card.CardId
import java.util.*

data class User(
    val id: UserId,
    val name: String,
    val cardId: CardId,
) {
    companion object {
        fun createNew(
            name: String,
            cardId: CardId,
        ): User = User(
            id = UserId.generateId(),
            name = name,
            cardId = cardId,
        )
    }
}

data class UserId(val value: UUID) {
    companion object {
        fun generateId(): UserId = UserId(UUID.randomUUID())
    }
}