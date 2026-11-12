package com.innaval.ingresosnamao.feature.auth.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.auth.domain.AuthError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthRobotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun givenLoginAndRegistrationWhenDisplayedThenShowsErrorsAndActions() {
        var state by mutableStateOf(AuthUiState(loading = false))
        var registrations = 0
        compose.setContent {
            IngressosTheme {
                AuthScreen(
                    state = state, onName = {}, onEmail = {}, onAddress = {}, onPassword = {},
                    onLogin = {}, onRegister = { registrations++ }, onGoogle = {}, onCompleteProfile = {},
                    onRetry = {}, onLeave = {}, showRegister = { state = state.copy(stage = AuthStage.REGISTER) },
                    showLogin = { state = state.copy(stage = AuthStage.LOGIN) },
                )
            }
        }
        val robot = AuthRobot(compose)
        robot.action.openRegistration()
        robot.result.registrationShown()
        robot.action.register()
        compose.runOnIdle {
            assertEquals(1, registrations)
            state = state.copy(stage = AuthStage.LOGIN, error = AuthError.INVALID_CREDENTIALS)
        }
        robot.result.invalidCredentialsShown()
        compose.runOnIdle { state = state.copy(loading = true) }
        robot.result.loginDisabled()
    }
}
