package com.innaval.ingresosnamao.events

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

internal class EventsInstrumentedRobot(compose: ComposeTestRule) {
    val action = Action(compose)
    val result = Result(compose)

    class Action(private val compose: ComposeTestRule) {
        fun refresh() = compose.onNodeWithText("Atualizar").performClick()
    }

    class Result(private val compose: ComposeTestRule) {
        fun emptyShown() = compose.onNodeWithText("Nenhum evento disponível no momento.").assertExists()
        fun errorShown() = compose.onNodeWithText("A lista de eventos recebida é inválida.").assertExists()
    }
}
