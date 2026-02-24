package ai

import domain.document.AiInterpretation
import entity.AiInterpretationEntity
import org.springframework.stereotype.Component
import repository.ai.AiInterpretationRepository

@Component
class SaveAiInterpretationInRepository(
    private val repository: AiInterpretationRepository,
) : SaveAiInterpretation {
    override fun save(interpretation: AiInterpretation): AiInterpretation =
        AiInterpretationEntity.toBusiness(repository.save(AiInterpretationEntity.fromBusiness(interpretation)))
}
