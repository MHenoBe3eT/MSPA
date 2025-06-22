package card

import domain.card.Card
import domain.document.Document
import domain.user.UserId

class CreateCardUseCase(
    private val createCard: CreateCard,
) {
    fun invoke(
        title: String,
        userId: UserId,
        documents: List<Document>
    ): Card {
        val card = Card.createNew(
            title = title,
            userId = userId,
            documents = documents
        )
        return createCard.create(card)
    }
}