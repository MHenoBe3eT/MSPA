package document

import domain.DocumentType
import domain.document.MedicalDocument
import domain.document.MedicalDocumentId

class UpdateDocumentTypeUseCase(
    private val getMedicalDocument: GetMedicalDocument,
    private val updateMedicalDocument: UpdateMedicalDocument,
) {
    operator fun invoke(id: MedicalDocumentId, newType: DocumentType): MedicalDocument {
        val document = getMedicalDocument.byId(id)
        return updateMedicalDocument.update(document.copy(documentType = newType))
    }
}
