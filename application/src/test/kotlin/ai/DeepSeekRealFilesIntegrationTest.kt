package ai

import com.fasterxml.jackson.databind.ObjectMapper
import controller.LoginRequest
import controller.RegisterRequest
import mspa.MSPAApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File

/**
 * Интеграционные тесты с реальными PDF-файлами через DeepSeek API.
 *
 * Запуск (ключ уже в application.properties):
 *   ./gradlew :application:test --tests "ai.DeepSeekRealFilesIntegrationTest"
 *
 * Или с ключом через env-переменную (тогда application.properties не трогать):
 *   DEEPSEEK_API_KEY=sk-xxx ./gradlew :application:test --tests "ai.DeepSeekRealFilesIntegrationTest"
 */
@SpringBootTest(classes = [MSPAApplication::class])
@AutoConfigureMockMvc
@Testcontainers
class DeepSeekRealFilesIntegrationTest {

    companion object {
        private val DEEPSEEK_API_KEY: String? = System.getenv("DEEPSEEK_API_KEY")

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("mspa_deepseek_test")
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
            registry.add("minio.bucket") { "mspa-deepseek-test" }

            if (DEEPSEEK_API_KEY != null) {
                // Переключаемся на DeepSeek только когда есть ключ
                registry.add("mspa.ai.provider") { "deepseek" }
                registry.add("spring.ai.model.chat") { "deepseek" }
                registry.add("spring.ai.deepseek.api-key") { DEEPSEEK_API_KEY }
            }
            // Иначе — наследуем application.properties (stub-режим), тесты будут пропущены через assumeTrue
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private var authToken: String = ""

    @BeforeEach
    fun setup() {
        assumeTrue(DEEPSEEK_API_KEY != null, "DEEPSEEK_API_KEY не задан — тест пропускается")
        val email = "deepseek-${System.nanoTime()}@example.com"
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

    private fun uploadPdf(file: File): String {
        val mockFile = MockMultipartFile("file", file.name, "application/pdf", file.readBytes())
        val result = mockMvc.perform(
            multipart("/uploaded-files")
                .file(mockFile)
                .header("Authorization", "Bearer $authToken")
        ).andExpect(status().isAccepted).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("id").asText()
    }

    /** Ждём перехода в PROCESSED или ERROR, таймаут 2 минуты (DeepSeek может обрабатывать долго). */
    private fun waitForProcessing(fileId: String, timeoutMs: Long = 120_000): String {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val result = mockMvc.perform(
                get("/uploaded-files/$fileId").header("Authorization", "Bearer $authToken")
            ).andReturn()
            val status = objectMapper.readTree(result.response.contentAsString).get("status").asText()
            if (status == "PROCESSED" || status == "ERROR") return status
            Thread.sleep(500)
        }
        return "TIMEOUT"
    }

    @Test
    fun `DeepSeek should process LabRes PDF and extract lab indicators`() {
        val pdf = File("../test-cases/LabRes-20251118112251642859.pdf")
        assumeTrue(pdf.exists(), "Файл не найден: ${pdf.absolutePath}")

        val fileId = uploadPdf(pdf)
        val finalStatus = waitForProcessing(fileId)
        assertEquals("PROCESSED", finalStatus, "DeepSeek должен успешно обработать PDF с анализами")

        val docs = getDocuments()
        assertTrue(docs.isArray && docs.size() > 0, "Должен создаться хотя бы один MedicalDocument")

        val docId = docs[0].get("id").asText()
        val doc = getDocument(docId)

        val interpretation = doc.get("interpretation")
        assertTrue(interpretation != null && !interpretation.isNull, "Должна быть интерпретация")
        assertTrue(interpretation.get("interpretationText").asText().isNotBlank(), "Текст интерпретации не должен быть пустым")
        assertTrue(interpretation.get("disclaimer").asText().isNotBlank(), "Disclaimer не должен быть пустым")
    }

    @Test
    fun `DeepSeek should process OAK biochemistry PDF`() {
        val pdf = File("../test-cases/ОАК + биохимия Власов 25062025.pdf")
        assumeTrue(pdf.exists(), "Файл не найден: ${pdf.absolutePath}")

        val fileId = uploadPdf(pdf)
        val finalStatus = waitForProcessing(fileId)
        assertEquals("PROCESSED", finalStatus, "DeepSeek должен успешно обработать PDF ОАК+биохимия")

        val docs = getDocuments()
        assertTrue(docs.isArray && docs.size() > 0, "Должен создаться хотя бы один MedicalDocument")
    }

    private fun getDocuments() = objectMapper.readTree(
        mockMvc.perform(
            get("/documents").header("Authorization", "Bearer $authToken")
        ).andExpect(status().isOk).andReturn().response.contentAsString
    )

    private fun getDocument(id: String) = objectMapper.readTree(
        mockMvc.perform(
            get("/documents/$id").header("Authorization", "Bearer $authToken")
        ).andExpect(status().isOk).andReturn().response.contentAsString
    )
}
