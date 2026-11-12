package com.innaval.ingresosnamao.feature.auth.presentation

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

internal class AuthRobot(compose: ComposeTestRule) {
    val action = Action(compose)
    val result = Result(compose)

    class Action(private val compose: ComposeTestRule) {
        fun openRegistration() = compose.onNodeWithText("Criar conta").performClick()
        fun register() = compose.onNodeWithText("Cadastrar").performClick()
    }

    class Result(private val compose: ComposeTestRule) {
        fun registrationShown() = compose.onNodeWithText("Cadastrar").assertExists()
        fun invalidCredentialsShown() = compose.onNodeWithText("E-mail ou senha inválidos.").assertExists()
        fun loginDisabled() = compose.onNode(hasText("Entrar") and hasClickAction()).assertIsNotEnabled()
    }
}
