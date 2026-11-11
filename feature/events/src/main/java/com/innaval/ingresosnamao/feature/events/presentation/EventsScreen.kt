package com.innaval.ingresosnamao.feature.events.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innaval.ingresosnamao.core.ui.components.ErrorState
import com.innaval.ingresosnamao.core.ui.components.LoadingState
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import org.koin.compose.viewmodel.koinViewModel

/** Stateful entry point, called by :app's NavHost. */
@Composable
fun EventsRoute(onEventClick: (eventId: String) -> Unit, viewModel: EventsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EventsScreen(state, onEventClick, viewModel::load)
}

/** Stateless and previewable. */
@Composable
fun EventsScreen(state: EventsUiState, onEventClick: (String) -> Unit, onRetry: () -> Unit) {
    when (state) {
        EventsUiState.Loading -> LoadingState()
        is EventsUiState.Error -> ErrorState(state.message, onRetry)
        is EventsUiState.Content -> LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(IngressosTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.sm),
        ) {
            items(state.events, key = { it.id }) { event ->
                Card(onClick = { onEventClick(event.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(IngressosTheme.spacing.md)) {
                        Text(event.name, style = MaterialTheme.typography.titleMedium)
                        Text(event.venue, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
