package user

import domain.user.UserId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import repository.user.UserRepository

@Transactional
@Component
class DeleteUserFromRepository(
    private val userRepository: UserRepository
) : DeleteUser {
    override fun byId(id: UserId) {
        userRepository.deleteById(id.value)
    }
}