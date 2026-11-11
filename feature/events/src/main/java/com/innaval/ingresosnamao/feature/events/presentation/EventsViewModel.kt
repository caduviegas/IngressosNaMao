package com.innaval.ingresosnamao.feature.events.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innaval.ingresosnamao.core.common.result.AppResult
import com.innaval.ingresosnamao.feature.events.domain.Event
import com.innaval.ingresosnamao.feature.events.domain.GetEventsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Content(val events: List<Event>) : EventsUiState
    data class Error(val message: String) : EventsUiState
}

class EventsViewModel(private val getEvents: GetEventsUseCase) : ViewModel() {
    private val _state = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val state: StateFlow<EventsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = EventsUiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = getEvents()) {
                is AppResult.Success -> EventsUiState.Content(result.data)
                is AppResult.Failure -> EventsUiState.Error("Não foi possível carregar os eventos")
            }
        }
    }
}
