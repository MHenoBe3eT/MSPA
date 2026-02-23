package ai

import domain.document.AiInterpretation

interface SaveAiInterpretation {
    fun save(interpretation: AiInterpretation): AiInterpretation
}
