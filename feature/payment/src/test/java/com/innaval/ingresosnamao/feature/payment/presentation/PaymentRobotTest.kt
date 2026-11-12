package com.innaval.ingresosnamao.feature.payment.presentation

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.payment.domain.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class PaymentRobot(private val compose: ComposeTestRule) {
    fun assertUnknownBlocksContinuing() {
        compose.onNodeWithText("Resultado desconhecido. Concilie a tentativa antes de outro pagamento.").assertExists()
        compose.onNodeWithText("Continuar").assertDoesNotExist()
    }
    fun continueApproved() {
        compose.onNodeWithText("Pagamento de teste aprovado. Este resultado não é válido para produção.").assertExists()
        compose.onNodeWithText("Continuar").performClick()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PaymentRobotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unknownAttemptBlocksContinuationUntilApproved() {
        val order = PaymentOrder("alice", "fingerprint", listOf(PaymentItem("event", "Show", 1, 100)), 100)
        val attempt = PaymentAttempt("reference", order, 1, AttemptStatus.UNKNOWN)
        val state = MutableStateFlow<PaymentState>(PaymentState.Active(attempt))
        val repository = mockk<PaymentRepository>()
        every { repository.state } returns state
        every { repository.expire() } returns Unit
        val viewModel = PaymentViewModel(StartPaymentUseCase(repository), repository)
        var continued = 0
        compose.setContent {
            IngressosTheme { PaymentRoute(order, true, { continued++ }, {}, viewModel, validate = { null }) }
        }
        val robot = PaymentRobot(compose)
        robot.assertUnknownBlocksContinuing()
        compose.runOnIdle { state.value = PaymentState.Active(attempt.copy(status = AttemptStatus.APPROVED)) }
        robot.continueApproved()
        compose.runOnIdle { assertEquals(1, continued) }
    }

    @Test fun unresolvedAttemptsRemainBlocked() {
        val order = PaymentOrder("alice", "fingerprint", listOf(PaymentItem("event", "Show", 1, 100)), 100)
        val attempt = PaymentAttempt("reference", order, 1, AttemptStatus.WAITING)
        val state = MutableStateFlow<PaymentState>(PaymentState.Active(attempt))
        val repository = mockk<PaymentRepository>()
        every { repository.state } returns state
        every { repository.expire() } returns Unit
        val viewModel = PaymentViewModel(StartPaymentUseCase(repository), repository)
        compose.setContent { IngressosTheme { PaymentRoute(order, true, {}, {}, viewModel, validate = { null }) } }
        compose.onNodeWithText("Aguardando retorno do emulador. Não inicie outro pagamento.").assertExists()
        compose.runOnIdle { state.value = PaymentState.Active(attempt.copy(status = AttemptStatus.CANCELLED)) }
        compose.onNodeWithText("Cancelamento recebido sem identificação verificável. A tentativa permanece bloqueada.").assertExists()
        compose.runOnIdle { state.value = PaymentState.Active(attempt.copy(status = AttemptStatus.ERROR)) }
        compose.onNodeWithText("Erro recebido sem identificação verificável. A tentativa permanece bloqueada.").assertExists()
        compose.runOnIdle {
            state.value = PaymentState.Active(attempt.copy(order = order.copy(userId = "other")))
        }
        compose.onNodeWithText("Há outra tentativa neste dispositivo. Resolva-a antes de iniciar outro pagamento.").assertExists()
        compose.onNodeWithText("Continuar").assertDoesNotExist()
    }
}
