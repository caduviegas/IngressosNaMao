package com.innaval.ingresosnamao.feature.auth.domain

data class AuthUser(val id: String, val email: String, val name: String = "")
data class UserProfile(val name: String, val email: String, val address: String)
data class Registration(val name: String, val email: String, val address: String, val password: String)

enum class AuthError {
    INVALID_CREDENTIALS, EMAIL_IN_USE, WEAK_PASSWORD, USER_DISABLED, TOO_MANY_REQUESTS,
    NETWORK, GOOGLE_CANCELLED, GOOGLE_FAILED, CONFIGURATION, PROFILE_READ, PROFILE_WRITE, UNKNOWN
}

sealed interface AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>
    data class Failure(val error: AuthError) : AuthResult<Nothing>
}

interface AuthRepository {
    suspend fun currentUser(): AuthResult<AuthUser?>
    suspend fun login(email: String, password: String): AuthResult<AuthUser>
    suspend fun loginWithGoogle(idToken: String): AuthResult<AuthUser>
    suspend fun register(email: String, password: String): AuthResult<AuthUser>
    suspend fun getProfile(user: AuthUser): AuthResult<UserProfile?>
    suspend fun saveProfile(user: AuthUser, profile: UserProfile): AuthResult<Unit>
    fun signOut()
}

enum class Field { NAME, EMAIL, ADDRESS, PASSWORD }

object AuthValidation {
    private val emailPattern = Regex("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$")

    fun login(email: String, password: String): Set<Field> = buildSet {
        if (!validEmail(email.trim())) add(Field.EMAIL)
        if (password.isEmpty()) add(Field.PASSWORD)
    }

    fun registration(data: Registration): Set<Field> = buildSet {
        addAll(profile(data.name, data.email, data.address))
        if (data.password.length !in 6..128) add(Field.PASSWORD)
    }

    fun profile(name: String, email: String, address: String): Set<Field> = buildSet {
        if (name.trim().length !in 1..100) add(Field.NAME)
        if (!validEmail(email.trim())) add(Field.EMAIL)
        if (address.trim().length !in 1..300) add(Field.ADDRESS)
    }

    private fun validEmail(email: String) = email.length <= 254 && emailPattern.matches(email)
}

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) = repository.login(email.trim(), password)
}

class GoogleLoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(idToken: String) = repository.loginWithGoogle(idToken)
}

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(data: Registration) = repository.register(data.email.trim(), data.password)
}

class GetCurrentUserUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.currentUser()
}

class GetProfileUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(user: AuthUser) = repository.getProfile(user)
}

class SaveProfileUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(user: AuthUser, profile: UserProfile) = repository.saveProfile(
        user, profile.copy(name = profile.name.trim(), email = profile.email.trim(), address = profile.address.trim())
    )
}

class SignOutUseCase(private val repository: AuthRepository) {
    operator fun invoke() = repository.signOut()
}
