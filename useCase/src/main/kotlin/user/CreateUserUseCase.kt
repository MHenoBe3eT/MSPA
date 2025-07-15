package user

import domain.card.CardId
import domain.user.User

class CreateUserUseCase(
    private val createUser: CreateUser
) {
    operator fun invoke(
        name: String,
        cardId: CardId,
    ): User {
        val user = User.createNew(
            name = name,
            cardId = cardId,
        )
        return createUser.create(user)
    }
}