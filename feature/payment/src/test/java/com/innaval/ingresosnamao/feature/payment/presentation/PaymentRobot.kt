package com.innaval.ingresosnamao.feature.payment.presentation

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

internal class PaymentRobot(compose: ComposeTestRule) {
    val action = Action(compose)
    val result = Result(compose)

    class Action(private val compose: ComposeTestRule) {
        fun continuePayment() = compose.onNodeWithText("Continuar").performClick()
        fun requestDiscard() = compose.onNodeWithText("Descartar tentativa").performClick()
        fun cancelDiscard() = compose.onNodeWithText("Não descartar").performClick()
        fun confirmDiscard() = compose.onNodeWithText("Descartar").performClick()
    }

    class Result(private val compose: ComposeTestRule) {
        fun unknownBlocksContinuation() {
            compose.onNodeWithText("Resultado desconhecido.").assertExists()
            compose.onNodeWithText("Continuar").assertDoesNotExist()
        }
        fun waitingShown() = compose.onNodeWithText("Aguardando retorno da Cielo.").assertExists()
        fun credentialFieldsAbsent() {
            compose.onNodeWithText("Client ID de teste").assertDoesNotExist()
            compose.onNodeWithText("Access token de teste").assertDoesNotExist()
        }
        fun approvedShown() = compose.onNodeWithText("Retorno do pagamento recebido.").assertExists()
        fun cancelledShown() = compose.onNodeWithText("Pagamento cancelado.").assertExists()
        fun errorShown() = compose.onNodeWithText("O pagamento retornou um erro.").assertExists()
        fun differentAttemptBlocksContinuation() {
            compose.onNodeWithText("Há outra tentativa neste dispositivo. Resolva-a antes de iniciar outro pagamento.")
                .assertExists()
            compose.onNodeWithText("Continuar").assertDoesNotExist()
        }
        fun authenticationErrorShown() =
            compose.onNodeWithText("Cielo: erro de autenticação (código 4). Verifique as credenciais configuradas.")
                .assertExists()
        fun retryShown() = compose.onNodeWithText("Tentar novamente").assertExists()
        fun discardAvailable() = compose.onNodeWithText("Descartar tentativa").assertExists()
        fun discardUnavailable() = compose.onNodeWithText("Descartar tentativa").assertDoesNotExist()
        fun backDisabled() = compose.onNodeWithContentDescription("Voltar").assertIsNotEnabled()
    }
}
