package file

import com.fasterxml.jackson.databind.ObjectMapper
import controller.LoginRequest
import controller.RegisterRequest
import mspa.MSPAApplication
import org.junit.jupiter.api.Assertions.assertArrayEquals
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
class UploadIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
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
        val email = "upload-test-${System.nanoTime()}@example.com"
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

    // Тест-кейс 31: 202 для валидного PDF
    @Test
    fun `should return 202 Accepted for valid PDF upload`() {
        val pdfContent = "%PDF-1.4 test content".toByteArray()
        val file = MockMultipartFile("file", "test.pdf", "application/pdf", pdfContent)

        mockMvc.perform(
            multipart("/uploaded-files")
                .file(file)
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andExpect(jsonPath("$.status").value("UPLOADED"))
            .andExpect(jsonPath("$.originalFileName").value("test.pdf"))
    }

    // Тест-кейс 32: 400 для файла > 10MB
    @Test
    fun `should return 400 for file exceeding 10MB`() {
        val oversizedContent = ByteArray(10 * 1024 * 1024 + 1)
        val file = MockMultipartFile("file", "big.pdf", "application/pdf", oversizedContent)

        mockMvc.perform(
            multipart("/uploaded-files")
                .file(file)
                .header("Authorization", "Bearer $authToken")
        ).andExpect(status().isBadRequest)
    }

    // Тест-кейс 33: 400 для неподдерживаемого формата
    @Test
    fun `should return 400 for unsupported file format`() {
        val content = "some document content".toByteArray()
        val file = MockMultipartFile("file", "doc.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", content)

        mockMvc.perform(
            multipart("/uploaded-files")
                .file(file)
                .header("Authorization", "Bearer $authToken")
        ).andExpect(status().isBadRequest)
    }

    // Тест-кейс 34: дубликат возвращает существующий файл
    @Test
    fun `should return existing file for duplicate upload`() {
        val content = "unique content for dedup test".toByteArray()
        val file = MockMultipartFile("file", "original.pdf", "application/pdf", content)

        val firstResult = mockMvc.perform(
            multipart("/uploaded-files")
                .file(file)
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isAccepted)
            .andReturn()

        val firstId = objectMapper.readTree(firstResult.response.contentAsString).get("id").asText()

        val duplicateFile = MockMultipartFile("file", "duplicate.pdf", "application/pdf", content)
        val secondResult = mockMvc.perform(
            multipart("/uploaded-files")
                .file(duplicateFile)
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isAccepted)
            .andReturn()

        val secondId = objectMapper.readTree(secondResult.response.contentAsString).get("id").asText()
        assert(firstId == secondId) { "Duplicate upload should return the same file ID" }
    }

    // Тест-кейс 35: GET /uploaded-files/{id} возвращает статус
    @Test
    fun `should return file status via GET by id`() {
        val content = "status check content".toByteArray()
        val file = MockMultipartFile("file", "status.pdf", "application/pdf", content)

        val uploadResult = mockMvc.perform(
            multipart("/uploaded-files")
                .file(file)
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isAccepted)
            .andReturn()

        val fileId = objectMapper.readTree(uploadResult.response.contentAsString).get("id").asText()

        mockMvc.perform(
            get("/uploaded-files/$fileId")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(fileId))
            .andExpect(jsonPath("$.status").isNotEmpty)
    }

    // Тест-кейс 36: GET /uploaded-files/{id}/download — байт в байт совпадает с оригиналом
    @Test
    fun `should download uploaded file byte-for-byte identical to original`() {
        val originalContent = "exact byte content for download test".toByteArray()
        val file = MockMultipartFile("file", "download.pdf", "application/pdf", originalContent)

        val uploadResult = mockMvc.perform(
            multipart("/uploaded-files")
                .file(file)
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isAccepted)
            .andReturn()

        val fileId = objectMapper.readTree(uploadResult.response.contentAsString).get("id").asText()

        val downloadResult = mockMvc.perform(
            get("/uploaded-files/$fileId/download")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andReturn()

        assertArrayEquals(originalContent, downloadResult.response.contentAsByteArray)
    }

    // Тест-кейс 37: 401 без токена
    @Test
    fun `should return 401 without auth token`() {
        val file = MockMultipartFile("file", "test.pdf", "application/pdf", ByteArray(100))

        mockMvc.perform(
            multipart("/uploaded-files").file(file)
        ).andExpect(status().isUnauthorized)
    }

    // Тест-кейс 38: 404 для несуществующего файла
    @Test
    fun `should return 404 for non-existent file id`() {
        mockMvc.perform(
            get("/uploaded-files/00000000-0000-0000-0000-000000000000")
                .header("Authorization", "Bearer $authToken")
        ).andExpect(status().isNotFound)
    }
}
