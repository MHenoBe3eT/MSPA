package document

import domain.DocumentType
import domain.document.MedicalDocument
import domain.document.MedicalDocumentId
import domain.file.UploadedFileId
import domain.user.UserId
import entity.MedicalDocumentEntity
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Component
import repository.ai.MedicalDocumentRepository
import java.time.LocalDate

@Component
class GetMedicalDocumentFromRepository(
    private val repository: MedicalDocumentRepository,
) : GetMedicalDocument {

    override fun byId(id: MedicalDocumentId): MedicalDocument =
        repository.findById(id.value)
            .map(MedicalDocumentEntity::toBusiness)
            .orElseThrow { EntityNotFoundException("MedicalDocument not found: ${id.value}") }

    override fun byUserId(
        userId: UserId,
        type: DocumentType?,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): List<MedicalDocument> {
        var results = repository.findByUserId(userId.value).map(MedicalDocumentEntity::toBusiness)
        if (type != null) results = results.filter { it.documentType == type }
        if (startDate != null) results = results.filter { !it.documentDate.isBefore(startDate) }
        if (endDate != null) results = results.filter { !it.documentDate.isAfter(endDate) }
        return results
    }

    override fun byUploadedFileId(uploadedFileId: UploadedFileId): List<MedicalDocument> =
        repository.findByUploadedFileId(uploadedFileId.value).map(MedicalDocumentEntity::toBusiness)
}
