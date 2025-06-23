package user

import domain.user.User

class CreateUserUseCase(
    private val createUser: CreateUser
) {
    operator fun invoke(
        name: String
    ): User {
        val user = User.createNew(
            name = name,
        )
        return createUser.create(user)
    }
}