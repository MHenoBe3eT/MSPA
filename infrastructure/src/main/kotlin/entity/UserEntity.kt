package entity

import domain.user.User
import domain.user.UserId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.*

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    var id: UUID,

    @Column(name = "email", unique = true, nullable = false)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
) {

    companion object {
        fun toBusiness(e: UserEntity): User {
            return User(
                id = UserId(e.id),
                email = e.email,
                passwordHash = e.passwordHash,
                name = e.name,
                createdAt = e.createdAt,
            )
        }

        fun fromBusiness(user: User): UserEntity {
            return UserEntity(
                id = user.id.value,
                email = user.email,
                passwordHash = user.passwordHash,
                name = user.name,
                createdAt = user.createdAt,
            )
        }
    }
}
