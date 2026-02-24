package document

import domain.document.AiInterpretation
import domain.document.MedicalDocumentId

interface GetAiInterpretation {
    fun byMedicalDocumentId(medicalDocumentId: MedicalDocumentId): AiInterpretation?
}
