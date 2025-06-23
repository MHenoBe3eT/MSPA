package card

import domain.card.Card
import domain.card.CardId

class GetCardByIdUseCase(
    private val getCard: GetCard,
) {
    operator fun invoke(id: CardId): Card = getCard.byId(id)
}