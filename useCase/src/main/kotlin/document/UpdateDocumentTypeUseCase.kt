package document

import domain.DocumentType
import domain.document.MedicalDocument
import domain.document.MedicalDocumentId
import domain.user.UserId

class UpdateDocumentTypeUseCase(
    private val getMedicalDocument: GetMedicalDocument,
    private val updateMedicalDocument: UpdateMedicalDocument,
) {
    operator fun invoke(id: MedicalDocumentId, newType: DocumentType, userId: UserId): MedicalDocument {
        val document = getMedicalDocument.byId(id, userId)
        return updateMedicalDocument.update(document.copy(documentType = newType))
    }
}
