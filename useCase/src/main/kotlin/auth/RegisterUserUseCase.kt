package auth

import domain.user.User
import user.CreateUser
import user.GetUserByEmail

class RegisterUserUseCase(
    private val getUserByEmail: GetUserByEmail,
    private val createUser: CreateUser,
    private val passwordEncoder: PasswordEncoder,
) {
    operator fun invoke(email: String, rawPassword: String, name: String): User {
        getUserByEmail.byEmail(email)?.let {
            throw EmailAlreadyExistsException(email)
        }

        val user = User.createNew(
            email = email,
            passwordHash = passwordEncoder.encode(rawPassword),
            name = name,
        )
        return createUser.create(user)
    }
}
