package ai

import domain.document.MedicalDocument
import entity.MedicalDocumentEntity
import org.springframework.stereotype.Component
import repository.ai.MedicalDocumentRepository

@Component
class SaveMedicalDocumentInRepository(
    private val repository: MedicalDocumentRepository,
) : SaveMedicalDocument {
    override fun save(document: MedicalDocument): MedicalDocument =
        MedicalDocumentEntity.toBusiness(repository.save(MedicalDocumentEntity.fromBusiness(document)))
}
