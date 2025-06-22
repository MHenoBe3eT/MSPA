package card

import domain.card.Card
import domain.card.CardId

interface GetCard {
    fun byId(id: CardId): Card
}