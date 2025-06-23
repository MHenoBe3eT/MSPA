package document

import domain.DocumentType
import domain.document.Document
import domain.document.DocumentId
import java.time.LocalDate

class UpdateDocumentByIdUseCase(
    private val updateDocument: UpdateDocument,
    private val getDocument: GetDocument
) {
    fun invoke(
        id: DocumentId,
        documentType: DocumentType,
        title: String,
        visitDate: LocalDate,
        filePath: String,
        extractedText: String
    ): Document {
        var document = getDocument.byId(id)
        document = Document(
            id = document.id,
            documentType = documentType,
            title = title,
            creationDate = document.creationDate,
            visitDate = visitDate,
            filePath = filePath,
            extractedText = extractedText
        )
        return updateDocument.update(document)
    }
}