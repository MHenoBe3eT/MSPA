package document

import document.GetMedicalDocument
import document.PagedResult
import domain.DocumentType
import domain.document.MedicalDocument
import domain.document.MedicalDocumentId
import domain.file.UploadedFileId
import domain.user.UserId
import entity.MedicalDocumentEntity
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.AccessDeniedException
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

    override fun byId(id: MedicalDocumentId, userId: UserId): MedicalDocument {
        val document = byId(id)
        if (document.userId != userId) throw AccessDeniedException("Access denied to document ${id.value}")
        return document
    }

    override fun byUserId(
        userId: UserId,
        type: DocumentType?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        page: Int,
        size: Int,
    ): PagedResult<MedicalDocument> {
        val pageable = PageRequest.of(page, size)
        val result = repository.findByUserIdFiltered(userId.value, type, startDate, endDate, pageable)
        return PagedResult(
            content = result.content.map(MedicalDocumentEntity::toBusiness),
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = result.number,
            size = result.size,
        )
    }

    override fun byUploadedFileId(uploadedFileId: UploadedFileId): List<MedicalDocument> =
        repository.findByUploadedFileId(uploadedFileId.value).map(MedicalDocumentEntity::toBusiness)
}
