package repository.ai

import domain.DocumentType
import entity.MedicalDocumentEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface MedicalDocumentRepository : JpaRepository<MedicalDocumentEntity, UUID> {

    @Query("""
        SELECT d FROM MedicalDocumentEntity d
        WHERE d.userId = :userId
          AND (:type IS NULL OR d.documentType = :type)
          AND (:startDate IS NULL OR d.documentDate >= :startDate)
          AND (:endDate IS NULL OR d.documentDate <= :endDate)
    """)
    fun findByUserIdFiltered(
        @Param("userId") userId: UUID,
        @Param("type") type: DocumentType?,
        @Param("startDate") startDate: LocalDate?,
        @Param("endDate") endDate: LocalDate?,
        pageable: Pageable,
    ): Page<MedicalDocumentEntity>

    fun findByUploadedFileId(uploadedFileId: UUID): List<MedicalDocumentEntity>
}
