package com.innaval.ingresosnamao.feature.cart.presentation

import com.innaval.ingresosnamao.feature.cart.domain.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<CartRepository>()
    private val item = CartItem("event", "Show", 100, 1)
    private val cart = CartSnapshot(listOf(item), 100)

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun cleanup() = Dispatchers.resetMain()

    private fun viewModel() = CartViewModel(
        "user", ObserveCartUseCase(repository), GetCartUseCase(repository),
        SetCartQuantityUseCase(repository), RemoveFromCartUseCase(repository),
    )

    @Test fun restoresContentAndPreventsRemovalViaDecrement() = runTest(dispatcher) {
        every { repository.observe("user") } returns flowOf(CartResult.Success(cart))
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(cart, vm.state.value.cart)
        vm.changeQuantity(item, -1)
        coVerify(exactly = 0) { repository.setQuantity(any(), any(), any()) }
    }

    @Test fun failureIsNotEmptyAndRetryRestores() = runTest(dispatcher) {
        every { repository.observe("user") } returnsMany listOf(
            flowOf(CartResult.Failure(CartError.STORAGE)),
            flowOf(CartResult.Success(cart)),
        )
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(CartError.STORAGE, vm.state.value.error)
        assertEquals(null, vm.state.value.cart)
        vm.retry()
        advanceUntilIdle()
        assertEquals(cart, vm.state.value.cart)
    }

    @Test fun duplicateOperationsAreBlockedAndErrorsAreExplicit() = runTest(dispatcher) {
        every { repository.observe("user") } returns flowOf(CartResult.Success(cart))
        val pending = CompletableDeferred<CartResult<Unit>>()
        coEvery { repository.remove("user", "event") } coAnswers { pending.await() }
        val vm = viewModel()
        advanceUntilIdle()
        vm.remove(item)
        vm.remove(item)
        runCurrent()
        assertTrue(vm.state.value.busy)
        pending.complete(CartResult.Failure(CartError.STORAGE))
        advanceUntilIdle()
        assertEquals(CartError.STORAGE, vm.state.value.error)
        assertEquals(cart, vm.state.value.cart)
        coVerify(exactly = 1) { repository.remove("user", "event") }
    }

    @Test fun checkoutReadsLatestDatabaseTotal() = runTest(dispatcher) {
        every { repository.observe("user") } returns flowOf(CartResult.Success(cart))
        coEvery { repository.get("user") } returns CartResult.Success(CartSnapshot(listOf(item.copy(quantity = 2)), 200))
        val vm = viewModel()
        advanceUntilIdle()
        var amount: Long? = null
        vm.checkout { total, _ -> amount = total }
        advanceUntilIdle()
        assertEquals(200L, amount)
    }

    @Test fun freeCartCannotStartPayment() = runTest(dispatcher) {
        val free = CartSnapshot(listOf(item.copy(unitPriceCents = 0)), 0)
        every { repository.observe("user") } returns flowOf(CartResult.Success(free))
        coEvery { repository.get("user") } returns CartResult.Success(free)
        val vm = viewModel()
        advanceUntilIdle()
        var started = false
        vm.checkout { _, _ -> started = true }
        advanceUntilIdle()
        assertEquals(false, started)
        assertEquals(CartError.FREE_CHECKOUT_UNSUPPORTED, vm.state.value.error)
    }
}
