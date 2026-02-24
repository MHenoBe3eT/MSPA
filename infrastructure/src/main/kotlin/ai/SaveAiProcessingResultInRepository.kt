package ai

import domain.ai.AiProcessingResult
import entity.AiProcessingResultEntity
import org.springframework.stereotype.Component
import repository.ai.AiProcessingResultRepository

@Component
class SaveAiProcessingResultInRepository(
    private val repository: AiProcessingResultRepository,
) : SaveAiProcessingResult {
    override fun save(result: AiProcessingResult): AiProcessingResult =
        AiProcessingResultEntity.toBusiness(repository.save(AiProcessingResultEntity.fromBusiness(result)))
}
