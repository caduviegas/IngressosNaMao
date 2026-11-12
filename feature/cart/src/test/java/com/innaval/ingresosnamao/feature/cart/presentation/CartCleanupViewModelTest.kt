package com.innaval.ingresosnamao.feature.cart.presentation

import com.innaval.ingresosnamao.feature.cart.domain.CartError
import com.innaval.ingresosnamao.feature.cart.domain.CartRepository
import com.innaval.ingresosnamao.feature.cart.domain.CartResult
import com.innaval.ingresosnamao.feature.cart.domain.ClearCartUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartCleanupViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<CartRepository>()

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun cleanup() = Dispatchers.resetMain()

    @Test fun failedClearCanBeRetriedWithoutAnotherPayment() = runTest(dispatcher) {
        val fingerprint = "a".repeat(64)
        coEvery { repository.clear("user", fingerprint) } returnsMany listOf(
            CartResult.Failure(CartError.STORAGE), CartResult.Success(Unit)
        )
        val vm = CartCleanupViewModel("user", fingerprint, ClearCartUseCase(repository))
        advanceUntilIdle()
        assertEquals(CartCleanupState.Error(CartError.STORAGE), vm.state.value)
        vm.retry()
        vm.retry()
        advanceUntilIdle()
        assertEquals(CartCleanupState.Complete, vm.state.value)
        vm.retry()
        coVerify(exactly = 2) { repository.clear("user", fingerprint) }
    }
}
