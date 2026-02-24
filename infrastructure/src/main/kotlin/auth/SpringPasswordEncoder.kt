package auth

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class SpringPasswordEncoder : PasswordEncoder {

    private val bcrypt = BCryptPasswordEncoder()

    override fun encode(rawPassword: String): String {
        return bcrypt.encode(rawPassword)
    }

    override fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return bcrypt.matches(rawPassword, encodedPassword)
    }
}
