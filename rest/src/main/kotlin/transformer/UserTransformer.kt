package transformer

import domain.user.User
import ru.vlasov.model.UserDto

class UserTransformer {
    companion object {
        fun fromBusiness(user: User): UserDto {
            return UserDto(
                id = user.id.value,
                name = user.name,
            )
        }
    }
}
