package file

import domain.file.UploadedFile
import domain.file.UploadedFileId
import domain.file.UploadStatus
import entity.UploadedFileEntity
import org.springframework.stereotype.Component
import repository.file.UploadedFileRepository

@Component
class UpdateUploadedFileStatusInRepository(
    private val repository: UploadedFileRepository,
) : UpdateUploadedFileStatus {
    override fun update(id: UploadedFileId, status: UploadStatus): UploadedFile {
        val entity = repository.findById(id.value)
            .orElseThrow { NoSuchElementException("UploadedFile not found: ${id.value}") }
        entity.status = status
        return UploadedFileEntity.toBusiness(repository.save(entity))
    }
}
