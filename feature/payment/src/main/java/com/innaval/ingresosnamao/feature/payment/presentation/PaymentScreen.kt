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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.innaval.ingresosnamao.core.ui.components.IngressosButton
import com.innaval.ingresosnamao.core.ui.components.IngressosTextField
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
    CIELO_AUTH_ERROR -> "Cielo: erro de autenticação (código 4). Verifique as credenciais de teste."
    null -> "O código de erro não foi registrado nesta tentativa."
    else -> "Cielo: erro não reconhecido (código $code)."
}

@Composable
fun PaymentRoute(
    order: PaymentOrder,
    enabled: Boolean,
    onPaid: () -> Unit,
    onBack: () -> Unit,
    viewModel: PaymentViewModel = koinViewModel(),
    validate: suspend () -> String?,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val validating by viewModel.validating.collectAsStateWithLifecycle()
    BackHandler(enabled = validating) {}
    var clientId by remember { mutableStateOf("") }
    var accessToken by remember { mutableStateOf("") }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(IngressosTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.md),
    ) {
        Text("Pagamento de teste", style = MaterialTheme.typography.headlineSmall)
        Text("Total: ${formatPriceBrl(order.totalCents)}")
        Text("Simulação no emulador Cielo. Não utilize credenciais de produção.")
        if (!enabled) {
            Text("O pagamento está desabilitado nesta versão.")
        } else when (val current = state) {
            PaymentState.Ready -> {
                IngressosTextField(clientId, { clientId = it }, label = "Client ID de teste", singleLine = true)
                IngressosTextField(
                    accessToken, { accessToken = it }, label = "Access token de teste",
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                )
                IngressosButton("Abrir emulador", {
                    viewModel.submitValidated(order, TestCredentials(clientId, accessToken), validate)
                    clientId = ""
                    accessToken = ""
                }, enabled = !validating, loading = validating)
            }
            is PaymentState.StorageError -> Text(current.message, color = MaterialTheme.colorScheme.error)
            is PaymentState.Active -> ActivePayment(current.attempt, order, onPaid)
        }
        if (enabled) SimulationRecoveryAction(state, viewModel, showError = false)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(onClick = onBack, enabled = !validating) { Text("Voltar") }
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
        AttemptStatus.WAITING -> Text("Aguardando retorno do emulador.")
        AttemptStatus.UNKNOWN -> Text("Resultado desconhecido nesta simulação.")
        AttemptStatus.CANCELLED -> Text("Simulação cancelada.")
        AttemptStatus.ERROR -> {
            Text("A simulação retornou um erro.")
            Text(cieloErrorMessage(attempt.errorCode))
        }
        AttemptStatus.APPROVED -> {
            if (sameOrder) {
                Text("Pagamento de teste aprovado. Este resultado não é válido para produção.")
                IngressosButton("Continuar", onPaid)
            } else {
                Text("Pagamento de teste aprovado. Conclua-o pela tela de eventos.")
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
        TextButton(onClick = { discardState = state }) { Text("Descartar tentativa de teste") }
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
        title = { Text("Descartar tentativa de teste?") },
        text = {
            Text(
                "Esta ação remove o registro da simulação, inclusive se estiver pendente ou aprovada. " +
                    "Ela não cancela uma transação e não deve ser usada em pagamentos reais."
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
