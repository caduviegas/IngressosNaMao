package com.innaval.ingresosnamao.feature.payment.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.innaval.ingresosnamao.core.ui.components.IngressosButton
import com.innaval.ingresosnamao.core.ui.components.IngressosBackScreen
import com.innaval.ingresosnamao.core.ui.format.formatPriceBrl
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.payment.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class PaymentViewModel(
    private val startPayment: StartPaymentUseCase,
    private val repository: PaymentRepository,
) : ViewModel() {
    val state = repository.state
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()
    private val mutableValidating = MutableStateFlow(false)
    val validating = mutableValidating.asStateFlow()
    private var automaticStartHandled = false

    init {
        repository.expire()
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                repository.expire()
            }
        }
    }

    fun submit(order: PaymentOrder, credentials: TestCredentials) {
        mutableError.value = when (val result = startPayment(order, credentials)) {
            StartResult.Started -> null
            is StartResult.Failure -> result.message
        }
    }

    fun submitValidated(order: PaymentOrder, credentials: TestCredentials, validate: suspend () -> String?) {
        if (mutableValidating.value) return
        mutableValidating.value = true
        mutableError.value = null
        viewModelScope.launch {
            try {
                val failure = validate()
                if (failure == null) submit(order, credentials) else mutableError.value = failure
            } finally {
                mutableValidating.value = false
            }
        }
    }

    fun startAutomatically(order: PaymentOrder, credentials: TestCredentials, validate: suspend () -> String?) {
        if (automaticStartHandled) return
        automaticStartHandled = true
        if (state.value == PaymentState.Ready) submitValidated(order, credentials, validate)
    }

    fun discardSimulation(expectedState: PaymentState) {
        mutableError.value = when (val result = repository.discardSimulation(expectedState)) {
            DiscardSimulationResult.Discarded -> null
            is DiscardSimulationResult.Failure -> result.message
        }
    }
}

private const val CIELO_GENERIC_ERROR = 2
private const val CIELO_PAYMENT_ERROR = 3
private const val CIELO_AUTH_ERROR = 4

private fun cieloErrorMessage(code: Int?): String = when (code) {
    CIELO_GENERIC_ERROR -> "Cielo: erro genérico (código 2)."
    CIELO_PAYMENT_ERROR -> "Cielo: erro no pagamento (código 3)."
    CIELO_AUTH_ERROR -> "Cielo: erro de autenticação (código 4). Verifique as credenciais configuradas."
    null -> "O código de erro não foi registrado nesta tentativa."
    else -> "Cielo: erro não reconhecido (código $code)."
}

@Composable
fun PaymentRoute(
    order: PaymentOrder,
    enabled: Boolean,
    credentials: TestCredentials,
    onPaid: () -> Unit,
    onBack: () -> Unit,
    viewModel: PaymentViewModel = koinViewModel(),
    validate: suspend () -> String?,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val validating by viewModel.validating.collectAsStateWithLifecycle()
    BackHandler(enabled = validating) {}
    LaunchedEffect(viewModel, order, enabled) {
        if (enabled) viewModel.startAutomatically(order, credentials, validate)
    }
    IngressosBackScreen("Pagamento", onBack, backEnabled = !validating) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(IngressosTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.md),
        ) {
            Text("Total: ${formatPriceBrl(order.totalCents)}")
            if (!enabled) {
                Text("O pagamento está desabilitado nesta versão.")
            } else when (val current = state) {
                PaymentState.Ready -> {
                    IngressosButton("Tentar novamente", {
                        viewModel.submitValidated(order, credentials, validate)
                    }, enabled = !validating, loading = validating)
                }
                is PaymentState.StorageError -> Text(current.message, color = MaterialTheme.colorScheme.error)
                is PaymentState.Active -> ActivePayment(current.attempt, order, onPaid)
            }
            if (enabled) SimulationRecoveryAction(state, viewModel, showError = false)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun ActivePayment(
    attempt: PaymentAttempt,
    order: PaymentOrder,
    onPaid: () -> Unit,
) {
    val sameUser = attempt.order.userId == order.userId
    val sameOrder = sameUser && attempt.order.fingerprint == order.fingerprint
    if (!sameOrder) {
        Text("Há outra tentativa neste dispositivo. Resolva-a antes de iniciar outro pagamento.")
    }
    if (sameUser) when (attempt.status) {
        AttemptStatus.WAITING -> Text("Aguardando retorno da Cielo.")
        AttemptStatus.UNKNOWN -> Text("Resultado desconhecido.")
        AttemptStatus.CANCELLED -> Text("Pagamento cancelado.")
        AttemptStatus.ERROR -> {
            Text("O pagamento retornou um erro.")
            Text(cieloErrorMessage(attempt.errorCode))
        }
        AttemptStatus.APPROVED -> {
            if (sameOrder) {
                Text("Retorno do pagamento recebido.")
                IngressosButton("Continuar", onPaid)
            } else {
                Text("Retorno recebido. Conclua o pedido pela tela de eventos.")
            }
        }
    }
}

@Composable
fun SimulationRecoveryAction(
    state: PaymentState,
    viewModel: PaymentViewModel = koinViewModel(),
    showError: Boolean = true,
) {
    var discardState by remember { mutableStateOf<PaymentState?>(null) }
    val error by viewModel.error.collectAsStateWithLifecycle()
    if (state != PaymentState.Ready) {
        TextButton(onClick = { discardState = state }) { Text("Descartar tentativa") }
        if (showError) error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    discardState?.let { expectedState ->
        DiscardConfirmation(
            onDismiss = { discardState = null },
            onConfirm = {
                discardState = null
                viewModel.discardSimulation(expectedState)
            },
        )
    }
}

@Composable
private fun DiscardConfirmation(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Descartar tentativa?") },
        text = {
            Text(
                "Esta ação remove o registro, inclusive se o pagamento estiver pendente ou aprovado. " +
                    "Ela não cancela uma transação."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Descartar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Não descartar") }
        },
    )
}
