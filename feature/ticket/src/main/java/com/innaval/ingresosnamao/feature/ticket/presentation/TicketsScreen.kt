package com.innaval.ingresosnamao.feature.ticket.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.innaval.ingresosnamao.core.ui.format.formatPriceBrl
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.ticket.domain.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.DateFormat
import java.util.Date

sealed interface TicketUiState<out T> {
    data object Loading : TicketUiState<Nothing>
    data object Empty : TicketUiState<Nothing>
    data object Error : TicketUiState<Nothing>
    data class Content<T>(val value: T) : TicketUiState<T>
}

class TicketsViewModel(
    private val userId: String,
    private val observe: ObservePurchasesUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<TicketUiState<List<PurchaseSummary>>>(TicketUiState.Loading)
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    init { retry() }
    fun retry() {
        job?.cancel()
        mutableState.value = TicketUiState.Loading
        job = viewModelScope.launch {
            observe(userId).collect { result ->
                mutableState.value = when (result) {
                    is TicketResult.Failure -> TicketUiState.Error
                    is TicketResult.Success -> if (result.value.isEmpty()) TicketUiState.Empty else TicketUiState.Content(result.value)
                }
            }
        }
    }
}

class ReceiptViewModel(
    private val userId: String,
    private val reference: String,
    private val observe: ObserveReceiptUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<TicketUiState<Receipt>>(TicketUiState.Loading)
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    init { retry() }
    fun retry() {
        job?.cancel()
        mutableState.value = TicketUiState.Loading
        job = viewModelScope.launch {
            observe(userId, reference).collect { result ->
                mutableState.value = when (result) {
                    is TicketResult.Failure -> TicketUiState.Error
                    is TicketResult.Success -> result.value?.let { TicketUiState.Content(it) } ?: TicketUiState.Empty
                }
            }
        }
    }
}

data class TicketDetail(val ticket: Ticket, val qr: QrResult?)

class TicketDetailViewModel(
    private val userId: String,
    private val id: String,
    private val observe: ObserveTicketUseCase,
    private val generator: QrCodeGenerator,
) : ViewModel() {
    private val mutableState = MutableStateFlow<TicketUiState<TicketDetail>>(TicketUiState.Loading)
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    init { retry() }
    fun retry() {
        job?.cancel()
        mutableState.value = TicketUiState.Loading
        job = viewModelScope.launch {
            observe(userId, id).collect { result ->
                when (result) {
                    is TicketResult.Failure -> mutableState.value = TicketUiState.Error
                    is TicketResult.Success -> {
                        val ticket = result.value
                        if (ticket == null) mutableState.value = TicketUiState.Empty
                        else {
                            mutableState.value = TicketUiState.Content(TicketDetail(ticket, null))
                            mutableState.value = TicketUiState.Content(TicketDetail(ticket, generator.generate(ticket.id)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketsRoute(
    userId: String,
    onReceipt: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TicketsViewModel = koinViewModel(key = "tickets:$userId", parameters = { parametersOf(userId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.padding(IngressosTheme.spacing.md)) {
        Text("Meus ingressos de teste", style = MaterialTheme.typography.headlineSmall)
        Text("Simulação — sem validade de entrada.")
        when (val current = state) {
            TicketUiState.Loading -> Text("Carregando compras…")
            TicketUiState.Empty -> Text("Nenhuma compra de teste encontrada.")
            TicketUiState.Error -> TicketErrorMessage("Não foi possível carregar as compras.", viewModel::retry)
            is TicketUiState.Content -> LazyColumn(Modifier.weight(1f, fill = false)) {
                items(current.value, key = { it.reference }) { purchase ->
                    TextButton(onClick = { onReceipt(purchase.reference) }) {
                        Text("Pedido ${purchase.orderId} — ${formatPriceBrl(purchase.totalCents)}")
                    }
                }
            }
        }
        TextButton(onClick = onBack) { Text("Voltar") }
    }
}

@Composable
fun ReceiptRoute(
    userId: String,
    reference: String,
    onTicket: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ReceiptViewModel = koinViewModel(key = "receipt:$userId:$reference", parameters = { parametersOf(userId, reference) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.padding(IngressosTheme.spacing.md)) {
        Text("Comprovante de teste", style = MaterialTheme.typography.headlineSmall)
        Text("Simulação — não comprova pagamento real.")
        when (val current = state) {
            TicketUiState.Loading -> Text("Carregando comprovante…")
            TicketUiState.Empty -> Text("Compra não encontrada.")
            TicketUiState.Error -> TicketErrorMessage("Não foi possível carregar o comprovante.", viewModel::retry)
            is TicketUiState.Content -> {
                val receipt = current.value
                val quantities = receipt.tickets.groupingBy {
                    Triple(it.eventId, it.eventName, it.unitPriceCents)
                }.eachCount().toList()
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    item {
                        Text("Pedido: ${receipt.purchase.orderId}")
                        Text("Total: ${formatPriceBrl(receipt.purchase.totalCents)}")
                        Text("Emitido: ${DateFormat.getDateTimeInstance().format(Date(receipt.purchase.issuedAtMillis))}")
                        Text("Ingressos: ${receipt.tickets.size}")
                    }
                    items(quantities) { (item, quantity) ->
                        Text("${item.second}: $quantity × ${formatPriceBrl(item.third)}")
                    }
                    item { Text("Abrir ingresso") }
                    items(receipt.tickets, key = { it.id }) { ticket ->
                        TextButton(onClick = { onTicket(ticket.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${ticket.eventName} — ${formatPriceBrl(ticket.unitPriceCents)}")
                        }
                    }
                }
            }
        }
        TextButton(onClick = onBack) { Text("Voltar") }
    }
}

@Composable
fun TicketDetailRoute(
    userId: String,
    id: String,
    onBack: () -> Unit,
    viewModel: TicketDetailViewModel = koinViewModel(key = "ticket:$userId:$id", parameters = { parametersOf(userId, id) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.padding(IngressosTheme.spacing.md)) {
        Text("Detalhes do ingresso", style = MaterialTheme.typography.headlineSmall)
        Text("Simulação — sem validade de entrada.")
        when (val current = state) {
            TicketUiState.Loading -> Text("Carregando ingresso…")
            TicketUiState.Empty -> Text("Ingresso não encontrado.")
            TicketUiState.Error -> TicketErrorMessage("Não foi possível carregar o ingresso.", viewModel::retry)
            is TicketUiState.Content -> {
                val detail = current.value
                Text(detail.ticket.eventName)
                Text("Valor: ${formatPriceBrl(detail.ticket.unitPriceCents)}")
                Text("ID: ${detail.ticket.id}")
                when (val qr = detail.qr) {
                    null -> Text("Gerando QR Code…")
                    QrResult.Failure -> TicketErrorMessage("Não foi possível gerar o QR Code.", viewModel::retry)
                    is QrResult.Success -> {
                        val bitmap = remember(qr.pattern) {
                            Bitmap.createBitmap(qr.pattern.pixels, qr.pattern.size, qr.pattern.size, Bitmap.Config.ARGB_8888).asImageBitmap()
                        }

                        Image(bitmap, contentDescription = "QR Code do ingresso de teste", modifier = Modifier.size(256.dp))
                    }
                }
            }
        }
        TextButton(onClick = onBack) { Text("Voltar") }
    }
}

@Composable
private fun TicketErrorMessage(message: String, onRetry: () -> Unit) {
    Text(message, color = MaterialTheme.colorScheme.error)
    TextButton(onClick = onRetry) { Text("Tentar novamente") }
}
