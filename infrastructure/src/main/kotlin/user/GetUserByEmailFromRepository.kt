package user

import domain.user.User
import entity.UserEntity
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import repository.user.UserRepository

@Transactional
@Component
class GetUserByEmailFromRepository(
    private val userRepository: UserRepository,
) : GetUserByEmail {
    override fun byEmail(email: String): User? {
        return userRepository.findByEmail(email)?.let { UserEntity.toBusiness(it) }
    }
}
