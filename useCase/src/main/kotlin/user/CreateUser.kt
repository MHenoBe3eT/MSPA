package user

import domain.user.User

interface CreateUser {
    fun create(user: User): User
}