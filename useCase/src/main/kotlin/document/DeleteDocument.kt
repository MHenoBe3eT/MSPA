package document

import domain.document.DocumentId

interface DeleteDocument {
    fun byId(id: DocumentId)
}