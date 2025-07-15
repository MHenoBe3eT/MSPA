package user

import domain.card.CardId
import domain.user.User
import domain.user.UserId

class UpdateUserByIdUseCase(
    private val updateUser: UpdateUser,
    private val getUser: GetUser
) {
    fun invoke(
        id: UserId,
        name: String,
        cardId: CardId,
    ): User {
        var user = getUser.byId(id)
        user = User(id = user.id, name = name, cardId = cardId)
        return updateUser.update(user)
    }
}