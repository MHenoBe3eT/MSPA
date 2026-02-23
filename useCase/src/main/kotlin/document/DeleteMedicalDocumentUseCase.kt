package document

import domain.document.MedicalDocumentId

class DeleteMedicalDocumentUseCase(
    private val getMedicalDocument: GetMedicalDocument,
    private val deleteMedicalDocument: DeleteMedicalDocument,
) {
    operator fun invoke(id: MedicalDocumentId) {
        getMedicalDocument.byId(id)
        deleteMedicalDocument.delete(id)
    }
}
