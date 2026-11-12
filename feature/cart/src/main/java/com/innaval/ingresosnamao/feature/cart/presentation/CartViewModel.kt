package com.innaval.ingresosnamao.feature.cart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innaval.ingresosnamao.feature.cart.domain.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartUiState(
    val loading: Boolean = true,
    val cart: CartSnapshot? = null,
    val busy: Boolean = false,
    val error: CartError? = null,
)

class CartViewModel(
    private val userId: String,
    private val observeCart: ObserveCartUseCase,
    private val getCart: GetCartUseCase,
    private val setQuantity: SetCartQuantityUseCase,
    private val removeItem: RemoveFromCartUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CartUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init { retry() }

    fun retry() {
        observation?.cancel()
        mutableState.update { it.copy(loading = true, cart = null, error = null) }
        observation = viewModelScope.launch {
            observeCart(userId).collect { result ->
                mutableState.update {
                    when (result) {
                        is CartResult.Success -> it.copy(loading = false, cart = result.value, error = null)
                        is CartResult.Failure -> it.copy(loading = false, cart = null, error = result.error)
                    }
                }
            }
        }
    }

    fun changeQuantity(item: CartItem, delta: Int) {
        if (delta !in listOf(-1, 1)) return
        if ((delta == -1 && item.quantity <= 1) || (delta == 1 && item.quantity == Int.MAX_VALUE)) return
        mutate { setQuantity(userId, item.eventId, item.quantity + delta) }
    }

    fun remove(item: CartItem) = mutate { removeItem(userId, item.eventId) }

    fun checkout(onCheckout: (Long, String) -> Unit) {
        if (state.value.busy || state.value.loading || state.value.cart == null) return
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = getCart(userId)) {
                is CartResult.Failure -> mutableState.update { it.copy(busy = false, error = result.error) }
                is CartResult.Success -> {
                    val cart = result.value
                    if (cart.items.isNotEmpty() && cart.totalCents == 0L) {
                        mutableState.update { it.copy(busy = false, cart = cart, error = CartError.FREE_CHECKOUT_UNSUPPORTED) }
                    } else {
                        mutableState.update { it.copy(busy = false, cart = cart) }
                        if (cart.items.isNotEmpty()) onCheckout(cart.totalCents, cartFingerprint(cart))
                    }
                }
            }
        }
    }

    private fun mutate(block: suspend () -> CartResult<Unit>) {
        if (state.value.busy || state.value.loading || state.value.cart == null) return
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = block()) {
                is CartResult.Success -> when (val refreshed = getCart(userId)) {
                    is CartResult.Success -> mutableState.update { it.copy(busy = false, cart = refreshed.value) }
                    is CartResult.Failure -> mutableState.update { it.copy(busy = false, cart = null, error = refreshed.error) }
                }
                is CartResult.Failure -> mutableState.update { it.copy(busy = false, error = result.error) }
            }
        }
    }
}
