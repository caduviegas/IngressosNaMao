package com.innaval.ingresosnamao.feature.events.presentation

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

internal class EventsRobot(compose: ComposeTestRule) {
    val action = Action(compose)
    val result = Result(compose)

    class Action(private val compose: ComposeTestRule) {
        fun retry() = compose.onNodeWithText("Tentar novamente").performClick()
        fun refresh() = compose.onNodeWithText("Atualizar").performClick()
        fun selectEvent() = compose.onNode(hasText("Show") and hasClickAction()).performClick()
    }

    class Result(private val compose: ComposeTestRule) {
        fun emptyShown() = compose.onNodeWithText("Nenhum evento disponível no momento.").assertExists()
        fun errorShown() = compose.onNodeWithText("A lista de eventos recebida é inválida.").assertExists()
        fun eventDisabled() = compose.onNode(hasText("Show") and hasClickAction()).assertIsNotEnabled()
        fun soldOutShown() = compose.onNodeWithText("Esgotado").assertExists()
    }
}
