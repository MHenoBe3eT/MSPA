package card

import domain.card.Card
import domain.card.CardId

interface UpdateCard {
    fun byId(id: CardId): Card
}