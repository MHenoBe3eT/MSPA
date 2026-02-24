package ai

import domain.document.InstrumentalStudyData

interface SaveInstrumentalStudyData {
    fun save(data: InstrumentalStudyData): InstrumentalStudyData
}
