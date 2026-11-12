package com.innaval.ingresosnamao.feature.payment.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
            is PaymentState.Active -> {
                val attempt = current.attempt
                val sameOrder = attempt.order.userId == order.userId && attempt.order.fingerprint == order.fingerprint
                if (!sameOrder) {
                    Text("Há outra tentativa neste dispositivo. Resolva-a antes de iniciar outro pagamento.")
                } else when (attempt.status) {
                    AttemptStatus.WAITING -> Text("Aguardando retorno do emulador. Não inicie outro pagamento.")
                    AttemptStatus.UNKNOWN -> Text("Resultado desconhecido. Concilie a tentativa antes de outro pagamento.")
                    AttemptStatus.CANCELLED -> Text("Cancelamento recebido sem identificação verificável. A tentativa permanece bloqueada.")
                    AttemptStatus.ERROR -> Text("Erro recebido sem identificação verificável. A tentativa permanece bloqueada.")
                    AttemptStatus.APPROVED -> {
                        Text("Pagamento de teste aprovado. Este resultado não é válido para produção.")
                        IngressosButton("Continuar", onPaid)
                    }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(onClick = onBack, enabled = !validating) { Text("Voltar") }
    }
}
