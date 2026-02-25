package document

import domain.DocumentType
import domain.document.MedicalDocumentId
import domain.user.UserId

class GetDocumentDetailsUseCase(
    private val getMedicalDocument: GetMedicalDocument,
    private val getAiInterpretation: GetAiInterpretation,
    private val getLabAnalysisData: GetLabAnalysisData,
    private val getVisitProtocolData: GetVisitProtocolData,
    private val getInstrumentalStudyData: GetInstrumentalStudyData,
) {
    operator fun invoke(id: MedicalDocumentId, userId: UserId): DocumentDetails {
        val document = getMedicalDocument.byId(id, userId)
        val interpretation = getAiInterpretation.byMedicalDocumentId(id)
        val structuredData = when (document.documentType) {
            DocumentType.LAB_ANALYSIS -> getLabAnalysisData.byMedicalDocumentId(id)
                ?.let { DocumentDetails.StructuredData.Lab(it) }
            DocumentType.VISIT_PROTOCOL -> getVisitProtocolData.byMedicalDocumentId(id)
                ?.let { DocumentDetails.StructuredData.VisitProtocol(it) }
            DocumentType.INSTRUMENTAL_STUDY -> getInstrumentalStudyData.byMedicalDocumentId(id)
                ?.let { DocumentDetails.StructuredData.InstrumentalStudy(it) }
        }
        return DocumentDetails(
            document = document,
            structuredData = structuredData,
            interpretation = interpretation,
        )
    }
}
