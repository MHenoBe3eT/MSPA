package document

import domain.document.MedicalDocumentId
import domain.user.UserId

class DeleteMedicalDocumentUseCase(
    private val getMedicalDocument: GetMedicalDocument,
    private val deleteMedicalDocument: DeleteMedicalDocument,
) {
    operator fun invoke(id: MedicalDocumentId, userId: UserId) {
        getMedicalDocument.byId(id, userId)
        deleteMedicalDocument.delete(id)
    }
}
