package auth

import user.GetUserByEmail

class AuthenticateUserUseCase(
    private val getUserByEmail: GetUserByEmail,
    private val passwordEncoder: PasswordEncoder,
    private val tokenProvider: TokenProvider,
) {
    operator fun invoke(email: String, rawPassword: String): String {
        val user = getUserByEmail.byEmail(email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(rawPassword, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        return tokenProvider.generateToken(user.id, user.email)
    }
}
