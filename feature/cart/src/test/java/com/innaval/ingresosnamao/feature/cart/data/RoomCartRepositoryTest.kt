package com.innaval.ingresosnamao.feature.cart.data

import com.innaval.ingresosnamao.core.common.dispatchers.DispatcherProvider
import com.innaval.ingresosnamao.core.database.cart.CartEntity
import com.innaval.ingresosnamao.feature.cart.domain.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class RoomCartRepositoryTest {
    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = mockk<DispatcherProvider> { every { io } returns dispatcher }
    private val source = mockk<CartLocalDataSource>()
    private val repository = RoomCartRepository(source, dispatchers)
    private val item = CartItem("event", "Show", 100, 1)

    @Test fun validatesBeforeCallingDatabase() = runTest(dispatcher) {
        assertEquals(CartResult.Failure(CartError.INVALID_ITEM), repository.add("", item))
        assertEquals(CartResult.Failure(CartError.INVALID_ITEM), repository.add("u", item.copy(quantity = 0)))
        assertEquals(CartResult.Failure(CartError.INVALID_ITEM), repository.setQuantity("u", "event", 0))
        coVerify(exactly = 0) { source.add(any(), any()) }
        coVerify(exactly = 0) { source.setQuantity(any(), any(), any()) }
    }

    @Test fun observesAndRestoresOnlyRequestedAccount() = runTest(dispatcher) {
        every { source.observe("u") } returns flowOf(listOf(CartEntity("u", "event", "Show", 100, 2)))
        assertEquals(
            CartResult.Success(CartSnapshot(listOf(item.copy(quantity = 2)), 200)),
            repository.observe("u").first(),
        )
    }

    @Test fun reportsReadAndWriteErrorsWithoutEmptyFallback() = runTest(dispatcher) {
        every { source.observe("u") } returns flow { throw IllegalStateException("db") }
        assertEquals(CartResult.Failure(CartError.STORAGE), repository.observe("u").first())
        coEvery { source.add("u", item) } throws IllegalStateException("db")
        assertEquals(CartResult.Failure(CartError.STORAGE), repository.add("u", item))
    }

    @Test fun propagatesCancellation() = runTest(dispatcher) {
        coEvery { source.add("u", item) } throws CancellationException("cancelled")
        try {
            repository.add("u", item)
            fail("CancellationException expected")
        } catch (_: CancellationException) {
            // Cancellation is not a storage error.
        }
    }
}
