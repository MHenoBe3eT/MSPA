package ai

import domain.ai.AiProcessingResult

interface SaveAiProcessingResult {
    fun save(result: AiProcessingResult): AiProcessingResult
}
