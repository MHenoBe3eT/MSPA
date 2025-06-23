package card

import domain.card.Card

interface UpdateCard {
    fun update(card: Card): Card
}