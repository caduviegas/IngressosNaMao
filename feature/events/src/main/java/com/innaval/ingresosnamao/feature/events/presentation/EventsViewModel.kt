package com.innaval.ingresosnamao.feature.events.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innaval.ingresosnamao.core.ui.format.formatPriceBrl
import com.innaval.ingresosnamao.feature.events.domain.EventsError
import com.innaval.ingresosnamao.feature.events.domain.EventsResult
import com.innaval.ingresosnamao.feature.events.domain.GetEventsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EventUiModel(
    val id: String, val imageUrl: String, val name: String, val price: String, val priceCents: Long, val quantity: Int
)

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Content(val events: List<EventUiModel>, val possiblyStale: Boolean) : EventsUiState
    data class Empty(val possiblyStale: Boolean) : EventsUiState
    data class Error(val error: EventsError) : EventsUiState
}

class EventsViewModel(private val getEvents: GetEventsUseCase) : ViewModel() {
    private val mutableState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val state: StateFlow<EventsUiState> = mutableState.asStateFlow()
    private var loadJob: Job? = null

    init { load() }

    fun load() {
        if (loadJob?.isActive == true) return
        mutableState.value = EventsUiState.Loading
        loadJob = viewModelScope.launch {
            mutableState.value = when (val result = getEvents()) {
                is EventsResult.Failure -> EventsUiState.Error(result.error)
                is EventsResult.Success -> {
                    val data = result.data
                    if (data.events.isEmpty()) EventsUiState.Empty(data.possiblyStale)
                    else EventsUiState.Content(
                        events = data.events.map {
                            EventUiModel(
                                it.id, it.imageUrl, it.name, formatPriceBrl(it.priceCents), it.priceCents, it.quantity
                            )
                        },
                        possiblyStale = data.possiblyStale,
                    )
                }
            }
        }
    }
}
