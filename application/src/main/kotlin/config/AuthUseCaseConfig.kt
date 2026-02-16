package config

import auth.AuthenticateUserUseCase
import auth.PasswordEncoder
import auth.RegisterUserUseCase
import auth.TokenProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import user.CreateUser
import user.GetUserByEmail

@Configuration
class AuthUseCaseConfig {

    @Bean
    fun registerUserUseCase(
        getUserByEmail: GetUserByEmail,
        createUser: CreateUser,
        passwordEncoder: PasswordEncoder,
    ): RegisterUserUseCase = RegisterUserUseCase(getUserByEmail, createUser, passwordEncoder)

    @Bean
    fun authenticateUserUseCase(
        getUserByEmail: GetUserByEmail,
        passwordEncoder: PasswordEncoder,
        tokenProvider: TokenProvider,
    ): AuthenticateUserUseCase = AuthenticateUserUseCase(getUserByEmail, passwordEncoder, tokenProvider)
}
