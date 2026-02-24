package ai

import domain.document.LabAnalysisData

interface SaveLabAnalysisData {
    fun save(data: LabAnalysisData): LabAnalysisData
}
