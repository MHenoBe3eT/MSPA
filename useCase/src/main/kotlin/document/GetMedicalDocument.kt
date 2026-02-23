package document

import domain.DocumentType
import domain.document.MedicalDocument
import domain.document.MedicalDocumentId
import domain.file.UploadedFileId
import domain.user.UserId
import java.time.LocalDate

interface GetMedicalDocument {
    fun byId(id: MedicalDocumentId): MedicalDocument
    fun byUserId(
        userId: UserId,
        type: DocumentType? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
    ): List<MedicalDocument>
    fun byUploadedFileId(uploadedFileId: UploadedFileId): List<MedicalDocument>
}
