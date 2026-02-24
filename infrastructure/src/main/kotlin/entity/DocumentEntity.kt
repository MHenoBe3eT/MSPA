package entity

import domain.DocumentType
import domain.document.Document
import domain.document.DocumentId
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "documents")
class DocumentEntity(
    @Id
    var id: UUID,

    @Column(name = "document_type")
    @Enumerated(EnumType.STRING)
    var documentType: DocumentType,

    @Column(name = "title")
    var title: String,

    @Column(name = "creation_date")
    var creationDate: LocalDate,

    @Column(name = "visit_date")
    var visitDate: LocalDate,

    @Column(name = "file_path")
    var filePath: String,

    @Column(name = "extracted_text")
    var extractedText: String
) {
    companion object {
        fun toBusiness(e: DocumentEntity): Document {
            return Document(
                id = DocumentId(e.id),
                documentType = e.documentType,
                title = e.title,
                creationDate = e.creationDate,
                visitDate = e.visitDate,
                filePath = e.filePath,
                extractedText = e.extractedText
            )
        }

        fun fromBusiness(document: Document): DocumentEntity {
            return DocumentEntity(
                id = document.id.value,
                documentType = document.documentType,
                title = document.title,
                creationDate = document.creationDate,
                visitDate = document.visitDate,
                filePath = document.filePath,
                extractedText = document.extractedText
            )
        }
    }
}