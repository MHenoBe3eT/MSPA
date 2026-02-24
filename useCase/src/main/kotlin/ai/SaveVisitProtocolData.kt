package ai

import domain.document.VisitProtocolData

interface SaveVisitProtocolData {
    fun save(data: VisitProtocolData): VisitProtocolData
}
