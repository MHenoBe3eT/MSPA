package card

import domain.card.CardId

class DeleteCardUseCase(
    private val deleteCard: DeleteCard
) {
    operator fun invoke(id: CardId): Unit = deleteCard.byId(id)
}