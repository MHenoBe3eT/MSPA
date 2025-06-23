package document

import domain.document.Document

interface CreateDocument {
    fun create(document: Document): Document
}