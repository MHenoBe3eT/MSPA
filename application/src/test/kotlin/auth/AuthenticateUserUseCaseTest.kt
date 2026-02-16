package auth

import domain.user.User
import domain.user.UserId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import user.GetUserByEmail
import java.time.Instant

class AuthenticateUserUseCaseTest {

    private lateinit var getUserByEmail: GetUserByEmail
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var tokenProvider: TokenProvider
    private lateinit var useCase: AuthenticateUserUseCase

    @BeforeEach
    fun setUp() {
        getUserByEmail = mockk()
        passwordEncoder = mockk()
        tokenProvider = mockk()
        useCase = AuthenticateUserUseCase(getUserByEmail, passwordEncoder, tokenProvider)
    }

    private fun createUser(email: String = "user@example.com"): User = User(
        id = UserId.generateId(),
        email = email,
        passwordHash = "hashed_password",
        name = "Test User",
        createdAt = Instant.now(),
    )

    // Тест-кейс 4: Успешный логин
    @Test
    fun `should return JWT token on successful authentication`() {
        val email = "user@example.com"
        val rawPassword = "password123"
        val expectedToken = "jwt.token.here"
        val user = createUser(email)

        every { getUserByEmail.byEmail(email) } returns user
        every { passwordEncoder.matches(rawPassword, user.passwordHash) } returns true
        every { tokenProvider.generateToken(user.id, user.email) } returns expectedToken

        val token = useCase(email, rawPassword)

        assertEquals(expectedToken, token)
    }

    // Тест-кейс 5: Неверный пароль
    @Test
    fun `should throw InvalidCredentialsException on wrong password`() {
        val email = "user@example.com"
        val user = createUser(email)

        every { getUserByEmail.byEmail(email) } returns user
        every { passwordEncoder.matches("wrong_password", user.passwordHash) } returns false

        assertThrows<InvalidCredentialsException> {
            useCase(email, "wrong_password")
        }
    }

    // Тест-кейс 6: Несуществующий email — та же ошибка что и для неверного пароля
    @Test
    fun `should throw InvalidCredentialsException when email not found`() {
        every { getUserByEmail.byEmail("nonexistent@example.com") } returns null

        val exception = assertThrows<InvalidCredentialsException> {
            useCase("nonexistent@example.com", "password")
        }

        assertEquals("Invalid email or password", exception.message)
    }
}
