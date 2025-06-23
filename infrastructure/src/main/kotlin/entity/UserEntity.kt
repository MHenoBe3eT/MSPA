package entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "user")
class UserEntity(
    @Id
    var id: UUID,
    var name: String,
) {
}