package com.innaval.ingresosnamao.feature.ticket.presentation

import com.innaval.ingresosnamao.feature.ticket.domain.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TicketViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<TicketRepository>()
    private val qr = mockk<QrCodeGenerator>()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun cleanup() = Dispatchers.resetMain()

    @Test fun detailShowsQrFailureForOwnedTicket() = runTest(dispatcher) {
        val ticket = Ticket("id", "ref", "event", "Show", 100)
        every { repository.observeTicket("alice", "id") } returns flowOf(TicketResult.Success(ticket))
        coEvery { qr.generate("id") } returns QrResult.Failure
        val vm = TicketDetailViewModel("alice", "id", ObserveTicketUseCase(repository), qr)
        advanceUntilIdle()
        val state = vm.state.value as TicketUiState.Content
        assertEquals(QrResult.Failure, state.value.qr)
        coVerify(exactly = 1) { qr.generate("id") }
    }

    @Test fun listDistinguishesEmptyAndStorageError() = runTest(dispatcher) {
        every { repository.observePurchases("alice") } returnsMany listOf(
            flowOf(TicketResult.Success(emptyList())),
            flowOf(TicketResult.Failure(TicketError.STORAGE)),
        )
        val vm = TicketsViewModel("alice", ObservePurchasesUseCase(repository))
        advanceUntilIdle()
        assertTrue(vm.state.value is TicketUiState.Empty)
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.state.value is TicketUiState.Error)
    }
}
