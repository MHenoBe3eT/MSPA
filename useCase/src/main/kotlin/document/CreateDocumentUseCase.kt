package document

import domain.DocumentType
import domain.document.Document
import java.time.LocalDate

class CreateDocumentUseCase(
    private val createDocument: CreateDocument
) {
    fun invoke(
        id: String,
        documentType: DocumentType,
        title: String,
        visitDate: LocalDate,
        filePath: String,
        extractedText: String
    ): Document {
        val document = Document.createNew(
            id = id,
            documentType = documentType,
            title = title,
            creationDate = LocalDate.now(),
            visitDate = visitDate,
            filePath = filePath,
            extractedText = extractedText
        )
        return createDocument.create(document)
    }
}