package document

import domain.document.Document
import domain.document.DocumentId

interface GetDocument {
    fun byId(id: DocumentId): Document
}