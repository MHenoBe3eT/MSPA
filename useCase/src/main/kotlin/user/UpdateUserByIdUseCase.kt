package user

import domain.user.User
import domain.user.UserId

class UpdateUserByIdUseCase(
    private val updateUser: UpdateUser,
    private val getUser: GetUser
) {
    fun invoke(
        id: UserId,
        name: String,
    ): User {
        val user = getUser.byId(id)
        return updateUser.update(user.copy(name = name))
    }
}
