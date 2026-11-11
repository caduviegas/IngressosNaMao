package com.innaval.ingresosnamao.feature.ticket.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.ticket.domain.ObserveTicketsUseCase
import com.innaval.ingresosnamao.feature.ticket.domain.Ticket
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.compose.viewmodel.koinViewModel

data class TicketsUiState(val tickets: List<Ticket> = emptyList())

class TicketsViewModel(observeTickets: ObserveTicketsUseCase) : ViewModel() {
    val state: StateFlow<TicketsUiState> = observeTickets()
        .map(::TicketsUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TicketsUiState())
}

@Composable
fun TicketsRoute(viewModel: TicketsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TicketsScreen(state)
}

@Composable
fun TicketsScreen(state: TicketsUiState) {
    Column(Modifier.padding(IngressosTheme.spacing.md)) {
        Text("Meus ingressos", style = MaterialTheme.typography.headlineSmall)
        state.tickets.forEach { Text(it.eventName) }
    }
}
