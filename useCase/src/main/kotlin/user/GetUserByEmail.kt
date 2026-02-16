package user

import domain.user.User

interface GetUserByEmail {
    fun byEmail(email: String): User?
}
