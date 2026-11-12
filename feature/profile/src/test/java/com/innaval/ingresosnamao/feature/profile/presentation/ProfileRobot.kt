package com.innaval.ingresosnamao.feature.profile.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

internal class ProfileRobot(compose: ComposeTestRule) {
    val action = Action(compose)
    val result = Result(compose)

    class Action(private val compose: ComposeTestRule) {
        fun confirmLogout() = compose.onNodeWithText("Sair").performClick()
        fun retry() = compose.onNodeWithText("Tentar novamente").performClick()
    }

    class Result(private val compose: ComposeTestRule) {
        fun logoutConfirmationShown() = compose.onNodeWithText("Sair da conta?").assertIsDisplayed()
        fun readErrorShown() =
            compose.onNodeWithText("Não foi possível carregar seu perfil. Tente novamente.").assertIsDisplayed()
        fun logoutAvailable() = compose.onNodeWithText("Sair da conta").assertExists()
        fun cachedProfileShown() {
            compose.onNodeWithText("Ana").assertIsDisplayed()
            compose.onNodeWithText("ana@example.com").assertIsDisplayed()
            compose.onNodeWithText("Não informado").assertIsDisplayed()
            compose.onNodeWithText("Dados salvos no dispositivo; podem estar desatualizados.").assertIsDisplayed()
        }
    }
}
