package entity

import domain.DocumentType
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "document")
class DocumentEntity(
    @Id
    var id: UUID,

    @Column(name = "document_type")
    @Enumerated(EnumType.STRING)
    var documentType: DocumentType,

    var title: String,
    var creationDate: LocalDate,
    var visitDate: LocalDate,
    var filePath: String,
    var extractedText: String
) {
}