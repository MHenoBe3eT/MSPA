package auth

import com.fasterxml.jackson.databind.ObjectMapper
import controller.LoginRequest
import controller.RegisterRequest
import mspa.MSPAApplication
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(classes = [MSPAApplication::class])
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
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
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun registerUser(
        email: String = "test@example.com",
        password: String = "password123",
        name: String = "Test User",
    ) = mockMvc.perform(
        post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(RegisterRequest(email, password, name)))
    )

    private fun loginUser(
        email: String = "test@example.com",
        password: String = "password123",
    ) = mockMvc.perform(
        post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(LoginRequest(email, password)))
    )

    // === POST /auth/register ===

    // Тест-кейс 11: 201 — валидный запрос
    @Test
    fun `register should return 201 with id, email and name without password`() {
        registerUser(email = "new-user-11@example.com")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andExpect(jsonPath("$.email").value("new-user-11@example.com"))
            .andExpect(jsonPath("$.name").value("Test User"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
    }

    // Тест-кейс 12: 409 — повторная регистрация с тем же email
    @Test
    fun `register should return 409 for duplicate email`() {
        registerUser(email = "duplicate-12@example.com")
            .andExpect(status().isCreated)

        registerUser(email = "duplicate-12@example.com")
            .andExpect(status().isConflict)
    }

    // Тест-кейс 13: 400 — невалидный email / пустой пароль / пустое имя
    @Test
    fun `register should return 400 for invalid email`() {
        registerUser(email = "not-an-email")
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `register should return 400 for blank password`() {
        registerUser(password = "")
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `register should return 400 for blank name`() {
        registerUser(name = "")
            .andExpect(status().isBadRequest)
    }

    // === POST /auth/login ===

    // Тест-кейс 14: 200 — верные credentials
    @Test
    fun `login should return 200 with JWT token for valid credentials`() {
        registerUser(email = "login-14@example.com", password = "correctpass")
            .andExpect(status().isCreated)

        loginUser(email = "login-14@example.com", password = "correctpass")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty)
    }

    // Тест-кейс 15: 401 — неверный пароль
    @Test
    fun `login should return 401 for wrong password`() {
        registerUser(email = "login-15@example.com", password = "correctpass")
            .andExpect(status().isCreated)

        loginUser(email = "login-15@example.com", password = "wrongpass")
            .andExpect(status().isUnauthorized)
    }

    // Тест-кейс 16: 401 — несуществующий email
    @Test
    fun `login should return 401 for nonexistent email`() {
        loginUser(email = "nonexistent@example.com", password = "password")
            .andExpect(status().isUnauthorized)
    }

    // === Защищённые эндпоинты ===

    // Тест-кейс 17: 401 — запрос без токена
    @Test
    fun `protected endpoint should return 401 without token`() {
        mockMvc.perform(get("/user"))
            .andExpect(status().isUnauthorized)
    }

    // Тест-кейс 18: 401 — невалидный/просроченный токен
    @Test
    fun `protected endpoint should return 401 with invalid token`() {
        mockMvc.perform(
            get("/user")
                .header("Authorization", "Bearer invalid.token.here")
        ).andExpect(status().isUnauthorized)
    }

    // Тест-кейс 19: 200 — запрос с валидным JWT
    @Test
    fun `protected endpoint should allow access with valid JWT`() {
        registerUser(email = "access-19@example.com", password = "password123")
            .andExpect(status().isCreated)

        val loginResult = loginUser(email = "access-19@example.com", password = "password123")
            .andExpect(status().isOk)
            .andReturn()

        val token = objectMapper.readTree(loginResult.response.contentAsString).get("token").asText()

        // Any authenticated endpoint should not return 401/403
        // Using /user as a protected endpoint that exists
        val result = mockMvc.perform(
            get("/user")
                .header("Authorization", "Bearer $token")
        ).andReturn()

        val statusCode = result.response.status
        // Should not be 401 or 403 — authentication passed
        assert(statusCode != 401 && statusCode != 403) {
            "Expected authenticated access but got status $statusCode"
        }
    }

    // === Полный flow (e2e) ===

    // Тест-кейс 20: Register → Login → Access protected endpoint
    @Test
    fun `full flow - register then login then access protected endpoint`() {
        // Step 1: Register
        registerUser(email = "flow-20@example.com", password = "securepass")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value("flow-20@example.com"))

        // Step 2: Login
        val loginResult = loginUser(email = "flow-20@example.com", password = "securepass")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andReturn()

        val token = objectMapper.readTree(loginResult.response.contentAsString).get("token").asText()

        // Step 3: Access protected endpoint with token
        val result = mockMvc.perform(
            get("/user")
                .header("Authorization", "Bearer $token")
        ).andReturn()

        assert(result.response.status != 401 && result.response.status != 403) {
            "Full flow failed: expected authenticated access but got status ${result.response.status}"
        }
    }
}
