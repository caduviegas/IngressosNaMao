package com.innaval.ingresosnamao.feature.cart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innaval.ingresosnamao.feature.cart.domain.CartError
import com.innaval.ingresosnamao.feature.cart.domain.CartResult
import com.innaval.ingresosnamao.feature.cart.domain.ClearCartUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CartCleanupState {
    data object Loading : CartCleanupState
    data object Complete : CartCleanupState
    data class Error(val error: CartError) : CartCleanupState
}

class CartCleanupViewModel(
    private val userId: String,
    private val fingerprint: String,
    private val clearCart: ClearCartUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<CartCleanupState>(CartCleanupState.Loading)
    val state = mutableState.asStateFlow()

    init { clear() }

    fun retry() {
        if (state.value is CartCleanupState.Error) {
            mutableState.value = CartCleanupState.Loading
            clear()
        }
    }

    private fun clear() {
        viewModelScope.launch {
            mutableState.value = when (val result = clearCart(userId, fingerprint)) {
                is CartResult.Success -> CartCleanupState.Complete
                is CartResult.Failure -> CartCleanupState.Error(result.error)
            }
        }
    }
}
