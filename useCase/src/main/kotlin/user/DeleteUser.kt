package user

import domain.user.UserId

interface DeleteUser {
    fun byId(id: UserId)
}