package com.innaval.ingresosnamao.navigation

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo

internal class MainNavigationRobot(compose: ComposeTestRule) {
    val action = Action(compose)
    val result = Result(compose)

    class Action(private val compose: ComposeTestRule) {
        fun select(label: String) = compose.onNode(hasText(label) and hasClickAction()).performClick()
        fun selectEvent() = compose.onNode(hasText("Show") and hasClickAction()).performClick()
        fun checkout() = compose.onNodeWithText("Finalizar compra").performScrollTo().performClick()
        fun openOrder() = compose.onNode(hasText("Pedido order", substring = true) and hasClickAction()).performClick()
        fun requestLogout() = compose.onNodeWithText("Sair da conta").performClick()
        fun confirmLogout() = compose.onNodeWithText("Sair").performClick()
        fun back() = compose.onNodeWithContentDescription("Voltar").performClick()
    }

    class Result(private val compose: ComposeTestRule) {
        fun selected(label: String) = compose.onNode(hasText(label) and hasClickAction()).assertIsSelected()
        fun disabled(label: String) = compose.onNode(hasText(label) and hasClickAction()).assertIsNotEnabled()
        fun shown(label: String) = compose.onNodeWithText(label).assertExists()
        fun hidden(label: String) = compose.onNodeWithText(label).assertDoesNotExist()
        fun backDisabled() = compose.onNodeWithContentDescription("Voltar").assertIsNotEnabled()
    }
}
