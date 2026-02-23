package config

import document.DeleteMedicalDocument
import document.DeleteMedicalDocumentUseCase
import document.GetAiInterpretation
import document.GetDocumentDetailsUseCase
import document.GetInstrumentalStudyData
import document.GetLabAnalysisData
import document.GetMedicalDocument
import document.GetVisitProtocolData
import document.UpdateMedicalDocument
import document.UpdateDocumentTypeUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DocumentConfig {

    @Bean
    fun getDocumentDetailsUseCase(
        getMedicalDocument: GetMedicalDocument,
        getAiInterpretation: GetAiInterpretation,
        getLabAnalysisData: GetLabAnalysisData,
        getVisitProtocolData: GetVisitProtocolData,
        getInstrumentalStudyData: GetInstrumentalStudyData,
    ): GetDocumentDetailsUseCase = GetDocumentDetailsUseCase(
        getMedicalDocument = getMedicalDocument,
        getAiInterpretation = getAiInterpretation,
        getLabAnalysisData = getLabAnalysisData,
        getVisitProtocolData = getVisitProtocolData,
        getInstrumentalStudyData = getInstrumentalStudyData,
    )

    @Bean
    fun updateDocumentTypeUseCase(
        getMedicalDocument: GetMedicalDocument,
        updateMedicalDocument: UpdateMedicalDocument,
    ): UpdateDocumentTypeUseCase = UpdateDocumentTypeUseCase(
        getMedicalDocument = getMedicalDocument,
        updateMedicalDocument = updateMedicalDocument,
    )

    @Bean
    fun deleteMedicalDocumentUseCase(
        getMedicalDocument: GetMedicalDocument,
        deleteMedicalDocument: DeleteMedicalDocument,
    ): DeleteMedicalDocumentUseCase = DeleteMedicalDocumentUseCase(
        getMedicalDocument = getMedicalDocument,
        deleteMedicalDocument = deleteMedicalDocument,
    )
}
