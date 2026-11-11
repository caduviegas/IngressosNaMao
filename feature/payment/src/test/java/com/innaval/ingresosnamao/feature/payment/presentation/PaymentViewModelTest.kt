package com.innaval.ingresosnamao.feature.payment.presentation

import com.innaval.ingresosnamao.core.common.result.AppResult
import com.innaval.ingresosnamao.feature.payment.domain.PayUseCase
import com.innaval.ingresosnamao.feature.payment.domain.PaymentReceipt
import com.innaval.ingresosnamao.feature.payment.domain.PaymentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<PaymentRepository>()

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun cleanup() = Dispatchers.resetMain()

    @Test fun paidStatePreventsDuplicatePayment() = runTest(dispatcher) {
        val result = CompletableDeferred<AppResult<PaymentReceipt>>()
        coEvery { repository.pay(100) } coAnswers { result.await() }
        val vm = PaymentViewModel(PayUseCase(repository))
        vm.submit(100)
        vm.submit(100)
        runCurrent()
        result.complete(AppResult.Success(PaymentReceipt("order", 100)))
        advanceUntilIdle()
        assertTrue(vm.state.value.paid)
        vm.submit(100)
        coVerify(exactly = 1) { repository.pay(any()) }
    }

    @Test fun invalidTotalIsReportedWithoutCallingProvider() = runTest(dispatcher) {
        val vm = PaymentViewModel(PayUseCase(repository))
        vm.submit(0)
        assertTrue(vm.state.value.error)
        coVerify(exactly = 0) { repository.pay(any()) }
    }
}
