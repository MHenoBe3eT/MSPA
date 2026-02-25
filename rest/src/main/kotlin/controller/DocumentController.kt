package controller

import document.DeleteMedicalDocumentUseCase
import document.DocumentDetails
import document.GetDocumentDetailsUseCase
import document.GetMedicalDocument
import document.PagedResult
import document.UpdateDocumentTypeUseCase
import domain.DocumentType
import domain.document.AiInterpretation
import domain.document.InstrumentalStudyData
import domain.document.LabAnalysisData
import domain.document.MedicalDocument
import domain.document.MedicalDocumentId
import domain.document.VisitProtocolData
import domain.user.UserId
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/documents")
class DocumentController(
    private val getMedicalDocument: GetMedicalDocument,
    private val getDocumentDetailsUseCase: GetDocumentDetailsUseCase,
    private val updateDocumentTypeUseCase: UpdateDocumentTypeUseCase,
    private val deleteMedicalDocumentUseCase: DeleteMedicalDocumentUseCase,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication,
    ): ResponseEntity<PagedResponse<MedicalDocumentResponse>> {
        val userId = authentication.principal as UserId
        val documentType = type?.let { DocumentType.valueOf(it) }
        val start = startDate?.let { LocalDate.parse(it) }
        val end = endDate?.let { LocalDate.parse(it) }
        val result = getMedicalDocument.byUserId(userId, documentType, start, end, page, size)
        return ResponseEntity.ok(PagedResponse.from(result, MedicalDocumentResponse::fromDomain))
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): ResponseEntity<DocumentDetailsResponse> {
        val userId = authentication.principal as UserId
        val details = getDocumentDetailsUseCase(MedicalDocumentId(id), userId)
        return ResponseEntity.ok(DocumentDetailsResponse.fromDomain(details))
    }

    @PatchMapping("/{id}")
    fun updateType(
        @PathVariable id: UUID,
        @RequestBody request: UpdateDocumentTypeRequest,
        authentication: Authentication,
    ): ResponseEntity<MedicalDocumentResponse> {
        val userId = authentication.principal as UserId
        val document = updateDocumentTypeUseCase(MedicalDocumentId(id), DocumentType.valueOf(request.documentType), userId)
        return ResponseEntity.ok(MedicalDocumentResponse.fromDomain(document))
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): ResponseEntity<Void> {
        val userId = authentication.principal as UserId
        deleteMedicalDocumentUseCase(MedicalDocumentId(id), userId)
        return ResponseEntity.noContent().build()
    }
}

data class MedicalDocumentResponse(
    val id: String,
    val uploadedFileId: String,
    val userId: String,
    val documentType: String,
    val status: String,
    val title: String,
    val documentDate: String,
    val createdAt: String,
) {
    companion object {
        fun fromDomain(d: MedicalDocument) = MedicalDocumentResponse(
            id = d.id.value.toString(),
            uploadedFileId = d.uploadedFileId.value.toString(),
            userId = d.userId.value.toString(),
            documentType = d.documentType.name,
            status = d.status.name,
            title = d.title,
            documentDate = d.documentDate.toString(),
            createdAt = d.createdAt.toString(),
        )
    }
}

data class DocumentDetailsResponse(
    val id: String,
    val uploadedFileId: String,
    val userId: String,
    val documentType: String,
    val status: String,
    val title: String,
    val documentDate: String,
    val createdAt: String,
    val interpretation: AiInterpretationResponse?,
    val labData: LabAnalysisDataResponse?,
    val visitProtocolData: VisitProtocolDataResponse?,
    val instrumentalStudyData: InstrumentalStudyDataResponse?,
) {
    companion object {
        fun fromDomain(details: DocumentDetails) = DocumentDetailsResponse(
            id = details.document.id.value.toString(),
            uploadedFileId = details.document.uploadedFileId.value.toString(),
            userId = details.document.userId.value.toString(),
            documentType = details.document.documentType.name,
            status = details.document.status.name,
            title = details.document.title,
            documentDate = details.document.documentDate.toString(),
            createdAt = details.document.createdAt.toString(),
            interpretation = details.interpretation?.let(AiInterpretationResponse::fromDomain),
            labData = (details.structuredData as? DocumentDetails.StructuredData.Lab)
                ?.data?.let(LabAnalysisDataResponse::fromDomain),
            visitProtocolData = (details.structuredData as? DocumentDetails.StructuredData.VisitProtocol)
                ?.data?.let(VisitProtocolDataResponse::fromDomain),
            instrumentalStudyData = (details.structuredData as? DocumentDetails.StructuredData.InstrumentalStudy)
                ?.data?.let(InstrumentalStudyDataResponse::fromDomain),
        )
    }
}

data class AiInterpretationResponse(
    val interpretationText: String,
    val riskMarkers: List<String>,
    val disclaimer: String,
    val modelVersion: String,
) {
    companion object {
        fun fromDomain(i: AiInterpretation) = AiInterpretationResponse(
            interpretationText = i.interpretationText,
            riskMarkers = i.riskMarkers,
            disclaimer = i.disclaimer,
            modelVersion = i.modelVersion,
        )
    }
}

data class LabIndicatorResponse(
    val name: String,
    val code: String?,
    val value: String,
    val unit: String?,
    val referenceRange: String?,
)

data class LabAnalysisDataResponse(
    val indicators: List<LabIndicatorResponse>,
) {
    companion object {
        fun fromDomain(d: LabAnalysisData) = LabAnalysisDataResponse(
            indicators = d.indicators.map {
                LabIndicatorResponse(
                    name = it.name,
                    code = it.code,
                    value = it.value,
                    unit = it.unit,
                    referenceRange = it.referenceRange,
                )
            },
        )
    }
}

data class VisitProtocolDataResponse(
    val narrativeText: String,
    val complaints: String?,
    val anamnesis: String?,
    val diagnosis: String?,
    val treatmentPlan: String?,
) {
    companion object {
        fun fromDomain(d: VisitProtocolData) = VisitProtocolDataResponse(
            narrativeText = d.narrativeText,
            complaints = d.complaints,
            anamnesis = d.anamnesis,
            diagnosis = d.diagnosis,
            treatmentPlan = d.treatmentPlan,
        )
    }
}

data class InstrumentalStudyDataResponse(
    val description: String,
    val findings: Map<String, String>?,
) {
    companion object {
        fun fromDomain(d: InstrumentalStudyData) = InstrumentalStudyDataResponse(
            description = d.description,
            findings = d.findings,
        )
    }
}

data class UpdateDocumentTypeRequest(val documentType: String)

data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
) {
    companion object {
        fun <T, R> from(result: PagedResult<T>, mapper: (T) -> R) = PagedResponse(
            content = result.content.map(mapper),
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = result.page,
            size = result.size,
        )
    }
}
