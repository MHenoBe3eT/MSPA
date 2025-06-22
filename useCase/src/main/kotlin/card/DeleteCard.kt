package card

import domain.card.CardId

interface DeleteCard {
    fun byId(id: CardId)
}


