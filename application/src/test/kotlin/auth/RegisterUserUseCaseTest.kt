package auth

import domain.user.User
import domain.user.UserId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import user.CreateUser
import user.GetUserByEmail
import java.time.Instant

class RegisterUserUseCaseTest {

    private lateinit var getUserByEmail: GetUserByEmail
    private lateinit var createUser: CreateUser
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var useCase: RegisterUserUseCase

    @BeforeEach
    fun setUp() {
        getUserByEmail = mockk()
        createUser = mockk()
        passwordEncoder = mockk()
        useCase = RegisterUserUseCase(getUserByEmail, createUser, passwordEncoder)
    }

    // Тест-кейс 1: Успешная регистрация
    @Test
    fun `should register user with valid email, password and name`() {
        val email = "test@example.com"
        val rawPassword = "password123"
        val name = "Test User"
        val hashedPassword = "hashed_password123"

        every { getUserByEmail.byEmail(email) } returns null
        every { passwordEncoder.encode(rawPassword) } returns hashedPassword
        val userSlot = slot<User>()
        every { createUser.create(capture(userSlot)) } answers { userSlot.captured }

        val result = useCase(email, rawPassword, name)

        assertEquals(email, result.email)
        assertEquals(hashedPassword, result.passwordHash)
        assertEquals(name, result.name)
        assertNotNull(result.id)
        assertNotNull(result.createdAt)
    }

    // Тест-кейс 2: Дубликат email
    @Test
    fun `should throw EmailAlreadyExistsException when email is taken`() {
        val email = "existing@example.com"
        val existingUser = User(
            id = UserId.generateId(),
            email = email,
            passwordHash = "hash",
            name = "Existing",
            createdAt = Instant.now(),
        )

        every { getUserByEmail.byEmail(email) } returns existingUser

        assertThrows<EmailAlreadyExistsException> {
            useCase(email, "password", "New User")
        }

        verify(exactly = 0) { createUser.create(any()) }
    }

    // Тест-кейс 3: Пароль хешируется
    @Test
    fun `should hash password using PasswordEncoder before saving`() {
        val rawPassword = "plaintext_password"
        val hashedPassword = "\$2a\$10\$hashedValue"

        every { getUserByEmail.byEmail(any()) } returns null
        every { passwordEncoder.encode(rawPassword) } returns hashedPassword
        val userSlot = slot<User>()
        every { createUser.create(capture(userSlot)) } answers { userSlot.captured }

        val result = useCase("user@example.com", rawPassword, "User")

        verify(exactly = 1) { passwordEncoder.encode(rawPassword) }
        assertEquals(hashedPassword, result.passwordHash)
        assertNotEquals(rawPassword, result.passwordHash)
    }
}
