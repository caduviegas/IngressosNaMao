package com.innaval.ingresosnamao.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innaval.ingresosnamao.feature.auth.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthStage { LOGIN, REGISTER, CHECK_PROFILE, COMPLETE_PROFILE, AUTHENTICATED }

data class AuthUiState(
    val stage: AuthStage = AuthStage.LOGIN,
    val loading: Boolean = true,
    val name: String = "",
    val email: String = "",
    val address: String = "",
    val password: String = "",
    val user: AuthUser? = null,
    val invalidFields: Set<Field> = emptySet(),
    val error: AuthError? = null,
)

class AuthViewModel(
    private val login: LoginUseCase,
    private val googleLogin: GoogleLoginUseCase,
    private val register: RegisterUseCase,
    private val currentUser: GetCurrentUserUseCase,
    private val getProfile: GetProfileUseCase,
    private val saveProfile: SaveProfileUseCase,
    private val signOut: SignOutUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = currentUser()) {
                is AuthResult.Success -> {
                    if (result.value == null) mutableState.update { it.copy(loading = false) }
                    else checkProfile(result.value)
                }
                is AuthResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    fun changeName(value: String) = edit { copy(name = value) }
    fun changeEmail(value: String) = edit { copy(email = value) }
    fun changeAddress(value: String) = edit { copy(address = value) }
    fun changePassword(value: String) = edit { copy(password = value) }

    private fun edit(change: AuthUiState.() -> AuthUiState) {
        mutableState.update { if (it.loading) it else it.change().copy(error = null, invalidFields = emptySet()) }
    }

    fun showRegister() {
        if (!state.value.loading) mutableState.update {
            it.copy(stage = AuthStage.REGISTER, error = null, invalidFields = emptySet(), password = "")
        }
    }

    fun showLogin() {
        if (!state.value.loading) mutableState.update {
            it.copy(stage = AuthStage.LOGIN, error = null, invalidFields = emptySet(), password = "")
        }
    }

    fun submitLogin() {
        val input = state.value
        if (input.loading) return
        val invalid = AuthValidation.login(input.email, input.password)
        if (invalid.isNotEmpty()) {
            mutableState.update { it.copy(invalidFields = invalid, error = null) }
            return
        }
        start {
            when (val result = login(input.email, input.password)) {
                is AuthResult.Success -> checkProfile(result.value)
                is AuthResult.Failure -> fail(result.error)
            }
        }
    }

    fun submitGoogle(obtainToken: suspend () -> AuthResult<String>) {
        if (state.value.loading) return
        start {
            when (val token = obtainToken()) {
                is AuthResult.Failure -> fail(token.error)
                is AuthResult.Success -> when (val result = googleLogin(token.value)) {
                    is AuthResult.Success -> checkProfile(result.value)
                    is AuthResult.Failure -> fail(result.error)
                }
            }
        }
    }

    fun submitRegistration() {
        val input = state.value
        if (input.loading) return
        val data = Registration(input.name, input.email, input.address, input.password)
        val invalid = AuthValidation.registration(data)
        if (invalid.isNotEmpty()) {
            mutableState.update { it.copy(invalidFields = invalid, error = null) }
            return
        }
        start {
            when (val result = register(data)) {
                is AuthResult.Failure -> fail(result.error)
                is AuthResult.Success -> {
                    val user = result.value
                    mutableState.update {
                        it.copy(stage = AuthStage.COMPLETE_PROFILE, user = user, email = user.email, password = "")
                    }
                    persistProfile(user, UserProfile(data.name, user.email, data.address))
                }
            }
        }
    }

    fun submitProfile() {
        val input = state.value
        val user = input.user ?: return
        if (input.loading) return
        val invalid = AuthValidation.profile(input.name, user.email, input.address)
        if (invalid.isNotEmpty()) {
            mutableState.update { it.copy(invalidFields = invalid, error = null) }
            return
        }
        start { persistProfile(user, UserProfile(input.name, user.email, input.address)) }
    }

    fun retryProfile() {
        val user = state.value.user ?: return
        if (!state.value.loading) start { checkProfile(user) }
    }

    fun leaveAccount() {
        if (state.value.loading) return
        val user = state.value.user ?: return
        start {
            when (val result = signOut(user.id)) {
                is AuthResult.Success -> mutableState.value = AuthUiState(loading = false)
                is AuthResult.Failure -> fail(result.error)
            }
        }
    }

    private fun start(block: suspend () -> Unit) {
        mutableState.update { it.copy(loading = true, error = null, invalidFields = emptySet()) }
        viewModelScope.launch { block() }
    }

    private suspend fun checkProfile(user: AuthUser) {
        mutableState.update { it.copy(stage = AuthStage.CHECK_PROFILE, user = user, password = "") }
        when (val result = getProfile(user)) {
            is AuthResult.Failure -> fail(result.error)
            is AuthResult.Success -> mutableState.update {
                if (result.value == null) it.copy(
                    stage = AuthStage.COMPLETE_PROFILE, loading = false,
                    name = user.name, email = user.email, address = ""
                ) else it.copy(stage = AuthStage.AUTHENTICATED, loading = false)
            }
        }
    }

    private suspend fun persistProfile(user: AuthUser, profile: UserProfile) {
        when (val result = saveProfile(user, profile)) {
            is AuthResult.Failure -> fail(result.error)
            is AuthResult.Success -> mutableState.update { it.copy(stage = AuthStage.AUTHENTICATED, loading = false) }
        }
    }

    private fun fail(error: AuthError) {
        mutableState.update { it.copy(loading = false, error = error, password = "") }
    }
}
