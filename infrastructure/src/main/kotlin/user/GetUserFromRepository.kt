package user

import domain.user.User
import domain.user.UserId
import entity.UserEntity
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import repository.user.UserRepository

@Transactional
@Component
class GetUserFromRepository(
    private val userRepository: UserRepository
) : GetUser {
    override fun byId(id: UserId): User {
        return byIdOrNull(id) ?: throw EntityNotFoundException()
    }

    override fun byIdOrNull(id: UserId): User? {
        return userRepository.findByIdOrNull(id.value)?.let { UserEntity.toBusiness(it) }
    }
}