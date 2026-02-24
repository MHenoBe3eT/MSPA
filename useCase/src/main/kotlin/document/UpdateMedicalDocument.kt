package document

import domain.document.MedicalDocument

interface UpdateMedicalDocument {
    fun update(document: MedicalDocument): MedicalDocument
}
