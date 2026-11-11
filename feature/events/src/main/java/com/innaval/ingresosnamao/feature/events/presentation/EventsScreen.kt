package com.innaval.ingresosnamao.feature.events.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.innaval.ingresosnamao.core.ui.components.ErrorState
import com.innaval.ingresosnamao.core.ui.components.LoadingState
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.events.domain.EventsError
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EventsRoute(
    onEventClick: (eventId: String, name: String, priceCents: Long) -> Unit,
    onCartClick: () -> Unit,
    adding: Boolean = false,
    cartError: String? = null,
    viewModel: EventsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column {
        TextButton(onClick = onCartClick, enabled = !adding) { Text("Ver carrinho") }
        if (adding) Text("Adicionando ao carrinho…", Modifier.padding(IngressosTheme.spacing.md))
        cartError?.let { Text(it, Modifier.padding(IngressosTheme.spacing.md), color = MaterialTheme.colorScheme.error) }
        Box(Modifier.weight(1f)) {
            EventsScreen(state, onEventClick, viewModel::load, adding)
        }
    }
}

@Composable
fun EventsScreen(
    state: EventsUiState,
    onEventClick: (String, String, Long) -> Unit,
    onRetry: () -> Unit,
    adding: Boolean = false,
) {
    when (state) {
        EventsUiState.Loading -> LoadingState()
        is EventsUiState.Error -> ErrorState(errorMessage(state.error), onRetry)
        is EventsUiState.Empty -> Column(Modifier.fillMaxSize()) {
            if (state.possiblyStale) StaleNotice(onRetry)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum evento disponível no momento.")
            }
        }
        is EventsUiState.Content -> LazyColumn(
            contentPadding = PaddingValues(IngressosTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.sm),
        ) {
            if (state.possiblyStale) item { StaleNotice(onRetry) }
            items(state.events, key = { it.id }) { event ->
                Card(
                    onClick = { onEventClick(event.id, event.name, event.priceCents) },
                    enabled = !adding,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        SubcomposeAsyncImage(
                            model = event.imageUrl,
                            contentDescription = "Imagem de ${event.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                        ) {
                            when (painter.state) {
                                is AsyncImagePainter.State.Loading, is AsyncImagePainter.State.Empty ->
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                is AsyncImagePainter.State.Error ->
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Imagem indisponível")
                                    }
                                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                            }
                        }
                        Column(Modifier.padding(IngressosTheme.spacing.md)) {
                            Text(event.name, style = MaterialTheme.typography.titleMedium)
                            Text(event.price, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StaleNotice(onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(IngressosTheme.spacing.md)) {
        Text("Não foi possível atualizar. Os eventos exibidos podem estar desatualizados.")
        TextButton(onClick = onRetry) { Text("Atualizar") }
    }
}

private fun errorMessage(error: EventsError): String = when (error) {
    EventsError.CONFIGURATION -> "Firebase não configurado. Contate o suporte."
    EventsError.UNAVAILABLE -> "Não foi possível atualizar os eventos. Verifique a conexão."
    EventsError.MISSING_DATA -> "A lista de eventos não está configurada."
    EventsError.INVALID_DATA -> "A lista de eventos recebida é inválida."
}
