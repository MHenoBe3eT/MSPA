package ai

import domain.DocumentType
import domain.document.LabAnalysisData
import domain.document.MedicalDocument
import domain.document.MedicalDocumentStatus
import domain.file.UploadStatus
import domain.file.UploadedFile
import domain.file.UploadedFileId
import domain.user.UserId
import file.FileStorage
import file.GetUploadedFile
import file.UpdateUploadedFileStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate

class ProcessUploadedFileUseCaseTest {

    private lateinit var getUploadedFile: GetUploadedFile
    private lateinit var fileStorage: FileStorage
    private lateinit var updateUploadedFileStatus: UpdateUploadedFileStatus
    private lateinit var aiDocumentProvider: AiDocumentProvider
    private lateinit var saveAiProcessingResult: SaveAiProcessingResult
    private lateinit var saveMedicalDocument: SaveMedicalDocument
    private lateinit var saveAiInterpretation: SaveAiInterpretation
    private lateinit var saveLabAnalysisData: SaveLabAnalysisData
    private lateinit var saveVisitProtocolData: SaveVisitProtocolData
    private lateinit var saveInstrumentalStudyData: SaveInstrumentalStudyData
    private lateinit var useCase: ProcessUploadedFileUseCase

    private val userId = UserId.generateId()
    private val fileId = UploadedFileId.generateId()
    private val fileBytes = "test file content".toByteArray()

    private val uploadedFile = UploadedFile(
        id = fileId,
        userId = userId,
        originalFileName = "test.pdf",
        contentType = "application/pdf",
        sizeBytes = fileBytes.size.toLong(),
        checksum = "abc123",
        storageKey = "${userId.value}/${fileId.value}/test.pdf",
        status = UploadStatus.UPLOADED,
        uploadedAt = Instant.now(),
    )

