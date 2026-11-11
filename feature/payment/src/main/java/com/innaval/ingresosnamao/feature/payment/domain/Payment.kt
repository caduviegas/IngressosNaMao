package com.innaval.ingresosnamao.feature.payment.domain

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class PaymentItem(val sku: String, val name: String, val quantity: Int, val unitPriceCents: Long)
@Serializable
data class PaymentOrder(
    val userId: String,
    val fingerprint: String,
    val items: List<PaymentItem>,
    val totalCents: Long,
)
data class TestCredentials(val clientId: String, val accessToken: String)

@Serializable
enum class AttemptStatus { WAITING, UNKNOWN, APPROVED, CANCELLED, ERROR }
@Serializable
data class PaymentAttempt(
    val reference: String,
    val order: PaymentOrder,
    val startedAtMillis: Long,
    val status: AttemptStatus,
    val orderId: String? = null,
)

sealed interface PaymentResult {
    data class Approved(val orderId: String) : PaymentResult
    data object Denied : PaymentResult
    data object Cancelled : PaymentResult
    data object Unknown : PaymentResult
    data class Error(val message: String) : PaymentResult
}

sealed interface PaymentState {
    data object Ready : PaymentState
    data class Active(val attempt: PaymentAttempt) : PaymentState
    data class StorageError(val message: String) : PaymentState
}

sealed interface StartResult {
    data object Started : StartResult
    data class Failure(val message: String) : StartResult
}

interface PaymentGateway {
    fun launch(attempt: PaymentAttempt, credentials: TestCredentials): StartResult
}

interface PaymentRepository {
    val state: StateFlow<PaymentState>
    fun start(order: PaymentOrder, credentials: TestCredentials): StartResult
    fun acceptCallback(encodedResponse: String?)
    fun expire()
    fun acknowledgeApproved(userId: String, fingerprint: String): Boolean
}

class StartPaymentUseCase(private val repository: PaymentRepository) {
    operator fun invoke(order: PaymentOrder, credentials: TestCredentials) = repository.start(order, credentials)
}

fun validOrder(order: PaymentOrder): Boolean {
    if (order.userId.isBlank() || order.fingerprint.isBlank() || order.items.isEmpty() ||
        order.items.any { it.sku.isBlank() || it.name.isBlank() || it.quantity <= 0 || it.unitPriceCents < 0 } ||
        order.items.map { it.sku }.distinct().size != order.items.size
    ) return false
    return try {
        order.totalCents > 0 && order.items.fold(0L) { total, item ->
            Math.addExact(total, Math.multiplyExact(item.unitPriceCents, item.quantity.toLong()))
        } == order.totalCents
    } catch (_: ArithmeticException) {
        false
    }
}
