package document

import domain.document.Document

interface UpdateDocument {
    fun update(document: Document): Document
}