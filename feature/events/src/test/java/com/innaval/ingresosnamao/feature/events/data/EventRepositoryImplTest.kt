package com.innaval.ingresosnamao.feature.events.data

import com.innaval.ingresosnamao.core.common.dispatchers.DispatcherProvider
import com.innaval.ingresosnamao.feature.events.domain.Event
import com.innaval.ingresosnamao.feature.events.domain.EventList
import com.innaval.ingresosnamao.feature.events.domain.EventsError
import com.innaval.ingresosnamao.feature.events.domain.EventsResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class EventRepositoryImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = mockk<DispatcherProvider> { every { io } returns dispatcher }
    private val source = mockk<EventsRemoteDataSource>()
    private val repository = EventRepositoryImpl(source, EventsJsonParser(), dispatchers)
    private val json = """[{"sku":1,"name":"Show","imageUrl":"https://example.com/a.jpg",""" +
        """"price":"125.00","quantity":3}]"""
    private val events = listOf(Event("1", "https://example.com/a.jpg", "Show", 12500, 3))

    @Test fun givenFreshFetchWhenItFailsThenUsesUnthrottledPathAndMarksStale() = runTest(dispatcher) {
        coEvery { source.fetchFreshAndActivate() } returns Unit
        every { source.activatedEvents() } returns json
        assertEquals(EventsResult.Success(EventList(events, false)), repository.getFreshEvents())
        coEvery { source.fetchFreshAndActivate() } throws Exception("offline")
        assertEquals(EventsResult.Success(EventList(events, true)), repository.getFreshEvents())
    }

    @Test fun givenActivatedRemoteValueWhenLoadedThenReturnsEvents() = runTest(dispatcher) {
        coEvery { source.fetchAndActivate() } returns Unit
        every { source.activatedEvents() } returns json
        assertEquals(EventsResult.Success(EventList(events, false)), repository.getEvents())
    }

    @Test fun givenFetchFailureWhenCacheExistsThenReturnsStaleCache() = runTest(dispatcher) {
        coEvery { source.fetchAndActivate() } throws Exception("offline")
        every { source.activatedEvents() } returns json
        assertEquals(EventsResult.Success(EventList(events, true)), repository.getEvents())
        every { source.activatedEvents() } returns "[]"
        assertEquals(EventsResult.Success(EventList(emptyList(), true)), repository.getEvents())
        every { source.activatedEvents() } returns null
        assertEquals(EventsResult.Failure(EventsError.UNAVAILABLE), repository.getEvents())
        every { source.activatedEvents() } returns "[invalid"
        assertEquals(EventsResult.Failure(EventsError.INVALID_DATA), repository.getEvents())
    }

    @Test fun givenMissingKeyWhenEventsRequestedThenReturnsError() = runTest(dispatcher) {
        coEvery { source.fetchAndActivate() } returns Unit
        every { source.activatedEvents() } returns null
        assertEquals(EventsResult.Failure(EventsError.MISSING_DATA), repository.getEvents())
    }

    @Test fun givenMissingFirebaseConfigurationWhenEventsRequestedThenReturnsError() = runTest(dispatcher) {
        coEvery { source.fetchAndActivate() } throws IllegalStateException("Firebase not initialized")
        assertEquals(EventsResult.Failure(EventsError.CONFIGURATION), repository.getEvents())
    }

    @Test fun givenCancelledFetchWhenEventsRequestedThenPropagatesCancellation() = runTest(dispatcher) {
        coEvery { source.fetchAndActivate() } throws CancellationException("cancelled")
        try {
            repository.getEvents()
            fail("CancellationException expected")
        } catch (_: CancellationException) {
            // Cancellation is not a cache hit.
        }
    }
}
