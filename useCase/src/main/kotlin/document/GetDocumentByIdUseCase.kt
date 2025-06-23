package document

import domain.document.Document
import domain.document.DocumentId

class GetDocumentByIdUseCase(
    private val getDocument: GetDocument
) {
    operator fun invoke(id: DocumentId): Document = getDocument.byId(id)
}