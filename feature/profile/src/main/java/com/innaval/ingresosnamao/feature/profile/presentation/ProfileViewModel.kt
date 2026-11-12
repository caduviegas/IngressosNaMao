package com.innaval.ingresosnamao.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innaval.ingresosnamao.feature.profile.domain.GetProfileUseCase
import com.innaval.ingresosnamao.feature.profile.domain.LogoutUseCase
import com.innaval.ingresosnamao.feature.profile.domain.ProfileError
import com.innaval.ingresosnamao.feature.profile.domain.ProfileIdentity
import com.innaval.ingresosnamao.feature.profile.domain.ProfileResult
import com.innaval.ingresosnamao.feature.profile.domain.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val identity: ProfileIdentity? = null,
    val profile: UserProfile? = null,
    val error: ProfileError? = null,
    val confirmingLogout: Boolean = false,
    val loggingOut: Boolean = false,
    val signedOut: Boolean = false,
)

class ProfileViewModel(
    private val getProfile: GetProfileUseCase,
    private val logout: LogoutUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState())
    val state = mutableState.asStateFlow()

    init { startLoad() }

    fun load() {
        if (state.value.loading || state.value.loggingOut || state.value.signedOut) return
        startLoad()
    }

    private fun startLoad() {
        mutableState.value = ProfileUiState()
        viewModelScope.launch {
            when (val session = getProfile.currentUser()) {
                is ProfileResult.Failure -> fail(session.error)
                is ProfileResult.Success -> {
                    val identity = session.value
                    if (identity == null) {
                        mutableState.value = ProfileUiState(loading = false, signedOut = true)
                    } else {
                        mutableState.update { it.copy(identity = identity) }
                        when (val result = getProfile(identity)) {
                            is ProfileResult.Failure -> fail(result.error)
                            is ProfileResult.Success -> mutableState.update {
                                it.copy(loading = false, profile = result.value)
                            }
                        }
                    }
                }
            }
        }
    }

    fun requestLogout() {
        mutableState.update {
            if (it.loading || it.loggingOut || it.identity == null) it
            else it.copy(confirmingLogout = true, error = null)
        }
    }

    fun cancelLogout() {
        mutableState.update { if (it.loggingOut) it else it.copy(confirmingLogout = false) }
    }

    fun confirmLogout() {
        val current = state.value
        val identity = current.identity ?: return
        if (!current.confirmingLogout || current.loggingOut || current.loading) return
        mutableState.update { it.copy(confirmingLogout = false, loggingOut = true, error = null) }
        viewModelScope.launch {
            when (val result = logout(identity.uid)) {
                is ProfileResult.Failure -> fail(result.error)
                is ProfileResult.Success -> mutableState.value = ProfileUiState(loading = false, signedOut = true)
            }
        }
    }

    private fun fail(error: ProfileError) {
        mutableState.update { it.copy(loading = false, loggingOut = false, error = error) }
    }
}
