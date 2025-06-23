package user

import domain.user.User
import domain.user.UserId

class UpdateUserByIdUseCase(
    private val updateUser: UpdateUser,
    private val getUser: GetUser
) {
    fun invoke(
        id: UserId,
        name: String
    ): User {
        var user = getUser.byId(id)
        user = User(id = user.id, name = name)
        return updateUser.update(user)
    }
}