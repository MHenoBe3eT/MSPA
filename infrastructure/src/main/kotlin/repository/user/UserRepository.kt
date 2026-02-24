package repository.user

import entity.UserEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import java.util.*

interface UserRepository : PagingAndSortingRepository<UserEntity, UUID>, CrudRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?
}