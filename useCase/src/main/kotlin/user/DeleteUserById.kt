package user

import domain.user.UserId

class DeleteUserById(
    private val deleteUser: DeleteUser
) {
    operator fun invoke(id: UserId) = deleteUser.byId(id)
}