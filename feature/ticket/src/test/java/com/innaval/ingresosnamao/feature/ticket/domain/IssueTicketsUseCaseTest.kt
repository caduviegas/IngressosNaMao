package com.innaval.ingresosnamao.feature.ticket.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class IssueTicketsUseCaseTest {
    private val repository = mockk<TicketRepository>()
    private val purchase = SimulatedPurchase("ref", "order", "user", "fp", 100, listOf(PurchaseItem("event", "Show", 100, 1)))

    @Test fun rejectsMoreThan100BeforeRepositoryCall() = runBlocking {
        val invalid = purchase.copy(totalCents = 10100, items = listOf(PurchaseItem("event", "Show", 100, 101)))
        assertEquals(TicketResult.Failure(TicketError.INVALID_PURCHASE), IssueTicketsUseCase(repository)(invalid))
        coVerify(exactly = 0) { repository.issue(any()) }
    }

    @Test fun propagatesStorageFailure() = runBlocking {
        coEvery { repository.issue(purchase) } returns TicketResult.Failure(TicketError.STORAGE)
        assertEquals(TicketResult.Failure(TicketError.STORAGE), IssueTicketsUseCase(repository)(purchase))
        coVerify(exactly = 1) { repository.issue(purchase) }
    }

    @Test fun acceptsExactly100AndRejectsOverflow() {
        assertEquals(true, validPurchaseItems(listOf(PurchaseItem("event", "Show", 100, 100)), 10000))
        assertEquals(false, validPurchaseItems(listOf(PurchaseItem("event", "Show", Long.MAX_VALUE, 2)), Long.MAX_VALUE))
    }
}
