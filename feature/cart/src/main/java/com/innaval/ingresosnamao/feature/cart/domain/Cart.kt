package com.innaval.ingresosnamao.feature.cart.domain

import kotlinx.coroutines.flow.Flow

data class CartItem(val eventId: String, val eventName: String, val unitPriceCents: Long, val quantity: Int)

interface CartRepository {
    fun observeCart(): Flow<List<CartItem>>
    suspend fun add(item: CartItem)
    suspend fun clear()
}

class ObserveCartUseCase(private val repository: CartRepository) {
    operator fun invoke(): Flow<List<CartItem>> = repository.observeCart()
}
