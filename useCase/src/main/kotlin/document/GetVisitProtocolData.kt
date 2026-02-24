package document

import domain.document.MedicalDocumentId
import domain.document.VisitProtocolData

interface GetVisitProtocolData {
    fun byMedicalDocumentId(medicalDocumentId: MedicalDocumentId): VisitProtocolData?
}
