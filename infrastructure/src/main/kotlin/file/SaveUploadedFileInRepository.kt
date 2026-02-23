package file

import domain.file.UploadedFile
import entity.UploadedFileEntity
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import repository.file.UploadedFileRepository

@Transactional
@Component
class SaveUploadedFileInRepository(
    private val uploadedFileRepository: UploadedFileRepository,
) : SaveUploadedFile {
    override fun save(uploadedFile: UploadedFile): UploadedFile {
        val entity = UploadedFileEntity.fromBusiness(uploadedFile)
        val saved = uploadedFileRepository.save(entity)
        return UploadedFileEntity.toBusiness(saved)
    }
}
