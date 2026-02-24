package user

import domain.user.User
import entity.UserEntity.Companion.fromBusiness
import entity.UserEntity.Companion.toBusiness
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import repository.user.UserRepository

@Component
@Transactional
class CreateUserInRepository(
    private val userRepository: UserRepository,
) : CreateUser {
    override fun create(user: User): User {
        return toBusiness(userRepository.save(fromBusiness(user)))
    }
}