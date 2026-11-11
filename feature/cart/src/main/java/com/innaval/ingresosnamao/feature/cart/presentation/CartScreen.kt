package com.innaval.ingresosnamao.feature.cart.presentation

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
import com.innaval.ingresosnamao.core.ui.components.IngressosButton
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.cart.domain.CartItem
import com.innaval.ingresosnamao.feature.cart.domain.ObserveCartUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.compose.viewmodel.koinViewModel

data class CartUiState(val items: List<CartItem> = emptyList()) {
    val totalCents: Long get() = items.sumOf { it.unitPriceCents * it.quantity }
}

class CartViewModel(observeCart: ObserveCartUseCase) : ViewModel() {
    val state: StateFlow<CartUiState> = observeCart()
        .map(::CartUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())
}

/** [onCheckout] receives the total in cents; :app maps it to the payment destination. */
@Composable
fun CartRoute(onCheckout: (totalCents: Long) -> Unit, viewModel: CartViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CartScreen(state) { onCheckout(state.totalCents) }
}

@Composable
fun CartScreen(state: CartUiState, onCheckout: () -> Unit) {
    Column(Modifier.padding(IngressosTheme.spacing.md)) {
        Text("Carrinho", style = MaterialTheme.typography.headlineSmall)
        state.items.forEach { Text("${it.quantity}x ${it.eventName}") }
        IngressosButton("Finalizar compra", onCheckout, enabled = state.items.isNotEmpty())
    }
}
