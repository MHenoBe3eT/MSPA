package ai

import com.fasterxml.jackson.databind.ObjectMapper
import controller.LoginRequest
import controller.RegisterRequest
import mspa.MSPAApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(classes = [MSPAApplication::class])
@AutoConfigureMockMvc
@Testcontainers
class AiProcessingIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("mspa_test")
            .withUsername("test")
            .withPassword("test")

        @Container
        @JvmStatic
        val minio = GenericContainer("minio/minio:latest")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server /data")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("minio.endpoint") { "http://${minio.host}:${minio.getMappedPort(9000)}" }
            registry.add("minio.access-key") { "minioadmin" }
            registry.add("minio.secret-key") { "minioadmin" }
            registry.add("minio.bucket") { "mspa-test" }
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private var authToken: String = ""

    @BeforeEach
    fun authenticate() {
        val email = "ai-proc-${System.nanoTime()}@example.com"
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "password123", "Test User")))
        ).andExpect(status().isCreated)

        val loginResult = mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequest(email, "password123")))
        ).andExpect(status().isOk).andReturn()

        authToken = objectMapper.readTree(loginResult.response.contentAsString).get("token").asText()
    }

    private fun uploadPdf(suffix: String = System.nanoTime().toString()): String {
        val content = "%PDF-1.4 test-$suffix".toByteArray()
        val file = MockMultipartFile("file", "test-$suffix.pdf", "application/pdf", content)

        val result = mockMvc.perform(
            multipart("/uploaded-files")
                .file(file)
                .header("Authorization", "Bearer $authToken")
        ).andExpect(status().isAccepted).andReturn()

        return objectMapper.readTree(result.response.contentAsString).get("id").asText()
    }

    private fun waitForProcessing(fileId: String, timeoutMs: Long = 15_000): String {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val result = mockMvc.perform(
                get("/uploaded-files/$fileId")
                    .header("Authorization", "Bearer $authToken")
            ).andReturn()
            val fileStatus = objectMapper.readTree(result.response.contentAsString).get("status").asText()
            if (fileStatus == "PROCESSED" || fileStatus == "ERROR") return fileStatus
            Thread.sleep(300)
        }
        return "TIMEOUT"
    }

    // Тест-кейс 51: статус после загрузки — UPLOADED
    @Test
    fun `file status should be UPLOADED immediately after upload`() {
        val content = "%PDF-1.4 fresh".toByteArray()
        val file = MockMultipartFile("file", "immediate.pdf", "application/pdf", content)

        mockMvc.perform(
            multipart("/uploaded-files")
                .file(file)
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.status").value("UPLOADED"))
            .andExpect(jsonPath("$.id").isNotEmpty)
    }

    // Тест-кейс 52: статус переходит из UPLOADED в PROCESSED после обработки
    @Test
    fun `file status should transition to PROCESSED after async AI processing`() {
        val fileId = uploadPdf()

        val finalStatus = waitForProcessing(fileId)

        assertEquals("PROCESSED", finalStatus, "File should reach PROCESSED status after AI processing")
    }

    // Тест-кейс 53: после обработки создаются MedicalDocument записи
    @Test
    fun `should create at least one MedicalDocument after AI processing`() {
        val fileId = uploadPdf()
        waitForProcessing(fileId)

        val documentsResult = mockMvc.perform(
            get("/documents")
                .header("Authorization", "Bearer $authToken")
        ).andExpect(status().isOk).andReturn()

        val documents = objectMapper.readTree(documentsResult.response.contentAsString)
        assertTrue(documents.isArray && documents.size() > 0, "At least one MedicalDocument should be created after processing")
    }

    // Тест-кейс 54: каждый документ имеет структурированные данные (labData) и интерпретацию с disclaimer
    @Test
    fun `processed document should have structured lab data and interpretation with disclaimer`() {
        val fileId = uploadPdf()
        waitForProcessing(fileId)

        val listResult = mockMvc.perform(
            get("/documents")
                .header("Authorization", "Bearer $authToken")
        ).andReturn()

        val documents = objectMapper.readTree(listResult.response.contentAsString)
        assertTrue(documents.size() > 0, "Expected at least one document")

        val documentId = documents[0].get("id").asText()

        mockMvc.perform(
            get("/documents/$documentId")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(documentId))
            .andExpect(jsonPath("$.documentType").value("LAB_ANALYSIS"))
            .andExpect(jsonPath("$.interpretation").isNotEmpty)
            .andExpect(jsonPath("$.interpretation.interpretationText").isNotEmpty)
            .andExpect(jsonPath("$.interpretation.disclaimer").isNotEmpty)
            .andExpect(jsonPath("$.labData").isNotEmpty)
            .andExpect(jsonPath("$.labData.indicators").isArray)
            .andExpect(jsonPath("$.labData.indicators[0].name").isNotEmpty)
    }

    // Тест-кейс 55: GET /uploaded-files/{id} после обработки показывает статус PROCESSED
    @Test
    fun `GET uploaded file should show PROCESSED status after AI processing completes`() {
        val fileId = uploadPdf()
        waitForProcessing(fileId)

        mockMvc.perform(
            get("/uploaded-files/$fileId")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(fileId))
            .andExpect(jsonPath("$.status").value("PROCESSED"))
    }

    // Тест-кейс 56: документы у разных пользователей изолированы
    @Test
    fun `documents from one user should not be visible to another user`() {
        // Загружаем файл от имени первого пользователя (authToken)
        val fileId = uploadPdf()
        waitForProcessing(fileId)

        // Регистрируем второго пользователя
        val email2 = "ai-proc2-${System.nanoTime()}@example.com"
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email2, "password123", "User Two")))
        ).andExpect(status().isCreated)

        val loginResult2 = mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequest(email2, "password123")))
        ).andExpect(status().isOk).andReturn()

        val token2 = objectMapper.readTree(loginResult2.response.contentAsString).get("token").asText()

        // Второй пользователь не должен видеть документы первого
        val user2DocsResult = mockMvc.perform(
            get("/documents")
                .header("Authorization", "Bearer $token2")
        ).andExpect(status().isOk).andReturn()

        val user2Docs = objectMapper.readTree(user2DocsResult.response.contentAsString)
        assertEquals(0, user2Docs.size(), "Second user should have no documents")
    }
}
