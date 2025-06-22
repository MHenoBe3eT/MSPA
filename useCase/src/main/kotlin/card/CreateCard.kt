package card

import domain.card.Card

interface CreateCard {
    fun create(card: Card): Card
}