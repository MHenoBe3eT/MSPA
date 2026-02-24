package user

import domain.user.User
import domain.user.UserId

interface GetUser {
    fun byId(id: UserId): User
    fun byIdOrNull(id: UserId): User?
}