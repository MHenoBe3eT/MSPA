package document

import domain.document.MedicalDocument
import entity.MedicalDocumentEntity
import org.springframework.stereotype.Component
import repository.ai.MedicalDocumentRepository

@Component
class UpdateMedicalDocumentInRepository(
    private val repository: MedicalDocumentRepository,
) : UpdateMedicalDocument {

    override fun update(document: MedicalDocument): MedicalDocument =
        MedicalDocumentEntity.toBusiness(repository.save(MedicalDocumentEntity.fromBusiness(document)))
}
