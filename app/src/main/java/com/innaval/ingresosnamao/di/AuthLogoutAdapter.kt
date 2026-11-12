package com.innaval.ingresosnamao.di

import com.innaval.ingresosnamao.feature.auth.domain.AuthError
import com.innaval.ingresosnamao.feature.auth.domain.AuthResult
import com.innaval.ingresosnamao.feature.auth.domain.AuthSessionLogout
import com.innaval.ingresosnamao.feature.profile.domain.LogoutUseCase
import com.innaval.ingresosnamao.feature.profile.domain.ProfileError
import com.innaval.ingresosnamao.feature.profile.domain.ProfileResult

internal class AuthLogoutAdapter(private val logout: LogoutUseCase) : AuthSessionLogout {
    override suspend fun signOut(userId: String): AuthResult<Unit> = when (val result = logout(userId)) {
        is ProfileResult.Success -> AuthResult.Success(Unit)
        is ProfileResult.Failure -> AuthResult.Failure(
            when (result.error) {
                ProfileError.CART -> AuthError.LOGOUT_CART
                ProfileError.SESSION_CHANGED -> AuthError.LOGOUT_SESSION_CHANGED
                ProfileError.SESSION_CHANGED_AFTER_CLEAR -> AuthError.LOGOUT_SESSION_CHANGED_AFTER_CLEAR
                ProfileError.SIGN_OUT_AFTER_CLEAR -> AuthError.LOGOUT_AFTER_CLEAR
                else -> AuthError.UNKNOWN
            }
        )
    }
}
