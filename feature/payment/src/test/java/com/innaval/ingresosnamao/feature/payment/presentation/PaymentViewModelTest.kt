package com.innaval.ingresosnamao.feature.payment.presentation

import com.innaval.ingresosnamao.feature.payment.domain.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<PaymentRepository>()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { repository.state } returns MutableStateFlow(PaymentState.Ready)
        every { repository.expire() } returns Unit
    }

    @After fun cleanup() = Dispatchers.resetMain()

    @Test fun displaysExplicitStartFailure() {
        val order = PaymentOrder("u", "fp", listOf(PaymentItem("id", "item", 1, 100)), 100)
        val credentials = TestCredentials("client", "token")
        every { repository.start(order, credentials) } returns StartResult.Failure("Indisponível")
        val viewModel = PaymentViewModel(StartPaymentUseCase(repository), repository)
        viewModel.submit(order, credentials)
        assertEquals("Indisponível", viewModel.error.value)
        verify(exactly = 1) { repository.start(order, credentials) }
    }

    @Test fun failedStockValidationBlocksPaymentAndDuplicateSubmissions() {
        val order = PaymentOrder("u", "fp", listOf(PaymentItem("id", "item", 1, 100)), 100)
        val credentials = TestCredentials("client", "token")
        val validation = CompletableDeferred<String?>()
        val viewModel = PaymentViewModel(StartPaymentUseCase(repository), repository)
        viewModel.submitValidated(order, credentials) { validation.await() }
        viewModel.submitValidated(order, credentials) { null }
        dispatcher.scheduler.runCurrent()
        assertEquals(true, viewModel.validating.value)
        validation.complete("Quantidade indisponível")
        dispatcher.scheduler.runCurrent()
        assertEquals("Quantidade indisponível", viewModel.error.value)
        assertEquals(false, viewModel.validating.value)
        verify(exactly = 0) { repository.start(any(), any()) }
    }
}
