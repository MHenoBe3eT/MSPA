package config

import ai.AiDocumentProvider
import ai.ProcessUploadedFileUseCase
import ai.SaveAiInterpretation
import ai.SaveAiProcessingResult
import ai.SaveInstrumentalStudyData
import ai.SaveLabAnalysisData
import ai.SaveMedicalDocument
import ai.SaveVisitProtocolData
import file.FileStorage
import file.GetUploadedFile
import file.UpdateUploadedFileStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiProcessingConfig {

    @Bean
    fun processUploadedFileUseCase(
        getUploadedFile: GetUploadedFile,
        fileStorage: FileStorage,
        updateUploadedFileStatus: UpdateUploadedFileStatus,
        aiDocumentProvider: AiDocumentProvider,
        saveAiProcessingResult: SaveAiProcessingResult,
        saveMedicalDocument: SaveMedicalDocument,
        saveAiInterpretation: SaveAiInterpretation,
        saveLabAnalysisData: SaveLabAnalysisData,
        saveVisitProtocolData: SaveVisitProtocolData,
        saveInstrumentalStudyData: SaveInstrumentalStudyData,
    ): ProcessUploadedFileUseCase = ProcessUploadedFileUseCase(
        getUploadedFile = getUploadedFile,
        fileStorage = fileStorage,
        updateUploadedFileStatus = updateUploadedFileStatus,
        aiDocumentProvider = aiDocumentProvider,
        saveAiProcessingResult = saveAiProcessingResult,
        saveMedicalDocument = saveMedicalDocument,
        saveAiInterpretation = saveAiInterpretation,
        saveLabAnalysisData = saveLabAnalysisData,
        saveVisitProtocolData = saveVisitProtocolData,
        saveInstrumentalStudyData = saveInstrumentalStudyData,
    )
}
