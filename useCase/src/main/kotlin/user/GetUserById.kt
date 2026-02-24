package user

import domain.user.User
import domain.user.UserId

class GetUserById(
    private val getUser: GetUser
) {
    fun invoke(id: UserId): User = getUser.byId(id)
}