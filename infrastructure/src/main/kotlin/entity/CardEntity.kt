package entity

import domain.card.Card
import domain.card.CardId
import domain.user.User
import domain.user.UserId
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "cards")
class CardEntity(
    @Id
    var id: UUID,

    @Column(name = "title")
    var title: String,

    @Column(name = "user_id")
    var userId: UUID,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "documents")
    var documents: List<DocumentEntity> = emptyList()
) {
    companion object{
        fun toBusiness(e: CardEntity): Card {
            return Card(
                id = CardId(e.id),
                title = e.title,
                userId = UserId(e.userId),
                documents = e.documents.map { DocumentEntity.toBusiness(it) }
            )
        }

        fun fromBusiness(user: User): UserEntity {
            return UserEntity(
                id = user.id.value,
                name = user.name,
                cardId = user.cardId?.value
            )
        }
    }

}