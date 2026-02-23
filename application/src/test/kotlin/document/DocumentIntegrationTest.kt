package document

import com.fasterxml.jackson.databind.ObjectMapper
import controller.LoginRequest
import controller.RegisterRequest
import mspa.MSPAApplication
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
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
class DocumentIntegrationTest {

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
        val email = "doc-test-${System.nanoTime()}@example.com"
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

    private fun uploadPdfAndWaitForProcessing(): String {
        val pdfContent = "%PDF-1.4 test-${System.nanoTime()}".toByteArray()
        val file = MockMultipartFile("file", "test.pdf", "application/pdf", pdfContent)

        val uploadResult = mockMvc.perform(
            multipart("/uploaded-files")
                .file(file)
                .header("Authorization", "Bearer $authToken")
        ).andExpect(status().isAccepted).andReturn()

        val fileId = objectMapper.readTree(uploadResult.response.contentAsString).get("id").asText()

        val timeout = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < timeout) {
            val statusResult = mockMvc.perform(
                get("/uploaded-files/$fileId")
                    .header("Authorization", "Bearer $authToken")
            ).andReturn()
            val fileStatus = objectMapper.readTree(statusResult.response.contentAsString).get("status").asText()
            if (fileStatus == "PROCESSED" || fileStatus == "ERROR") break
            Thread.sleep(300)
        }

        return fileId
    }

    // Тест-кейс 41: GET /documents — список документов пользователя
    @Test
    fun `should return list of documents for authenticated user`() {
        uploadPdfAndWaitForProcessing()

        mockMvc.perform(
            get("/documents")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].id").isNotEmpty)
            .andExpect(jsonPath("$[0].documentType").value("LAB_ANALYSIS"))
    }

    // Тест-кейс 42: GET /documents?type=LAB_ANALYSIS — фильтр по типу
    @Test
    fun `should filter documents by type`() {
        uploadPdfAndWaitForProcessing()

        mockMvc.perform(
            get("/documents?type=LAB_ANALYSIS")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].documentType").value("LAB_ANALYSIS"))

        mockMvc.perform(
            get("/documents?type=VISIT_PROTOCOL")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    // Тест-кейс 43: GET /documents/{id} — детали документа с интерпретацией
    @Test
    fun `should return document details with interpretation and structured data`() {
        uploadPdfAndWaitForProcessing()

        val listResult = mockMvc.perform(
            get("/documents")
                .header("Authorization", "Bearer $authToken")
        ).andReturn()

        val documents = objectMapper.readTree(listResult.response.contentAsString)
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
    }

    // Тест-кейс 44: PATCH /documents/{id} — смена типа документа
    @Test
    fun `should update document type`() {
        uploadPdfAndWaitForProcessing()

        val listResult = mockMvc.perform(
            get("/documents")
                .header("Authorization", "Bearer $authToken")
        ).andReturn()

        val documentId = objectMapper.readTree(listResult.response.contentAsString)[0].get("id").asText()

        mockMvc.perform(
            patch("/documents/$documentId")
                .header("Authorization", "Bearer $authToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"documentType": "VISIT_PROTOCOL"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(documentId))
            .andExpect(jsonPath("$.documentType").value("VISIT_PROTOCOL"))

        val updatedResult = mockMvc.perform(
            get("/documents/$documentId")
                .header("Authorization", "Bearer $authToken")
        ).andReturn()

        val updatedType = objectMapper.readTree(updatedResult.response.contentAsString).get("documentType").asText()
        assertEquals("VISIT_PROTOCOL", updatedType)
    }

    // Тест-кейс 45: DELETE /documents/{id} — удаление документа
    @Test
    fun `should delete document and return 404 on subsequent get`() {
        uploadPdfAndWaitForProcessing()

        val listResult = mockMvc.perform(
            get("/documents")
                .header("Authorization", "Bearer $authToken")
        ).andReturn()

        val documentId = objectMapper.readTree(listResult.response.contentAsString)[0].get("id").asText()

        mockMvc.perform(
            delete("/documents/$documentId")
                .header("Authorization", "Bearer $authToken")
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            get("/documents/$documentId")
                .header("Authorization", "Bearer $authToken")
        ).andExpect(status().isNotFound)
    }

    // Тест-кейс 46: 401 без токена
    @Test
    fun `should return 401 for unauthenticated requests`() {
        mockMvc.perform(get("/documents"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/documents/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isUnauthorized)
    }

    // Тест-кейс 47: 404 для несуществующего документа
    @Test
    fun `should return 404 for non-existent document`() {
        mockMvc.perform(
            get("/documents/00000000-0000-0000-0000-000000000000")
                .header("Authorization", "Bearer $authToken")
        ).andExpect(status().isNotFound)
    }
}
