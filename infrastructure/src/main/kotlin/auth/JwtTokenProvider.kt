package auth

import domain.user.UserId
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiration-ms:3600000}") private val expirationMs: Long,
) : TokenProvider {

    private val key: SecretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))

    override fun generateToken(userId: UserId, email: String): String {
        val now = Date()
        val expiry = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(userId.value.toString())
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    override fun getUserId(token: String): UserId {
        val claims = parseToken(token)
        return UserId(UUID.fromString(claims.subject))
    }

    override fun getEmail(token: String): String {
        val claims = parseToken(token)
        return claims.get("email", String::class.java)
    }

    override fun isValid(token: String): Boolean {
        return try {
            parseToken(token)
            true
        } catch (_: JwtException) {
            false
        }
    }

    private fun parseToken(token: String) =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
