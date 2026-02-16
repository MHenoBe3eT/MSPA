package user

import domain.user.User

class CreateUserUseCase(
    private val createUser: CreateUser
) {
    operator fun invoke(
        email: String,
        passwordHash: String,
        name: String,
    ): User {
        val user = User.createNew(
            email = email,
            passwordHash = passwordHash,
            name = name,
        )
        return createUser.create(user)
    }
}
