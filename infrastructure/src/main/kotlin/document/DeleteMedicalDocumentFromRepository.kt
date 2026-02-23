package document

import domain.document.MedicalDocumentId
import org.springframework.stereotype.Component
import repository.ai.MedicalDocumentRepository

@Component
class DeleteMedicalDocumentFromRepository(
    private val repository: MedicalDocumentRepository,
) : DeleteMedicalDocument {

    override fun delete(id: MedicalDocumentId) = repository.deleteById(id.value)
}
