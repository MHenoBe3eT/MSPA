package document

import domain.document.LabAnalysisData
import domain.document.MedicalDocumentId

interface GetLabAnalysisData {
    fun byMedicalDocumentId(medicalDocumentId: MedicalDocumentId): LabAnalysisData?
}
