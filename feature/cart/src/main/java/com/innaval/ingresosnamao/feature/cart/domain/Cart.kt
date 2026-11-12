package com.innaval.ingresosnamao.feature.cart.domain

import kotlinx.coroutines.flow.Flow
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest

data class CartItem(val eventId: String, val eventName: String, val unitPriceCents: Long, val quantity: Int)
data class CartSnapshot(val items: List<CartItem>, val totalCents: Long)

enum class CartError { INVALID_ITEM, NOT_FOUND, LIMIT_EXCEEDED, FREE_CHECKOUT_UNSUPPORTED, CART_CHANGED, STORAGE }

sealed interface CartResult<out T> {
    data class Success<T>(val value: T) : CartResult<T>
    data class Failure(val error: CartError) : CartResult<Nothing>
}

fun calculateCart(items: List<CartItem>): CartResult<CartSnapshot> {
    if (items.any { it.eventId.isBlank() || it.eventName.isBlank() || it.quantity <= 0 || it.unitPriceCents < 0 } ||
        items.map { it.eventId }.toSet().size != items.size
    ) return CartResult.Failure(CartError.INVALID_ITEM)
    return try {
        val total = items.fold(0L) { sum, item ->
            Math.addExact(sum, Math.multiplyExact(item.unitPriceCents, item.quantity.toLong()))
        }
        CartResult.Success(CartSnapshot(items, total))
    } catch (e: ArithmeticException) {
        CartResult.Failure(CartError.LIMIT_EXCEEDED)
    }
}

fun cartFingerprint(cart: CartSnapshot): String {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { output ->
        cart.items.sortedBy { it.eventId }.forEach { item ->
            listOf(item.eventId, item.eventName).forEach {
                val encoded = it.toByteArray(Charsets.UTF_8)
                output.writeInt(encoded.size)
                output.write(encoded)
            }
            output.writeLong(item.unitPriceCents)
            output.writeInt(item.quantity)
        }
    }
    return MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

interface CartRepository {
    fun observe(userId: String): Flow<CartResult<CartSnapshot>>
    suspend fun get(userId: String): CartResult<CartSnapshot>
    suspend fun add(userId: String, item: CartItem): CartResult<Unit>
    suspend fun setQuantity(userId: String, eventId: String, quantity: Int): CartResult<Unit>
    suspend fun remove(userId: String, eventId: String): CartResult<Unit>
    suspend fun clear(userId: String, expectedFingerprint: String): CartResult<Unit>
    suspend fun clearForLogout(userId: String): CartResult<Unit>
}

class ObserveCartUseCase(private val repository: CartRepository) {
    operator fun invoke(userId: String) = repository.observe(userId)
}
class GetCartUseCase(private val repository: CartRepository) {
    suspend operator fun invoke(userId: String) = repository.get(userId)
}
class AddToCartUseCase(private val repository: CartRepository) {
    suspend operator fun invoke(userId: String, item: CartItem) = repository.add(userId, item)
}
class SetCartQuantityUseCase(private val repository: CartRepository) {
    suspend operator fun invoke(userId: String, eventId: String, quantity: Int) =
        repository.setQuantity(userId, eventId, quantity)
}
class RemoveFromCartUseCase(private val repository: CartRepository) {
    suspend operator fun invoke(userId: String, eventId: String) = repository.remove(userId, eventId)
}
class ClearCartUseCase(private val repository: CartRepository) {
    suspend operator fun invoke(userId: String, expectedFingerprint: String) = repository.clear(userId, expectedFingerprint)
}
class ClearCartForLogoutUseCase(private val repository: CartRepository) {
    suspend operator fun invoke(userId: String) = repository.clearForLogout(userId)
}
