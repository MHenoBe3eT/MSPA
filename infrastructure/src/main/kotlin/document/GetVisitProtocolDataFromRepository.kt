package document

import domain.document.MedicalDocumentId
import domain.document.VisitProtocolData
import entity.VisitProtocolDataEntity
import org.springframework.stereotype.Component
import repository.ai.VisitProtocolDataRepository

@Component
class GetVisitProtocolDataFromRepository(
    private val repository: VisitProtocolDataRepository,
) : GetVisitProtocolData {

    override fun byMedicalDocumentId(medicalDocumentId: MedicalDocumentId): VisitProtocolData? =
        repository.findByMedicalDocumentId(medicalDocumentId.value)
            ?.let(VisitProtocolDataEntity::toBusiness)
}
