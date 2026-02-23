package repository.file

import entity.UploadedFileEntity
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface UploadedFileRepository : CrudRepository<UploadedFileEntity, UUID> {
    fun findByChecksumAndUserId(checksum: String, userId: UUID): UploadedFileEntity?
}
