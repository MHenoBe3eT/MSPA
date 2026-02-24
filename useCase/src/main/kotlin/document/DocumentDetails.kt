package document

import domain.document.AiInterpretation
import domain.document.InstrumentalStudyData
import domain.document.LabAnalysisData
import domain.document.MedicalDocument
import domain.document.VisitProtocolData

data class DocumentDetails(
    val document: MedicalDocument,
    val structuredData: StructuredData?,
    val interpretation: AiInterpretation?,
) {
    sealed class StructuredData {
        data class Lab(val data: LabAnalysisData) : StructuredData()
        data class VisitProtocol(val data: VisitProtocolData) : StructuredData()
        data class InstrumentalStudy(val data: InstrumentalStudyData) : StructuredData()
    }
}
