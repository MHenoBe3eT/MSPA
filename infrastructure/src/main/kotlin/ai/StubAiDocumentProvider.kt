package ai

import domain.DocumentType
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(name = ["mspa.ai.provider"], havingValue = "stub", matchIfMissing = true)
class StubAiDocumentProvider : AiDocumentProvider {

    override val modelVersion: String = "stub-1.0"

    override fun processDocument(fileContent: ByteArray, contentType: String, fileName: String): AiExtractionResult {
        log.info { "Stub AI provider processing file: $fileName ($contentType, ${fileContent.size} bytes)" }

        val rawText = """
            STUB AI EXTRACTION
            File: $fileName
            Content-Type: $contentType
            Size: ${fileContent.size} bytes

            CBC (Complete Blood Count):
            Hemoglobin: 14.2 g/dL (reference: 13.5-17.5)
            Hematocrit: 42% (reference: 41-53%)
            WBC: 7.2 x10^9/L (reference: 4.5-11.0)
            Platelets: 250 x10^9/L (reference: 150-400)
        """.trimIndent()

        val document = ExtractedDocument(
            type = DocumentType.LAB_ANALYSIS,
            title = "Complete Blood Count — $fileName",
            documentDate = LocalDate.now(),
            structuredData = DocumentStructuredData.Lab(
                indicators = listOf(
                    LabIndicatorDto(
                        name = "Hemoglobin",
                        code = "HGB",
                        value = "14.2",
                        unit = "g/dL",
                        referenceRange = "13.5-17.5",
                    ),
                    LabIndicatorDto(
                        name = "Hematocrit",
                        code = "HCT",
                        value = "42",
                        unit = "%",
                        referenceRange = "41-53",
                    ),
                    LabIndicatorDto(
                        name = "White Blood Cells",
                        code = "WBC",
                        value = "7.2",
                        unit = "x10^9/L",
                        referenceRange = "4.5-11.0",
                    ),
                    LabIndicatorDto(
                        name = "Platelets",
                        code = "PLT",
                        value = "250",
                        unit = "x10^9/L",
                        referenceRange = "150-400",
                    ),
                )
            ),
            interpretationText = "All blood count indicators are within normal reference ranges. No significant abnormalities detected.",
            riskMarkers = emptyList(),
            disclaimer = "This interpretation is generated automatically and is for informational purposes only. " +
                "It does not constitute medical advice and should not replace consultation with a qualified healthcare professional.",
        )

        return AiExtractionResult(rawText = rawText, documents = listOf(document))
    }
}
