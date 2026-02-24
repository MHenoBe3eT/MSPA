package auth

import domain.user.UserId

interface TokenProvider {
    fun generateToken(userId: UserId, email: String): String
    fun getUserId(token: String): UserId
    fun getEmail(token: String): String
    fun isValid(token: String): Boolean
}
