package ai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import domain.DocumentType
import mu.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.ByteArrayResource
import org.springframework.stereotype.Component
import org.springframework.util.MimeType
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(name = ["mspa.ai.provider"], havingValue = "deepseek")
class DeepSeekAiDocumentProvider(chatClientBuilder: ChatClient.Builder) : AiDocumentProvider {

    private val chatClient = chatClientBuilder.build()
    private val objectMapper = jacksonObjectMapper()

    override val modelVersion: String = "deepseek-chat"

    override fun processDocument(fileContent: ByteArray, contentType: String, fileName: String): AiExtractionResult {
        log.info { "DeepSeek AI processing file: $fileName ($contentType, ${fileContent.size} bytes)" }

        val rawResponse = chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user { spec ->
                spec.text("Extract medical document information from the attached file: $fileName")
                    .media(MimeType.valueOf(contentType), ByteArrayResource(fileContent))
            }
            .call()
            .content() ?: throw IllegalStateException("DeepSeek returned empty response for file: $fileName")

        log.debug { "DeepSeek raw response for $fileName: $rawResponse" }

        val jsonText = extractJson(rawResponse)
        val response: DeepSeekResponse = objectMapper.readValue(jsonText)

        val documents = response.documents.map { doc ->
            ExtractedDocument(
                type = mapDocumentType(doc.type),
                title = doc.title,
                documentDate = parseDate(doc.documentDate),
                structuredData = mapStructuredData(doc),
                interpretationText = doc.interpretationText,
                riskMarkers = doc.riskMarkers,
                disclaimer = doc.disclaimer,
            )
        }

        return AiExtractionResult(rawText = rawResponse, documents = documents)
    }

    private fun mapDocumentType(type: String): DocumentType = when (type.uppercase()) {
        "LAB_ANALYSIS" -> DocumentType.LAB_ANALYSIS
        "VISIT_PROTOCOL" -> DocumentType.VISIT_PROTOCOL
        "INSTRUMENTAL_STUDY" -> DocumentType.INSTRUMENTAL_STUDY
        else -> {
            log.warn { "Unknown document type from DeepSeek: '$type', defaulting to LAB_ANALYSIS" }
            DocumentType.LAB_ANALYSIS
        }
    }

    private fun parseDate(dateStr: String?): LocalDate =
        runCatching { LocalDate.parse(dateStr) }.getOrDefault(LocalDate.now())

    private fun mapStructuredData(doc: DeepSeekDoc): DocumentStructuredData = when (doc.type.uppercase()) {
        "LAB_ANALYSIS" -> DocumentStructuredData.Lab(
            indicators = doc.indicators.orEmpty().map {
                LabIndicatorDto(
                    name = it.name,
                    code = it.code,
                    value = it.value,
                    unit = it.unit,
                    referenceRange = it.referenceRange,
                )
            }
        )
        "VISIT_PROTOCOL" -> DocumentStructuredData.VisitProtocol(
            narrativeText = doc.narrativeText.orEmpty(),
            complaints = doc.complaints,
            anamnesis = doc.anamnesis,
            diagnosis = doc.diagnosis,
            treatmentPlan = doc.treatmentPlan,
        )
        else -> DocumentStructuredData.InstrumentalStudy(
            description = doc.description.orEmpty(),
            findings = doc.findings,
        )
    }

    private fun extractJson(raw: String): String {
        val codeBlockRegex = Regex("```(?:json)?\\s*\\n?(.*?)\\n?```", RegexOption.DOT_MATCHES_ALL)
        return codeBlockRegex.find(raw)?.groupValues?.get(1)?.trim() ?: raw.trim()
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class DeepSeekResponse(val documents: List<DeepSeekDoc> = emptyList())

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class DeepSeekDoc(
        val type: String = "",
        val title: String = "",
        val documentDate: String? = null,
        val interpretationText: String = "",
        val riskMarkers: List<String> = emptyList(),
        val disclaimer: String = "",
        val indicators: List<DeepSeekIndicator>? = null,
        val narrativeText: String? = null,
        val complaints: String? = null,
        val anamnesis: String? = null,
        val diagnosis: String? = null,
        val treatmentPlan: String? = null,
        val description: String? = null,
        val findings: Map<String, String>? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class DeepSeekIndicator(
        val name: String = "",
        val code: String? = null,
        val value: String = "",
        val unit: String? = null,
        val referenceRange: String? = null,
    )

    private companion object {
        val SYSTEM_PROMPT = """
            You are a medical document analysis AI. Your task is to extract structured information from medical documents.
            Analyze the provided document and return ONLY a valid JSON object (no markdown code blocks, no explanations, just JSON).

            JSON structure:
            {
              "documents": [
                {
                  "type": "LAB_ANALYSIS | VISIT_PROTOCOL | INSTRUMENTAL_STUDY",
                  "title": "descriptive document title",
                  "documentDate": "YYYY-MM-DD",
                  "interpretationText": "detailed interpretation of results",
                  "riskMarkers": ["abnormal finding 1", "abnormal finding 2"],
                  "disclaimer": "standard medical disclaimer text",

                  For LAB_ANALYSIS include:
                  "indicators": [
                    {"name": "indicator name", "code": "abbreviation or null", "value": "numeric value as string", "unit": "measurement unit or null", "referenceRange": "normal range or null"}
                  ],

                  For VISIT_PROTOCOL include:
                  "narrativeText": "full narrative text",
                  "complaints": "patient complaints or null",
                  "anamnesis": "medical history or null",
                  "diagnosis": "diagnosis or null",
                  "treatmentPlan": "treatment plan or null",

                  For INSTRUMENTAL_STUDY include:
                  "description": "study description",
                  "findings": {"finding_key": "finding_value"}
                }
              ]
            }

            Rules:
            - Return ONLY valid JSON, nothing else
            - documentDate must be YYYY-MM-DD (use today if unknown)
            - riskMarkers is [] when no abnormalities found
            - All text fields must be in the same language as the document
            - disclaimer must state that results are informational only and not medical advice
        """.trimIndent()
    }
}
