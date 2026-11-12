package com.innaval.ingresosnamao.feature.ticket.presentation

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

internal class TicketRobot(compose: ComposeTestRule) {
    val action = Action(compose)
    val result = Result(compose)

    class Action(private val compose: ComposeTestRule) {
        fun retry() = compose.onNodeWithText("Tentar novamente").performClick()
        fun openTicket() = compose.onNode(hasText("Show", substring = true) and hasClickAction()).performClick()
    }

    class Result(private val compose: ComposeTestRule) {
        fun receiptErrorShown() = compose.onNodeWithText("Não foi possível carregar o comprovante.").assertExists()
        fun receiptShown() {
            compose.onNodeWithText("Este resumo não comprova pagamento.").assertExists()
            compose.onNodeWithText("Ingressos: 1").assertExists()
        }
        fun invalidTicketAndQrErrorShown() {
            compose.onNodeWithText("Ingresso sem validade de entrada.").assertExists()
            compose.onNodeWithText("Não foi possível gerar o QR Code.").assertExists()
        }
        fun qrShown() = compose.onNodeWithContentDescription("QR Code do ingresso").assertExists()
    }
}
