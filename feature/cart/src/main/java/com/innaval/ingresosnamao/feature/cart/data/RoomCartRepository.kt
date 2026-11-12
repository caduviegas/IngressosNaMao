package com.innaval.ingresosnamao.feature.cart.data

import androidx.room.withTransaction
import com.innaval.ingresosnamao.core.common.dispatchers.DispatcherProvider
import com.innaval.ingresosnamao.core.database.AppDatabase
import com.innaval.ingresosnamao.core.database.cart.CartDao
import com.innaval.ingresosnamao.core.database.cart.CartEntity
import com.innaval.ingresosnamao.feature.cart.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class CartLocalDataSource(private val database: AppDatabase, private val dao: CartDao) {
    fun observe(userId: String): Flow<List<CartEntity>> = dao.observe(userId)
    suspend fun get(userId: String): List<CartEntity> = dao.getAll(userId)

    suspend fun add(userId: String, item: CartItem): CartResult<Unit> = database.withTransaction {
        val existing = dao.getAll(userId).map(CartEntity::toDomain)
        val current = existing.find { it.eventId == item.eventId }
        val quantity = if (current == null) 1 else {
            if (current.quantity == Int.MAX_VALUE) return@withTransaction CartResult.Failure(CartError.LIMIT_EXCEEDED)
            current.quantity + 1
        }
        val replacement = item.copy(quantity = quantity)
        val updated = existing.filterNot { it.eventId == item.eventId } + replacement
        when (val checked = calculateCart(updated)) {
            is CartResult.Failure -> checked
            is CartResult.Success -> {
                dao.upsert(replacement.toEntity(userId))
                CartResult.Success(Unit)
            }
        }
    }

    suspend fun setQuantity(userId: String, eventId: String, quantity: Int): CartResult<Unit> = database.withTransaction {
        val existing = dao.getAll(userId).map(CartEntity::toDomain)
        val current = existing.find { it.eventId == eventId }
            ?: return@withTransaction CartResult.Failure(CartError.NOT_FOUND)
        val replacement = current.copy(quantity = quantity)
        val updated = existing.map { if (it.eventId == eventId) replacement else it }
        when (val checked = calculateCart(updated)) {
            is CartResult.Failure -> checked
            is CartResult.Success -> {
                dao.upsert(replacement.toEntity(userId))
                CartResult.Success(Unit)
            }
        }
    }

    suspend fun remove(userId: String, eventId: String): CartResult<Unit> =
        if (dao.remove(userId, eventId) == 0) CartResult.Failure(CartError.NOT_FOUND) else CartResult.Success(Unit)

    suspend fun clear(userId: String, expectedFingerprint: String): CartResult<Unit> = database.withTransaction {
        when (val cart = calculateCart(dao.getAll(userId).map(CartEntity::toDomain))) {
            is CartResult.Failure -> cart
            is CartResult.Success -> {
                if (cart.value.items.isNotEmpty() && cartFingerprint(cart.value) != expectedFingerprint) {
                    CartResult.Failure(CartError.CART_CHANGED)
                } else {
                    dao.clear(userId)
                    CartResult.Success(Unit)
                }
            }
        }
    }
}

private fun CartEntity.toDomain() = CartItem(eventId, eventName, unitPriceCents, quantity)
private fun CartItem.toEntity(userId: String) = CartEntity(userId, eventId, eventName, unitPriceCents, quantity)

internal class RoomCartRepository(
    private val source: CartLocalDataSource,
    private val dispatchers: DispatcherProvider,
) : CartRepository {
    override fun observe(userId: String): Flow<CartResult<CartSnapshot>> {
        if (userId.isBlank()) return flowOf(CartResult.Failure(CartError.INVALID_ITEM))
        return flow { emitAll(source.observe(userId)) }
            .map { calculateCart(it.map(CartEntity::toDomain)) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(CartResult.Failure(CartError.STORAGE))
            }
            .flowOn(dispatchers.io)
    }

    override suspend fun get(userId: String): CartResult<CartSnapshot> = guarded(userId) {
        calculateCart(source.get(userId).map(CartEntity::toDomain))
    }

    override suspend fun add(userId: String, item: CartItem): CartResult<Unit> = guarded(userId) {
        if (item.quantity != 1 || calculateCart(listOf(item)) is CartResult.Failure) {
            CartResult.Failure(CartError.INVALID_ITEM)
        } else source.add(userId, item)
    }

    override suspend fun setQuantity(userId: String, eventId: String, quantity: Int): CartResult<Unit> = guarded(userId) {
        if (eventId.isBlank() || quantity <= 0) CartResult.Failure(CartError.INVALID_ITEM)
        else source.setQuantity(userId, eventId, quantity)
    }

    override suspend fun remove(userId: String, eventId: String): CartResult<Unit> = guarded(userId) {
        if (eventId.isBlank()) CartResult.Failure(CartError.INVALID_ITEM) else source.remove(userId, eventId)
    }

    override suspend fun clear(userId: String, expectedFingerprint: String): CartResult<Unit> = guarded(userId) {
        if (!expectedFingerprint.matches(Regex("[0-9a-f]{64}"))) CartResult.Failure(CartError.INVALID_ITEM)
        else source.clear(userId, expectedFingerprint)
    }

    private suspend fun <T> guarded(userId: String, block: suspend () -> CartResult<T>): CartResult<T> {
        if (userId.isBlank()) return CartResult.Failure(CartError.INVALID_ITEM)
        return withContext(dispatchers.io) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CartResult.Failure(CartError.STORAGE)
            }
        }
    }
}
