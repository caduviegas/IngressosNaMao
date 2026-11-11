package com.innaval.ingresosnamao.feature.ticket.domain

import kotlinx.coroutines.flow.Flow

const val MAX_TICKETS_PER_PURCHASE = 100

data class PurchaseItem(val eventId: String, val eventName: String, val unitPriceCents: Long, val quantity: Int)
data class SimulatedPurchase(
    val reference: String,
    val orderId: String,
    val userId: String,
    val fingerprint: String,
    val totalCents: Long,
    val items: List<PurchaseItem>,
)
data class Ticket(
    val id: String,
    val purchaseReference: String,
    val eventId: String,
    val eventName: String,
    val unitPriceCents: Long,
)
data class PurchaseSummary(
    val reference: String,
    val orderId: String,
    val totalCents: Long,
    val issuedAtMillis: Long,
)
data class Receipt(val purchase: PurchaseSummary, val tickets: List<Ticket>)

enum class TicketError { INVALID_PURCHASE, CONFLICT, STORAGE }
sealed interface TicketResult<out T> {
    data class Success<T>(val value: T) : TicketResult<T>
    data class Failure(val error: TicketError) : TicketResult<Nothing>
}

fun validPurchaseItems(items: List<PurchaseItem>, totalCents: Long): Boolean {
    if (items.isEmpty() || items.any {
            it.eventId.isBlank() || it.eventName.isBlank() || it.quantity <= 0 || it.unitPriceCents < 0
        } || items.map { it.eventId }.distinct().size != items.size
    ) return false
    return try {
        val count = items.fold(0L) { total, item -> Math.addExact(total, item.quantity.toLong()) }
        val total = items.fold(0L) { sum, item ->
            Math.addExact(sum, Math.multiplyExact(item.unitPriceCents, item.quantity.toLong()))
        }
        count <= MAX_TICKETS_PER_PURCHASE && total > 0 && total == totalCents
    } catch (_: ArithmeticException) {
        false
    }
}

interface TicketRepository {
    suspend fun issue(purchase: SimulatedPurchase): TicketResult<Receipt>
    fun observePurchases(userId: String): Flow<TicketResult<List<PurchaseSummary>>>
    fun observeReceipt(userId: String, reference: String): Flow<TicketResult<Receipt?>>
    fun observeTicket(userId: String, id: String): Flow<TicketResult<Ticket?>>
}

class IssueTicketsUseCase(private val repository: TicketRepository) {
    suspend operator fun invoke(purchase: SimulatedPurchase): TicketResult<Receipt> {
        if (purchase.reference.isBlank() || purchase.orderId.isBlank() || purchase.userId.isBlank() ||
            purchase.fingerprint.isBlank() || !validPurchaseItems(purchase.items, purchase.totalCents)
        ) return TicketResult.Failure(TicketError.INVALID_PURCHASE)
        return repository.issue(purchase)
    }
}
class ObservePurchasesUseCase(private val repository: TicketRepository) {
    operator fun invoke(userId: String) = repository.observePurchases(userId)
}
class ObserveReceiptUseCase(private val repository: TicketRepository) {
    operator fun invoke(userId: String, reference: String) = repository.observeReceipt(userId, reference)
}
class ObserveTicketUseCase(private val repository: TicketRepository) {
    operator fun invoke(userId: String, id: String) = repository.observeTicket(userId, id)
}
