package com.innaval.ingresosnamao.navigation

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

internal class ProfileFlowRobot(compose: ComposeTestRule) {
    val action = Action(compose)
    val result = Result(compose)

    class Action(private val compose: ComposeTestRule) {
        fun openProfile() = compose.onNode(hasText("Meu perfil") and hasClickAction()).performClick()
        fun requestLogout() = compose.onNodeWithText("Sair da conta").performClick()
        fun confirmLogout() = compose.onNodeWithText("Sair").performClick()
    }

    class Result(private val compose: ComposeTestRule) {
        fun profileShown() = compose.onNodeWithText("Alice").assertExists()
        fun loginShown() = compose.onNodeWithText("Entrar com Google").assertExists()
    }
}
