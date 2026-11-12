package com.innaval.ingresosnamao.feature.events.presentation

import com.innaval.ingresosnamao.core.ui.format.formatPriceBrl
import com.innaval.ingresosnamao.feature.events.domain.Event
import com.innaval.ingresosnamao.feature.events.domain.EventList
import com.innaval.ingresosnamao.feature.events.domain.EventRepository
import com.innaval.ingresosnamao.feature.events.domain.EventsError
import com.innaval.ingresosnamao.feature.events.domain.EventsResult
import com.innaval.ingresosnamao.feature.events.domain.GetEventsUseCase
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<EventRepository>()
    private val event = Event("id", "https://example.com/a.jpg", "Show", 12500, 10)

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = EventsViewModel(GetEventsUseCase(repository))

    @Test fun givenEventsWhenLoadedThenMapsAndFormatsPrices() = runTest(dispatcher) {
        coEvery { repository.getEvents() } returns EventsResult.Success(EventList(listOf(event), false))
        val vm = viewModel()
        assertEquals(EventsUiState.Loading, vm.state.value)
        advanceUntilIdle()
        assertEquals(
            EventsUiState.Content(
                listOf(EventUiModel(event.id, event.imageUrl, event.name, "R$ 125,00", event.priceCents, 10)), false
            ),
            vm.state.value,
        )
        assertEquals("R$ 1.234,56", formatPriceBrl(123456))
        assertEquals("R$ 0,00", formatPriceBrl(0))
        assertEquals("R$ 92.233.720.368.547.758,07", formatPriceBrl(Long.MAX_VALUE))
    }

    @Test fun givenEmptyCacheWhenLoadedThenShowsStaleEmptyState() = runTest(dispatcher) {
        coEvery { repository.getEvents() } returns EventsResult.Success(EventList(emptyList(), true))
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(EventsUiState.Empty(true), vm.state.value)
    }

    @Test fun givenFailedRequestWhenRetriedThenLoadsEvents() = runTest(dispatcher) {
        coEvery { repository.getEvents() } returnsMany listOf(
            EventsResult.Failure(EventsError.INVALID_DATA),
            EventsResult.Success(EventList(listOf(event), true))
        )
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(EventsUiState.Error(EventsError.INVALID_DATA), vm.state.value)
        vm.load()
        advanceUntilIdle()
        assertEquals(
            EventsUiState.Content(
                listOf(EventUiModel(event.id, event.imageUrl, event.name, "R$ 125,00", event.priceCents, 10)), true
            ),
            vm.state.value,
        )
        coVerify(exactly = 2) { repository.getEvents() }
    }

    @Test fun givenLoadingRequestWhenLoadedAgainThenAvoidsDuplicate() = runTest(dispatcher) {
        val pending = CompletableDeferred<EventsResult>()
        coEvery { repository.getEvents() } coAnswers { pending.await() }
        val vm = viewModel()
        vm.load()
        runCurrent()
        vm.load()
        coVerify(exactly = 1) { repository.getEvents() }
        pending.complete(EventsResult.Success(EventList(emptyList(), false)))
        advanceUntilIdle()
        assertEquals(EventsUiState.Empty(false), vm.state.value)
    }
}
