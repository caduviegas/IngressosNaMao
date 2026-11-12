package com.innaval.ingresosnamao.feature.cart.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innaval.ingresosnamao.core.ui.components.ErrorState
import com.innaval.ingresosnamao.core.ui.components.IngressosButton
import com.innaval.ingresosnamao.core.ui.components.IngressosBackScreen
import com.innaval.ingresosnamao.core.ui.components.IngressosCard
import com.innaval.ingresosnamao.core.ui.components.LoadingState
import com.innaval.ingresosnamao.core.ui.format.formatPriceBrl
import com.innaval.ingresosnamao.core.ui.theme.IngressosTheme
import com.innaval.ingresosnamao.feature.cart.domain.CartError
import com.innaval.ingresosnamao.feature.cart.domain.CartItem
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CartRoute(
    userId: String,
    onCheckout: (totalCents: Long, fingerprint: String) -> Unit,
    onBack: () -> Unit,
    viewModel: CartViewModel = koinViewModel(key = userId, parameters = { parametersOf(userId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CartScreen(state, viewModel::changeQuantity, viewModel::remove, { viewModel.checkout(onCheckout) }, onBack, viewModel::retry)
}

@Composable
fun CartCleanupRoute(
    userId: String,
    fingerprint: String,
    onComplete: () -> Unit,
    viewModel: CartCleanupViewModel = koinViewModel(
        key = "cleanup:$userId:$fingerprint", parameters = { parametersOf(userId, fingerprint) }
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state == CartCleanupState.Complete) onComplete() }
    when (val current = state) {
        CartCleanupState.Loading, CartCleanupState.Complete -> LoadingState()
        is CartCleanupState.Error -> {
            if (current.error == CartError.CART_CHANGED) {
                Column(
                    Modifier.padding(IngressosTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.sm),
                ) {
                    Text("Retorno recebido. O carrinho mudou e não foi apagado; revise os itens antes de outra compra.")
                    IngressosButton("Continuar", onComplete)
                }
            } else {
                ErrorState(
                    "Retorno recebido, mas não foi possível limpar o carrinho. Tente novamente.",
                    viewModel::retry,
                )
            }
        }
    }
}

@Composable
fun CartScreen(
    state: CartUiState,
    onQuantity: (CartItem, Int) -> Unit,
    onRemove: (CartItem) -> Unit,
    onCheckout: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val cart = state.cart
    IngressosBackScreen("Carrinho", onBack, backEnabled = !state.busy) {
        when {
            state.loading -> LoadingState()
            cart == null -> ErrorState(cartErrorMessage(state.error ?: CartError.STORAGE), onRetry)
            else -> LazyColumn(
                contentPadding = PaddingValues(IngressosTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.sm),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.sm)) {
                        if (cart.items.isEmpty()) Text("Seu carrinho está vazio.")
                        state.error?.let { Text(cartErrorMessage(it), color = MaterialTheme.colorScheme.error) }
                    }
                }
                items(cart.items, key = { it.eventId }) { item ->
                    IngressosCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(IngressosTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.xs),
                        ) {
                            Text(item.eventName, style = MaterialTheme.typography.titleMedium)
                            Text("Unitário: ${formatPriceBrl(item.unitPriceCents)}")
                            Text("Subtotal: ${formatPriceBrl(item.unitPriceCents * item.quantity)}")
                            Row(horizontalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.xs)) {
                                TextButton(
                                    onClick = { onQuantity(item, -1) },
                                    enabled = !state.busy && item.quantity > 1,
                                    modifier = Modifier.semantics {
                                        contentDescription = "Diminuir quantidade de ${item.eventName}"
                                    },
                                ) { Text("−") }
                                Text("${item.quantity}", modifier = Modifier.padding(IngressosTheme.spacing.md))
                                TextButton(
                                    onClick = { onQuantity(item, 1) },
                                    enabled = !state.busy && item.quantity < Int.MAX_VALUE,
                                    modifier = Modifier.semantics {
                                        contentDescription = "Aumentar quantidade de ${item.eventName}"
                                    },
                                ) { Text("+") }
                                TextButton(onClick = { onRemove(item) }, enabled = !state.busy) { Text("Remover") }
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(IngressosTheme.spacing.sm)) {
                        Text("Total: ${formatPriceBrl(cart.totalCents)}", style = MaterialTheme.typography.titleMedium)
                        IngressosButton(
                            "Finalizar compra", onCheckout, enabled = cart.items.isNotEmpty() && !state.busy,
                        )
                    }
                }
            }
        }
    }
}

fun cartErrorMessage(error: CartError): String = when (error) {
    CartError.INVALID_ITEM -> "Os dados do carrinho são inválidos."
    CartError.NOT_FOUND -> "O item não está mais no carrinho."
    CartError.LIMIT_EXCEEDED -> "Quantidade ou total excede o limite permitido."
    CartError.FREE_CHECKOUT_UNSUPPORTED -> "A finalização de ingressos gratuitos ainda não é suportada."
    CartError.CART_CHANGED -> "O carrinho foi alterado."
    CartError.STORAGE -> "Não foi possível acessar o carrinho. Tente novamente."
    CartError.STOCK_UNAVAILABLE -> "Não foi possível atualizar a disponibilidade. Tente novamente."
    CartError.STOCK_EXCEEDED -> "Quantidade indisponível. Reduza ou remova o item."
    CartError.PRODUCT_CHANGED -> "O preço do evento mudou. Remova o item e adicione novamente."
}
