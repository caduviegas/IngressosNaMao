package com.innaval.ingresosnamao.feature.payment.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.payment.domain.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PaymentRobotTest {
    @get:Rule val compose = createComposeRule()
    private val credentials = TestCredentials("client", "token")

    @Test fun givenPaymentValidationWhenInProgressThenBackIsDisabled() {
        val order = PaymentOrder("alice", "fp", listOf(PaymentItem("event", "Show", 1, 100)), 100)
        val repository = mockk<PaymentRepository>()
        every { repository.state } returns MutableStateFlow(PaymentState.Ready)
        every { repository.expire() } returns Unit
        val viewModel = PaymentViewModel(StartPaymentUseCase(repository), repository)
        val pending = CompletableDeferred<String?>()
        compose.setContent {
            IngressosTheme { PaymentRoute(order, true, credentials, {}, {}, viewModel, validate = { pending.await() }) }
        }
        PaymentRobot(compose).result.backDisabled()
        compose.runOnIdle { pending.complete("Não foi possível validar o carrinho.") }
    }

    @Test fun givenReadyPaymentWhenDisplayedThenStartsOnceWithoutCredentialFields() {
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
        val robot = PaymentRobot(compose)
        robot.result.waitingShown()
        robot.result.credentialFieldsAbsent()
        compose.runOnIdle { refresh.value++ }
        compose.waitForIdle()
        verify(exactly = 1) { repository.start(order, credentials) }
    }

    @Test fun givenUnknownAttemptWhenDisplayedThenBlocksUntilApproved() {
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
        robot.result.unknownBlocksContinuation()
        compose.runOnIdle { state.value = PaymentState.Active(attempt.copy(status = AttemptStatus.APPROVED)) }
        robot.result.approvedShown()
        robot.action.continuePayment()
        compose.runOnIdle { assertEquals(1, continued) }
    }

    @Test fun givenUnresolvedAttemptWhenDisplayedThenRemainsBlocked() {
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
        val robot = PaymentRobot(compose)
        robot.result.waitingShown()
        compose.runOnIdle { state.value = PaymentState.Active(attempt.copy(status = AttemptStatus.CANCELLED)) }
        robot.result.cancelledShown()
        compose.runOnIdle { state.value = PaymentState.Active(attempt.copy(status = AttemptStatus.ERROR)) }
        robot.result.errorShown()
        compose.runOnIdle {
            state.value = PaymentState.Active(attempt.copy(order = order.copy(userId = "other")))
        }
        robot.result.differentAttemptBlocksContinuation()
    }

    @Test fun givenDifferentCartAttemptWhenDiscardingThenRequiresConfirmation() {
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
        val robot = PaymentRobot(compose)
        robot.result.differentAttemptBlocksContinuation()
        robot.result.authenticationErrorShown()
        robot.action.requestDiscard()
        verify(exactly = 0) { repository.discardSimulation(any()) }
        robot.action.cancelDiscard()
        verify(exactly = 0) { repository.discardSimulation(any()) }
        robot.action.requestDiscard()
        robot.action.confirmDiscard()
        verify(exactly = 1) { repository.discardSimulation(PaymentState.Active(attempt)) }
        robot.result.retryShown()
    }

    @Test fun givenAttemptStatesOrAnotherUserWhenDisplayedThenOffersDiscard() {
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
        val robot = PaymentRobot(compose)
        robot.result.discardAvailable()
        compose.runOnIdle { state.value = PaymentState.Active(attempt.copy(status = AttemptStatus.UNKNOWN)) }
        robot.result.discardAvailable()
        compose.runOnIdle { state.value = PaymentState.Active(attempt.copy(status = AttemptStatus.APPROVED)) }
        robot.result.discardAvailable()
        compose.runOnIdle {
            state.value = PaymentState.Active(
                attempt.copy(status = AttemptStatus.ERROR, order = order.copy(userId = "other"))
            )
        }
        robot.result.discardAvailable()
        compose.runOnIdle { state.value = PaymentState.StorageError("Falha ao restaurar") }
        robot.result.discardAvailable()
    }

    @Test fun givenStorageErrorWithoutCartWhenDiscardingThenClearsError() {
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
        val robot = PaymentRobot(compose)
        robot.action.requestDiscard()
        robot.action.confirmDiscard()
        verify(exactly = 1) { repository.discardSimulation(initial) }
        robot.result.discardUnavailable()
    }

    @Test fun givenDisabledPaymentWhenDisplayedThenRecoveryIsUnavailable() {
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
        PaymentRobot(compose).result.discardUnavailable()
        verify(exactly = 0) { repository.start(any(), any()) }
    }
}
