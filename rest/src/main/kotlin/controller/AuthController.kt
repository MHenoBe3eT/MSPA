package controller

import auth.AuthenticateUserUseCase
import auth.RegisterUserUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@RestController
@RequestMapping("/auth")
class AuthController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val authenticateUserUseCase: AuthenticateUserUseCase,
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> {
        val user = registerUserUseCase(request.email, request.password, request.name)
        return ResponseEntity(
            RegisterResponse(
                id = user.id.value.toString(),
                email = user.email,
                name = user.name,
            ),
            HttpStatus.CREATED,
        )
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val token = authenticateUserUseCase(request.email, request.password)
        return ResponseEntity.ok(LoginResponse(token = token))
    }
}

data class RegisterRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    val password: String,
    @field:NotBlank
    val name: String,
)

data class RegisterResponse(
    val id: String,
    val email: String,
    val name: String,
)

data class LoginRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    val password: String,
)

data class LoginResponse(
    val token: String,
)
