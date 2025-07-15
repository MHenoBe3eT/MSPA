package entity

import domain.card.CardId
import domain.user.User
import domain.user.UserId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    var id: UUID,

    @Column(name = "name")
    var name: String,

    @Column(name = "card_id")
    var cardId: UUID,
) {

    companion object{
        fun toBusiness(e: UserEntity): User {
            return User(
                id = UserId(e.id),
                name = e.name,
                cardId = CardId(e.cardId),
            )
        }

        fun fromBusiness(user: User): UserEntity {
            return UserEntity(
                id = user.id.value,
                name = user.name,
                cardId = user.cardId.value
            )
        }
    }
}