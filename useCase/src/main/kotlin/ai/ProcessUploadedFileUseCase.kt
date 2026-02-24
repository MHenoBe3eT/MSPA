package ai

import domain.ai.AiProcessingResult
import domain.ai.AiProcessingResultId
import domain.document.AiInterpretation
import domain.document.AiInterpretationId
import domain.document.InstrumentalStudyData
import domain.document.InstrumentalStudyDataId
import domain.document.LabAnalysisData
import domain.document.LabAnalysisDataId
import domain.document.LabIndicator
import domain.document.MedicalDocument
import domain.document.MedicalDocumentId
import domain.document.MedicalDocumentStatus
import domain.document.VisitProtocolData
import domain.document.VisitProtocolDataId
import domain.file.UploadedFileId
import domain.file.UploadStatus
import file.FileStorage
import file.GetUploadedFile
import file.UpdateUploadedFileStatus
import mu.KotlinLogging
import java.time.Instant

private val log = KotlinLogging.logger {}

class ProcessUploadedFileUseCase(
    private val getUploadedFile: GetUploadedFile,
    private val fileStorage: FileStorage,
    private val updateUploadedFileStatus: UpdateUploadedFileStatus,
    private val aiDocumentProvider: AiDocumentProvider,
    private val saveAiProcessingResult: SaveAiProcessingResult,
    private val saveMedicalDocument: SaveMedicalDocument,
    private val saveAiInterpretation: SaveAiInterpretation,
    private val saveLabAnalysisData: SaveLabAnalysisData,
    private val saveVisitProtocolData: SaveVisitProtocolData,
    private val saveInstrumentalStudyData: SaveInstrumentalStudyData,
) {
    operator fun invoke(uploadedFileId: UploadedFileId) {
        val uploadedFile = getUploadedFile.byId(uploadedFileId)
        log.info { "Starting AI processing for file ${uploadedFileId.value}" }

        updateUploadedFileStatus.update(uploadedFileId, UploadStatus.PROCESSING)

        try {
            val fileContent = fileStorage.retrieve(uploadedFile.storageKey)

            val extraction = aiDocumentProvider.processDocument(
                fileContent = fileContent,
                contentType = uploadedFile.contentType,
                fileName = uploadedFile.originalFileName,
            )

            saveAiProcessingResult.save(
                AiProcessingResult(
                    id = AiProcessingResultId.generateId(),
                    uploadedFileId = uploadedFileId,
                    rawExtractedText = extraction.rawText,
                    modelVersion = aiDocumentProvider.modelVersion,
                    processedAt = Instant.now(),
                )
            )

            for (doc in extraction.documents) {
                val docId = MedicalDocumentId.generateId()

                saveMedicalDocument.save(
                    MedicalDocument(
                        id = docId,
                        uploadedFileId = uploadedFileId,
                        userId = uploadedFile.userId,
                        documentType = doc.type,
                        status = MedicalDocumentStatus.PROCESSED,
                        title = doc.title,
                        documentDate = doc.documentDate,
                        createdAt = Instant.now(),
                    )
                )

                when (val data = doc.structuredData) {
                    is DocumentStructuredData.Lab -> saveLabAnalysisData.save(
                        LabAnalysisData(
                            id = LabAnalysisDataId.generateId(),
                            medicalDocumentId = docId,
                            indicators = data.indicators.map {
                                LabIndicator(
                                    name = it.name,
                                    code = it.code,
                                    value = it.value,
                                    unit = it.unit,
                                    referenceRange = it.referenceRange,
                                )
                            },
                        )
                    )

                    is DocumentStructuredData.VisitProtocol -> saveVisitProtocolData.save(
                        VisitProtocolData(
                            id = VisitProtocolDataId.generateId(),
                            medicalDocumentId = docId,
                            narrativeText = data.narrativeText,
                            complaints = data.complaints,
                            anamnesis = data.anamnesis,
                            diagnosis = data.diagnosis,
                            treatmentPlan = data.treatmentPlan,
                        )
                    )

                    is DocumentStructuredData.InstrumentalStudy -> saveInstrumentalStudyData.save(
                        InstrumentalStudyData(
                            id = InstrumentalStudyDataId.generateId(),
                            medicalDocumentId = docId,
                            description = data.description,
                            findings = data.findings,
                        )
                    )
                }

                saveAiInterpretation.save(
                    AiInterpretation(
                        id = AiInterpretationId.generateId(),
                        medicalDocumentId = docId,
                        interpretationText = doc.interpretationText,
                        riskMarkers = doc.riskMarkers,
                        disclaimer = doc.disclaimer,
                        modelVersion = aiDocumentProvider.modelVersion,
                    )
                )
            }

            updateUploadedFileStatus.update(uploadedFileId, UploadStatus.PROCESSED)
            log.info { "AI processing completed for file ${uploadedFileId.value}, created ${extraction.documents.size} documents" }
        } catch (e: Exception) {
            log.error(e) { "AI processing failed for file ${uploadedFileId.value}" }
            updateUploadedFileStatus.update(uploadedFileId, UploadStatus.ERROR)
            throw e
        }
    }
}
