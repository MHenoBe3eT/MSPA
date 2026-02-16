package controller

import domain.user.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import ru.vlasov.api.UserControllerApi
import ru.vlasov.model.UserDto
import transformer.UserTransformer.Companion.fromBusiness
import user.CreateUser

class UserController(
    private val createUser: CreateUser
) : UserControllerApi {
    override fun createUser(userDto: UserDto): ResponseEntity<UserDto> {
        val user = User.createNew(
            email = "",
            passwordHash = "",
            name = userDto.name,
        )
        val result = createUser.create(user)
        return ResponseEntity(fromBusiness(result), HttpStatus.CREATED)
    }
}
