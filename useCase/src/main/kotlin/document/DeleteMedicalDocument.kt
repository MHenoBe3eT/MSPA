package document

import domain.document.MedicalDocumentId

interface DeleteMedicalDocument {
    fun delete(id: MedicalDocumentId)
}
