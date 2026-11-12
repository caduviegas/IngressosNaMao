package com.innaval.ingresosnamao.di

import com.innaval.ingresosnamao.feature.cart.domain.AvailableEvent
import com.innaval.ingresosnamao.feature.cart.domain.CartError
import com.innaval.ingresosnamao.feature.cart.domain.CartResult
import com.innaval.ingresosnamao.feature.events.domain.Event
import com.innaval.ingresosnamao.feature.events.domain.EventList
import com.innaval.ingresosnamao.feature.events.domain.EventRepository
import com.innaval.ingresosnamao.feature.events.domain.EventsError
import com.innaval.ingresosnamao.feature.events.domain.EventsResult
import com.innaval.ingresosnamao.feature.events.domain.GetEventsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CartAvailabilityAdapterTest {
    private val repository = mockk<EventRepository>()
    private val adapter = CartAvailabilityAdapter(GetEventsUseCase(repository))
    private val events = listOf(Event("1", "https://example.com/image", "Show", 4990, 120))

    @Test fun onlySuccessfulFreshCatalogIsAccepted() = runTest {
        coEvery { repository.getFreshEvents() } returnsMany listOf(
            EventsResult.Success(EventList(events, false)),
            EventsResult.Success(EventList(events, true)),
            EventsResult.Failure(EventsError.UNAVAILABLE),
        )
        assertEquals(CartResult.Success(listOf(AvailableEvent("1", 4990, 120))), adapter.load())
        assertEquals(CartResult.Failure(CartError.STOCK_UNAVAILABLE), adapter.load())
        assertEquals(CartResult.Failure(CartError.STOCK_UNAVAILABLE), adapter.load())
    }
}
