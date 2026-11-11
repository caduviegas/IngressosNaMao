package com.innaval.ingresosnamao.feature.cart.data

import com.innaval.ingresosnamao.feature.cart.domain.CartItem
import com.innaval.ingresosnamao.feature.cart.domain.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory for now; swap for a Room-backed implementation without touching domain/presentation. */
internal class InMemoryCartRepository : CartRepository {
    private val items = MutableStateFlow<List<CartItem>>(emptyList())

    override fun observeCart(): Flow<List<CartItem>> = items
    override suspend fun add(item: CartItem) = items.update { it + item }
    override suspend fun clear() = items.update { emptyList() }
}
