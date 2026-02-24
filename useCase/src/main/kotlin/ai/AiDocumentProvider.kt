package ai

import domain.DocumentType
import java.time.LocalDate

interface AiDocumentProvider {
    val modelVersion: String
    fun processDocument(fileContent: ByteArray, contentType: String, fileName: String): AiExtractionResult
}

data class AiExtractionResult(
    val rawText: String,
    val documents: List<ExtractedDocument>,
)

data class ExtractedDocument(
    val type: DocumentType,
    val title: String,
    val documentDate: LocalDate,
    val structuredData: DocumentStructuredData,
    val interpretationText: String,
    val riskMarkers: List<String>,
    val disclaimer: String,
)

sealed class DocumentStructuredData {
    data class Lab(
        val indicators: List<LabIndicatorDto>,
    ) : DocumentStructuredData()

    data class VisitProtocol(
        val narrativeText: String,
        val complaints: String?,
        val anamnesis: String?,
        val diagnosis: String?,
        val treatmentPlan: String?,
    ) : DocumentStructuredData()

    data class InstrumentalStudy(
        val description: String,
        val findings: Map<String, String>?,
    ) : DocumentStructuredData()
}

data class LabIndicatorDto(
    val name: String,
    val code: String?,
    val value: String,
    val unit: String?,
    val referenceRange: String?,
)
