package schema

import mspa.MSPAApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(classes = [MSPAApplication::class])
@Testcontainers
class LiquibaseMigrationIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("mspa_test")
            .withUsername("test")
            .withPassword("test")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private fun tableExists(tableName: String): Boolean {
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
            Int::class.java,
            tableName
        )
        return count != null && count > 0
    }

    private fun columnExists(tableName: String, columnName: String): Boolean {
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
            Int::class.java,
            tableName,
            columnName
        )
        return count != null && count > 0
    }

    private fun indexExists(indexName: String): Boolean {
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = ?",
            Int::class.java,
            indexName
        )
        return count != null && count > 0
    }

    private fun getColumnDataType(tableName: String, columnName: String): String? =
        jdbcTemplate.queryForObject(
            "SELECT data_type FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
            String::class.java,
            tableName,
            columnName
        )

    private fun uniqueConstraintExists(tableName: String, columnName: String): Boolean {
        val count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.table_constraints tc
            JOIN information_schema.constraint_column_usage ccu
                ON tc.constraint_name = ccu.constraint_name AND tc.table_schema = ccu.table_schema
            WHERE tc.table_schema = 'public'
              AND tc.table_name = ?
              AND tc.constraint_type = 'UNIQUE'
              AND ccu.column_name = ?
            """.trimIndent(),
            Int::class.java,
            tableName,
            columnName
        )
        return count != null && count > 0
    }

    // === Таблицы ===

    @Test
    fun `all required tables should exist after liquibase migration`() {
        val requiredTables = listOf(
            "users",
            "documents",
            "uploaded_files",
            "ai_processing_results",
            "medical_documents",
            "lab_analysis_data",
            "visit_protocol_data",
            "instrumental_study_data",
            "ai_interpretations",
        )
        requiredTables.forEach { tableName ->
            assertTrue(tableExists(tableName), "Table '$tableName' should exist after migration")
        }
    }

    // === Колонки: users ===

    @Test
    fun `users table should have all required columns`() {
        listOf("id", "email", "password_hash", "name", "created_at").forEach { column ->
            assertTrue(columnExists("users", column), "Column 'users.$column' should exist")
        }
    }

    // === Колонки: uploaded_files ===

    @Test
    fun `uploaded_files table should have all required columns`() {
        listOf(
            "id", "user_id", "original_file_name", "content_type",
            "size_bytes", "checksum", "storage_key", "status", "uploaded_at"
        ).forEach { column ->
            assertTrue(columnExists("uploaded_files", column), "Column 'uploaded_files.$column' should exist")
        }
    }

    // === Колонки: medical_documents ===

    @Test
    fun `medical_documents table should have all required columns`() {
        listOf(
            "id", "uploaded_file_id", "user_id", "document_type",
            "status", "title", "document_date", "created_at"
        ).forEach { column ->
            assertTrue(columnExists("medical_documents", column), "Column 'medical_documents.$column' should exist")
        }
    }

    // === Колонки: ai_processing_results ===

    @Test
    fun `ai_processing_results table should have all required columns`() {
        listOf("id", "uploaded_file_id", "raw_extracted_text", "model_version", "processed_at").forEach { column ->
            assertTrue(columnExists("ai_processing_results", column), "Column 'ai_processing_results.$column' should exist")
        }
    }

    // === Колонки: ai_interpretations ===

    @Test
    fun `ai_interpretations table should have all required columns`() {
        listOf("id", "medical_document_id", "interpretation_text", "risk_markers", "disclaimer", "model_version").forEach { column ->
            assertTrue(columnExists("ai_interpretations", column), "Column 'ai_interpretations.$column' should exist")
        }
    }

    // === Колонки: visit_protocol_data ===

    @Test
    fun `visit_protocol_data table should have all required columns`() {
        listOf("id", "medical_document_id", "narrative_text", "complaints", "anamnesis", "diagnosis", "treatment_plan").forEach { column ->
            assertTrue(columnExists("visit_protocol_data", column), "Column 'visit_protocol_data.$column' should exist")
        }
    }

    // === Колонки: instrumental_study_data ===

    @Test
    fun `instrumental_study_data table should have all required columns`() {
        listOf("id", "medical_document_id", "description", "findings").forEach { column ->
            assertTrue(columnExists("instrumental_study_data", column), "Column 'instrumental_study_data.$column' should exist")
        }
    }

    // === JSONB типы ===

    @Test
    fun `lab_analysis_data indicators column should be of type jsonb`() {
        assertTrue(columnExists("lab_analysis_data", "indicators"), "Column 'lab_analysis_data.indicators' should exist")
        assertEquals("jsonb", getColumnDataType("lab_analysis_data", "indicators"), "indicators column should be jsonb")
    }

    @Test
    fun `ai_interpretations risk_markers column should be of type jsonb`() {
        assertTrue(columnExists("ai_interpretations", "risk_markers"), "Column 'ai_interpretations.risk_markers' should exist")
        assertEquals("jsonb", getColumnDataType("ai_interpretations", "risk_markers"), "risk_markers column should be jsonb")
    }

    @Test
    fun `instrumental_study_data findings column should be of type jsonb`() {
        assertTrue(columnExists("instrumental_study_data", "findings"), "Column 'instrumental_study_data.findings' should exist")
        assertEquals("jsonb", getColumnDataType("instrumental_study_data", "findings"), "findings column should be jsonb")
    }

    // === Индексы ===

    @Test
    fun `all required indexes should exist`() {
        val requiredIndexes = listOf(
            "idx_uploaded_files_user_id",
            "idx_uploaded_files_checksum_user_id",
            "idx_medical_documents_user_id",
            "idx_medical_documents_uploaded_file_id",
        )
        requiredIndexes.forEach { indexName ->
            assertTrue(indexExists(indexName), "Index '$indexName' should exist")
        }
    }

    // === Ограничения ===

    @Test
    fun `users email column should have unique constraint`() {
        assertTrue(uniqueConstraintExists("users", "email"), "users.email should have a UNIQUE constraint")
    }
}
