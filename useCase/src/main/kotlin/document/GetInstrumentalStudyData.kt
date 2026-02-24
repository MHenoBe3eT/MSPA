package document

import domain.document.InstrumentalStudyData
import domain.document.MedicalDocumentId

interface GetInstrumentalStudyData {
    fun byMedicalDocumentId(medicalDocumentId: MedicalDocumentId): InstrumentalStudyData?
}