    @BeforeEach
    fun setUp() {
        getUploadedFile = mockk()
        fileStorage = mockk()
        updateUploadedFileStatus = mockk(relaxed = true)
        aiDocumentProvider = mockk()
        saveAiProcessingResult = mockk(relaxed = true)
        saveMedicalDocument = mockk()
        saveAiInterpretation = mockk(relaxed = true)
        saveLabAnalysisData = mockk(relaxed = true)
        saveVisitProtocolData = mockk(relaxed = true)
        saveInstrumentalStudyData = mockk(relaxed = true)

        useCase = ProcessUploadedFileUseCase(
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

    private fun makeLabExtraction(count: Int = 1) = AiExtractionResult(
        rawText = "raw text",
        documents = (1..count).map { i ->
            ExtractedDocument(
                type = DocumentType.LAB_ANALYSIS,
                title = "Lab Result $i",
                documentDate = LocalDate.now(),
                structuredData = DocumentStructuredData.Lab(
                    indicators = listOf(
                        LabIndicatorDto(name = "Hemoglobin", code = "HGB", value = "14.2", unit = "g/dL", referenceRange = "13.5-17.5")
                    )
                ),
                interpretationText = "Normal",
                riskMarkers = emptyList(),
                disclaimer = "Not medical advice",
            )
        }
    )

    @Test
    fun `should transition status from UPLOADED to PROCESSING then PROCESSED on success`() {
        val statusSlots = mutableListOf<UploadStatus>()
        every { getUploadedFile.byId(fileId) } returns uploadedFile
        every { fileStorage.retrieve(uploadedFile.storageKey) } returns fileBytes
        every { aiDocumentProvider.modelVersion } returns "stub-1.0"
        every { aiDocumentProvider.processDocument(any(), any(), any()) } returns makeLabExtraction()
        every { saveMedicalDocument.save(any()) } answers { firstArg() }
        every { updateUploadedFileStatus.update(fileId, capture(statusSlots)) } returns uploadedFile

        useCase(fileId)

        assertEquals(2, statusSlots.size)
        assertEquals(UploadStatus.PROCESSING, statusSlots[0])
        assertEquals(UploadStatus.PROCESSED, statusSlots[1])
    }

    @Test
    fun `should create N MedicalDocuments when AI returns N extracted documents`() {
        val docSlots = mutableListOf<MedicalDocument>()
        every { getUploadedFile.byId(fileId) } returns uploadedFile
        every { fileStorage.retrieve(any()) } returns fileBytes
        every { aiDocumentProvider.modelVersion } returns "stub-1.0"
        every { aiDocumentProvider.processDocument(any(), any(), any()) } returns makeLabExtraction(count = 3)
        every { saveMedicalDocument.save(capture(docSlots)) } answers { firstArg() }
        every { updateUploadedFileStatus.update(any(), any()) } returns uploadedFile

        useCase(fileId)

        assertEquals(3, docSlots.size)
        docSlots.forEach { doc ->
            assertEquals(DocumentType.LAB_ANALYSIS, doc.documentType)
            assertEquals(MedicalDocumentStatus.PROCESSED, doc.status)
            assertEquals(userId, doc.userId)
            assertEquals(fileId, doc.uploadedFileId)
        }
    }

    @Test
    fun `should save structured data for each document type`() {
        val labSlot = slot<LabAnalysisData>()
        every { getUploadedFile.byId(fileId) } returns uploadedFile
        every { fileStorage.retrieve(any()) } returns fileBytes
        every { aiDocumentProvider.modelVersion } returns "stub-1.0"
        every { aiDocumentProvider.processDocument(any(), any(), any()) } returns makeLabExtraction()
        every { saveMedicalDocument.save(any()) } answers { firstArg() }
        every { saveLabAnalysisData.save(capture(labSlot)) } answers { firstArg() }
        every { updateUploadedFileStatus.update(any(), any()) } returns uploadedFile

        useCase(fileId)

        assertNotNull(labSlot.captured)
        assertEquals(1, labSlot.captured.indicators.size)
        assertEquals("Hemoglobin", labSlot.captured.indicators[0].name)
    }

    @Test
    fun `should save AiInterpretation for each document`() {
        every { getUploadedFile.byId(fileId) } returns uploadedFile
        every { fileStorage.retrieve(any()) } returns fileBytes
        every { aiDocumentProvider.modelVersion } returns "stub-1.0"
        every { aiDocumentProvider.processDocument(any(), any(), any()) } returns makeLabExtraction(count = 2)
        every { saveMedicalDocument.save(any()) } answers { firstArg() }
        every { updateUploadedFileStatus.update(any(), any()) } returns uploadedFile

        useCase(fileId)

        verify(exactly = 2) { saveAiInterpretation.save(any()) }
    }

    @Test
    fun `should set status to ERROR and rethrow when AI processing fails`() {
        val statusSlots = mutableListOf<UploadStatus>()
        every { getUploadedFile.byId(fileId) } returns uploadedFile
        every { fileStorage.retrieve(any()) } returns fileBytes
        every { aiDocumentProvider.modelVersion } returns "stub-1.0"
        every { aiDocumentProvider.processDocument(any(), any(), any()) } throws RuntimeException("AI failure")
        every { updateUploadedFileStatus.update(fileId, capture(statusSlots)) } returns uploadedFile

        assertThrows<RuntimeException> { useCase(fileId) }

        assertEquals(2, statusSlots.size)
        assertEquals(UploadStatus.PROCESSING, statusSlots[0])
        assertEquals(UploadStatus.ERROR, statusSlots[1])
        verify(exactly = 0) { saveMedicalDocument.save(any()) }
    }

    @Test
    fun `should call AI provider with correct file metadata`() {
        val contentTypeSlot = slot<String>()
        val fileNameSlot = slot<String>()
        every { getUploadedFile.byId(fileId) } returns uploadedFile
        every { fileStorage.retrieve(any()) } returns fileBytes
        every { aiDocumentProvider.modelVersion } returns "stub-1.0"
        every { aiDocumentProvider.processDocument(any(), capture(contentTypeSlot), capture(fileNameSlot)) } returns makeLabExtraction()
        every { saveMedicalDocument.save(any()) } answers { firstArg() }
        every { updateUploadedFileStatus.update(any(), any()) } returns uploadedFile

        useCase(fileId)

        assertEquals("application/pdf", contentTypeSlot.captured)
        assertEquals("test.pdf", fileNameSlot.captured)
    }
}
