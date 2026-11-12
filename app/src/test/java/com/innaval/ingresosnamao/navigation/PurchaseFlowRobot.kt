package com.innaval.ingresosnamao.navigation

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode

internal class PurchaseFlowRobot(compose: ComposeTestRule) {
    val action = Action(compose)
    val result = Result(compose)

    class Action(private val compose: ComposeTestRule) {
        fun selectEvent() = compose.onNode(hasText("Show") and hasClickAction()).performClick()
        fun checkout() = compose.onNodeWithText("Finalizar compra").performScrollTo().performClick()
        fun continueApproved() = compose.onNodeWithText("Continuar").performClick()
        fun openTicket() {
            compose.onNode(hasScrollAction())
                .performScrollToNode(hasText("Show", substring = true) and hasClickAction())
            compose.onNode(hasText("Show", substring = true) and hasClickAction()).performClick()
        }
    }

    class Result(private val compose: ComposeTestRule) {
        fun navigationHidden() = compose.onNodeWithText("Home").assertDoesNotExist()
        fun receiptShown() = compose.onNodeWithText("Este resumo não comprova pagamento.").assertExists()
        fun ticketShown() {
            compose.onNodeWithText("Detalhes do ingresso").assertExists()
            compose.onNodeWithText("ID: ticket-id").assertExists()
            compose.onNodeWithText("Ingresso sem validade de entrada.").assertExists()
        }
    }
}
