package com.innaval.ingresosnamao.feature.auth.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.auth.domain.AuthError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class AuthRobot(private val compose: ComposeTestRule) {
    fun openRegistration() = compose.onNodeWithText("Criar conta").performClick()
    fun assertRegistration() = compose.onNodeWithText("Cadastrar").assertExists()
    fun assertError() = compose.onNodeWithText("E-mail ou senha inválidos.").assertExists()
    fun assertLoginDisabled() = compose.onNode(hasText("Entrar") and hasClickAction()).assertIsNotEnabled()
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthRobotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun registrationAndExplicitErrorsAreVisible() {
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
        robot.openRegistration()
        robot.assertRegistration()
        compose.onNodeWithText("Cadastrar").performClick()
        compose.runOnIdle {
            assertEquals(1, registrations)
            state = state.copy(stage = AuthStage.LOGIN, error = AuthError.INVALID_CREDENTIALS)
        }
        robot.assertError()
        compose.runOnIdle { state = state.copy(loading = true) }
        robot.assertLoginDisabled()
    }
}
