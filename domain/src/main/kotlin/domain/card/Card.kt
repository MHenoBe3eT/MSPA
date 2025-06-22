package domain.card

import domain.document.Document
import domain.user.UserId
import java.util.*

data class Card(
    val id: CardId,
    val title: String,
    val userId: UserId,
    val documents: List<Document>,
) {
    companion object {
        fun createNew(
            title: String,
            userId: UserId,
            documents: List<Document>,
        ): Card = Card(
            id = CardId.generateId(),
            title = title,
            userId = userId,
            documents = documents
        )
    }
}

data class CardId(val value: UUID) {
    companion object {
        fun generateId(): CardId = CardId(UUID.randomUUID())
    }
}