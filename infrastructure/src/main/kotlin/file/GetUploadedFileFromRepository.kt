package file

import domain.file.UploadedFile
import domain.file.UploadedFileId
import domain.user.UserId
import entity.UploadedFileEntity
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import repository.file.UploadedFileRepository

@Transactional(readOnly = true)
@Component
class GetUploadedFileFromRepository(
    private val uploadedFileRepository: UploadedFileRepository,
) : GetUploadedFile {

    override fun byId(id: UploadedFileId): UploadedFile =
        byIdOrNull(id) ?: throw EntityNotFoundException("UploadedFile not found: ${id.value}")

    override fun byId(id: UploadedFileId, userId: UserId): UploadedFile {
        val file = byId(id)
        if (file.userId != userId) throw AccessDeniedException("Access denied to file ${id.value}")
        return file
    }

    override fun byIdOrNull(id: UploadedFileId): UploadedFile? =
        uploadedFileRepository.findByIdOrNull(id.value)?.let { UploadedFileEntity.toBusiness(it) }

    override fun byChecksumAndUserId(checksum: String, userId: UserId): UploadedFile? =
        uploadedFileRepository.findByChecksumAndUserId(checksum, userId.value)
            ?.let { UploadedFileEntity.toBusiness(it) }
}
