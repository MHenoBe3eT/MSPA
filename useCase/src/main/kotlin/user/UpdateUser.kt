package user

import domain.user.User

interface UpdateUser {
    fun update(user: User): User
}