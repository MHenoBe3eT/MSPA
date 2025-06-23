package document

import domain.document.DocumentId

class DeleteDocumentUseCase(
    val deleteDocument: DeleteDocument
) {
    operator fun invoke(id: DocumentId): Unit = deleteDocument.byId(id)
}