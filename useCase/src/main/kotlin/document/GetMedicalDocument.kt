package document

import domain.DocumentType
import domain.document.MedicalDocument
import domain.document.MedicalDocumentId
import domain.file.UploadedFileId
import domain.user.UserId
import java.time.LocalDate

interface GetMedicalDocument {
    fun byId(id: MedicalDocumentId): MedicalDocument
    fun byId(id: MedicalDocumentId, userId: UserId): MedicalDocument
    fun byUserId(
        userId: UserId,
        type: DocumentType? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        page: Int = 0,
        size: Int = 20,
    ): PagedResult<MedicalDocument>
    fun byUploadedFileId(uploadedFileId: UploadedFileId): List<MedicalDocument>
}
