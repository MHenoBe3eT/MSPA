package user

import domain.user.User
import entity.UserEntity.Companion.fromBusiness
import entity.UserEntity.Companion.toBusiness
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import repository.user.UserRepository

@Transactional
@Component
class UpdateUserInRepository(
    private val userRepository: UserRepository,
) : UpdateUser {
    override fun update(user: User): User {
        return toBusiness(userRepository.save(fromBusiness(user)))
    }

}