package com.innaval.ingresosnamao.feature.payment.presentation

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
import com.innaval.ingresosnamao.core.common.result.AppResult
import com.innaval.ingresosnamao.core.ui.components.IngressosButton
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.payment.domain.PayUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

data class PaymentUiState(val loading: Boolean = false, val error: Boolean = false)

class PaymentViewModel(private val pay: PayUseCase) : ViewModel() {
    private val _state = MutableStateFlow(PaymentUiState())
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()

    fun submit(amountCents: Long, onPaid: () -> Unit) {
        _state.update { PaymentUiState(loading = true) }
        viewModelScope.launch {
            when (pay(amountCents)) {
                is AppResult.Success -> { _state.update { PaymentUiState() }; onPaid() }
                is AppResult.Failure -> _state.update { PaymentUiState(error = true) }
            }
        }
    }
}

@Composable
fun PaymentRoute(amountCents: Long, onPaid: () -> Unit, viewModel: PaymentViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PaymentScreen(amountCents, state) { viewModel.submit(amountCents, onPaid) }
}

@Composable
fun PaymentScreen(amountCents: Long, state: PaymentUiState, onPay: () -> Unit) {
    Column(Modifier.padding(IngressosTheme.spacing.md)) {
        Text("Pagamento", style = MaterialTheme.typography.headlineSmall)
        Text("Total: R$ %d,%02d".format(amountCents / 100, amountCents % 100))
        if (state.error) Text("Falha no pagamento")
        IngressosButton("Pagar", onPay, enabled = !state.loading)
    }
}
