package com.innaval.ingresosnamao.feature.payment.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.payment.domain.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class PaymentRobot(private val compose: ComposeTestRule) {
    fun assertUnknownBlocksContinuing() {
        compose.onNodeWithText("Resultado desconhecido.").assertExists()
        compose.onNodeWithText("Continuar").assertDoesNotExist()
    }
    fun continueApproved() {
        compose.onNodeWithText("Retorno do pagamento recebido.").assertExists()
        compose.onNodeWithText("Continuar").performClick()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PaymentRobotTest {
    @get:Rule val compose = createComposeRule()
    private val credentials = TestCredentials("client", "token")

    @Test fun startsAutomaticallyWithoutCredentialFieldsOrDuplicateOnRecomposition() {
        val order = PaymentOrder("alice", "fingerprint", listOf(PaymentItem("event", "Show", 1, 100)), 100)
        val repository = mockk<PaymentRepository>()
        val state = MutableStateFlow<PaymentState>(PaymentState.Ready)
        every { repository.state } returns state
        every { repository.expire() } returns Unit
        every { repository.start(order, credentials) } answers {
            state.value = PaymentState.Active(PaymentAttempt("reference", order, 1, AttemptStatus.WAITING))
            StartResult.Started
        }
        val viewModel = PaymentViewModel(StartPaymentUseCase(repository), repository)
        val refresh = mutableStateOf(0)
        compose.setContent {
            IngressosTheme {
                Text("Atualização ${refresh.value}")
                PaymentRoute(order, true, credentials, {}, {}, viewModel, validate = { null })
            }
        }
        compose.onNodeWithText("Aguardando retorno da Cielo.").assertExists()
        compose.onNodeWithText("Client ID de teste").assertDoesNotExist()
        compose.onNodeWithText("Access token de teste").assertDoesNotExist()
        compose.runOnIdle { refresh.value++ }
        compose.waitForIdle()
        verify(exactly = 1) { repository.start(order, credentials) }
    }

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
            IngressosTheme {
                PaymentRoute(order, true, credentials, { continued++ }, {}, viewModel, validate = { null })
            }
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
        compose.setContent {
            IngressosTheme { PaymentRoute(order, true, credentials, {}, {}, viewModel, validate = { null }) }
        }
        compose.onNodeWithText("Aguardando retorno da Cielo.").assertExists()
        compose.runOnIdle { state.value = PaymentState.Active(attempt.copy(status = AttemptStatus.CANCELLED)) }
        compose.onNodeWithText("Pagamento cancelado.").assertExists()
        compose.runOnIdle { state.value = PaymentState.Active(attempt.copy(status = AttemptStatus.ERROR)) }
        compose.onNodeWithText("O pagamento retornou um erro.").assertExists()
        compose.runOnIdle {
            state.value = PaymentState.Active(attempt.copy(order = order.copy(userId = "other")))
        }
        compose.onNodeWithText("Há outra tentativa neste dispositivo. Resolva-a antes de iniciar outro pagamento.").assertExists()
        compose.onNodeWithText("Continuar").assertDoesNotExist()
    }

    @Test fun failedAttemptForDifferentCartRequiresConfirmationBeforeDiscard() {
        val order = PaymentOrder("alice", "new", listOf(PaymentItem("event", "Show", 1, 100)), 100)
        val attempt = PaymentAttempt(
            "reference", order.copy(fingerprint = "old"), 1, AttemptStatus.ERROR, errorCode = 4,
        )
        val state = MutableStateFlow<PaymentState>(PaymentState.Active(attempt))
        val repository = mockk<PaymentRepository>()
        every { repository.state } returns state
        every { repository.expire() } returns Unit
        every { repository.discardSimulation(PaymentState.Active(attempt)) } answers {
            state.value = PaymentState.Ready
            DiscardSimulationResult.Discarded
        }
        val viewModel = PaymentViewModel(StartPaymentUseCase(repository), repository)
        compose.setContent {
            IngressosTheme { PaymentRoute(order, true, credentials, {}, {}, viewModel, validate = { null }) }
        }
        compose.onNodeWithText("Há outra tentativa neste dispositivo. Resolva-a antes de iniciar outro pagamento.")
            .assertExists()
        compose.onNodeWithText("Cielo: erro de autenticação (código 4). Verifique as credenciais configuradas.")
            .assertExists()
        compose.onNodeWithText("Descartar tentativa").performClick()
        verify(exactly = 0) { repository.discardSimulation(any()) }
        compose.onNodeWithText("Não descartar").performClick()
        verify(exactly = 0) { repository.discardSimulation(any()) }
        compose.onNodeWithText("Descartar tentativa").performClick()
        compose.onNodeWithText("Descartar").performClick()
        verify(exactly = 1) { repository.discardSimulation(PaymentState.Active(attempt)) }
        compose.onNodeWithText("Tentar novamente").assertExists()
    }

    @Test fun discardIsAvailableForAllStatesAndAnotherUser() {
        val order = PaymentOrder("alice", "fp", listOf(PaymentItem("event", "Show", 1, 100)), 100)
        val attempt = PaymentAttempt("reference", order, 1, AttemptStatus.WAITING)
        val state = MutableStateFlow<PaymentState>(PaymentState.Active(attempt))
        val repository = mockk<PaymentRepository>()
        every { repository.state } returns state
        every { repository.expire() } returns Unit
        val viewModel = PaymentViewModel(StartPaymentUseCase(repository), repository)
        compose.setContent {
            IngressosTheme { PaymentRoute(order, true, credentials, {}, {}, viewModel, validate = { null }) }
        }
        compose.onNodeWithText("Descartar tentativa").assertExists()
        compose.runOnIdle { state.value = PaymentState.Active(attempt.copy(status = AttemptStatus.UNKNOWN)) }
        compose.onNodeWithText("Descartar tentativa").assertExists()
        compose.runOnIdle { state.value = PaymentState.Active(attempt.copy(status = AttemptStatus.APPROVED)) }
        compose.onNodeWithText("Descartar tentativa").assertExists()
        compose.runOnIdle {
            state.value = PaymentState.Active(
                attempt.copy(status = AttemptStatus.ERROR, order = order.copy(userId = "other"))
            )
        }
        compose.onNodeWithText("Descartar tentativa").assertExists()
        compose.runOnIdle { state.value = PaymentState.StorageError("Falha ao restaurar") }
        compose.onNodeWithText("Descartar tentativa").assertExists()
    }

    @Test fun recoveryActionCanClearStorageErrorWithoutCart() {
        val initial = PaymentState.StorageError("Não foi possível restaurar")
        val state = MutableStateFlow<PaymentState>(initial)
        val repository = mockk<PaymentRepository>()
        every { repository.state } returns state
        every { repository.expire() } returns Unit
        every { repository.discardSimulation(initial) } answers {
            state.value = PaymentState.Ready
            DiscardSimulationResult.Discarded
        }
        val viewModel = PaymentViewModel(StartPaymentUseCase(repository), repository)
        compose.setContent {
            val current = state.collectAsStateWithLifecycle()
            IngressosTheme { SimulationRecoveryAction(current.value, viewModel) }
        }
        compose.onNodeWithText("Descartar tentativa").performClick()
        compose.onNodeWithText("Descartar").performClick()
        verify(exactly = 1) { repository.discardSimulation(initial) }
        compose.onNodeWithText("Descartar tentativa").assertDoesNotExist()
    }

    @Test fun recoveryIsUnavailableInDisabledPaymentScreen() {
        val order = PaymentOrder("alice", "fp", listOf(PaymentItem("event", "Show", 1, 100)), 100)
        val state = MutableStateFlow<PaymentState>(
            PaymentState.Active(PaymentAttempt("ref", order, 1, AttemptStatus.UNKNOWN))
        )
        val repository = mockk<PaymentRepository>()
        every { repository.state } returns state
        every { repository.expire() } returns Unit
        val viewModel = PaymentViewModel(StartPaymentUseCase(repository), repository)
        compose.setContent {
            IngressosTheme { PaymentRoute(order, false, credentials, {}, {}, viewModel, validate = { null }) }
        }
        compose.onNodeWithText("Descartar tentativa").assertDoesNotExist()
        verify(exactly = 0) { repository.start(any(), any()) }
    }
}
