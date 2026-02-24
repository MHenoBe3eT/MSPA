package document

import domain.document.AiInterpretation
import domain.document.MedicalDocumentId
import entity.AiInterpretationEntity
import org.springframework.stereotype.Component
import repository.ai.AiInterpretationRepository

@Component
class GetAiInterpretationFromRepository(
    private val repository: AiInterpretationRepository,
) : GetAiInterpretation {

    override fun byMedicalDocumentId(medicalDocumentId: MedicalDocumentId): AiInterpretation? =
        repository.findByMedicalDocumentId(medicalDocumentId.value)
            ?.let(AiInterpretationEntity::toBusiness)
}
