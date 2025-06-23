package card

import domain.card.Card
import domain.card.CardId
import domain.document.Document
import domain.user.UserId

class UpdateCardUseCase(
    private val updateCard: UpdateCard,
    private val getCard: GetCard,
) {
    fun invoke(
        id: CardId,
        title: String,
        userId: UserId,
        documents: List<Document>,
    ): Card {
        var card = getCard.byId(id)
        card = Card(
            id = card.id,
            title = title,
            userId = userId,
            documents = documents,
        )
        return updateCard.update(card)
    }
}