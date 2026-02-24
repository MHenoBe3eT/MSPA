package auth

import domain.user.UserId
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class JwtTokenProviderTest {

    private lateinit var tokenProvider: JwtTokenProvider

    private val secret = Base64.getEncoder().encodeToString(
        "thisIsAVeryLongSecretKeyForJWTTokenSigning1234567890".toByteArray()
    )
    private val expirationMs = 3600000L

    @BeforeEach
    fun setUp() {
        tokenProvider = JwtTokenProvider(secret, expirationMs)
    }

    // Тест-кейс 7: Генерация токена
    @Test
    fun `should generate valid JWT token from userId and email`() {
        val userId = UserId.generateId()
        val email = "user@example.com"

        val token = tokenProvider.generateToken(userId, email)

        assertNotNull(token)
        assertTrue(token.split(".").size == 3, "Token should have 3 parts (header.payload.signature)")
        assertTrue(tokenProvider.isValid(token))
    }

    // Тест-кейс 8: Парсинг токена
    @Test
    fun `should extract userId and email from valid token`() {
        val userId = UserId.generateId()
        val email = "user@example.com"

        val token = tokenProvider.generateToken(userId, email)

        val extractedUserId = tokenProvider.getUserId(token)
        val extractedEmail = tokenProvider.getEmail(token)

        assertEquals(userId, extractedUserId)
        assertEquals(email, extractedEmail)
    }

    // Тест-кейс 9: Просроченный токен
    @Test
    fun `should return false for expired token`() {
        val expiredProvider = JwtTokenProvider(secret, -1000L) // negative = already expired
        val userId = UserId.generateId()

        val token = expiredProvider.generateToken(userId, "user@example.com")

        assertFalse(tokenProvider.isValid(token))
    }

    // Тест-кейс 10: Невалидный токен
    @Test
    fun `should return false for tampered token`() {
        assertFalse(tokenProvider.isValid("invalid.token.here"))
    }

    @Test
    fun `should return false for token signed with different key`() {
        val otherSecret = Base64.getEncoder().encodeToString(
            "anotherVeryLongSecretKeyForJWTTokenSigning1234567890".toByteArray()
        )
        val otherKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(otherSecret))

        val foreignToken = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .signWith(otherKey)
            .compact()

        assertFalse(tokenProvider.isValid(foreignToken))
    }
}
