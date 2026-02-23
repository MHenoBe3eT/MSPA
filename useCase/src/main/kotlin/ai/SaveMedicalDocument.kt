package ai

import domain.document.MedicalDocument

interface SaveMedicalDocument {
    fun save(document: MedicalDocument): MedicalDocument
}
