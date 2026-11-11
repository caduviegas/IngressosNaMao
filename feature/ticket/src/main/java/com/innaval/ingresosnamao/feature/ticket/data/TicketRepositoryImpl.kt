package com.innaval.ingresosnamao.feature.ticket.data

import androidx.room.withTransaction
import com.innaval.ingresosnamao.core.common.dispatchers.DispatcherProvider
import com.innaval.ingresosnamao.core.database.AppDatabase
import com.innaval.ingresosnamao.core.database.ticket.IssuedTicketDao
import com.innaval.ingresosnamao.core.database.ticket.IssuedTicketEntity
import com.innaval.ingresosnamao.core.database.ticket.TicketPurchaseEntity
import com.innaval.ingresosnamao.feature.ticket.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

private fun IssuedTicketEntity.toDomain() = Ticket(id, purchaseReference, eventId, eventName, unitPriceCents)
private fun TicketPurchaseEntity.toDomain() = PurchaseSummary(reference, orderId, totalCents, issuedAtMillis)

internal class TicketRepositoryImpl(
    private val database: AppDatabase,
    private val dao: IssuedTicketDao,
    private val dispatchers: DispatcherProvider,
) : TicketRepository {
    override suspend fun issue(purchase: SimulatedPurchase): TicketResult<Receipt> {
        if (purchase.reference.isBlank() || purchase.orderId.isBlank() || purchase.userId.isBlank() ||
            purchase.fingerprint.isBlank() || !validPurchaseItems(purchase.items, purchase.totalCents)
        ) return TicketResult.Failure(TicketError.INVALID_PURCHASE)
        return withContext(dispatchers.io) {
            try {
                database.withTransaction {
                    val existing = dao.findPurchase(purchase.reference)
                    if (existing != null) {
                        val tickets = dao.findTickets(purchase.reference)
                        val expected = purchase.items.flatMap { item ->
                            List(item.quantity) { Triple(item.eventId, item.eventName, item.unitPriceCents) }
                        }.groupingBy { it }.eachCount()
                        val actual = tickets.map { Triple(it.eventId, it.eventName, it.unitPriceCents) }
                            .groupingBy { it }.eachCount()
                        if (existing.orderId != purchase.orderId || existing.userId != purchase.userId ||
                            existing.fingerprint != purchase.fingerprint || existing.totalCents != purchase.totalCents ||
                            expected != actual
                        ) TicketResult.Failure(TicketError.CONFLICT)
                        else TicketResult.Success(Receipt(existing.toDomain(), tickets.map { it.toDomain() }))
                    } else if (dao.findPurchaseByOrder(purchase.orderId) != null) {
                        TicketResult.Failure(TicketError.CONFLICT)
                    } else {
                        val entity = TicketPurchaseEntity(
                            purchase.reference, purchase.orderId, purchase.userId, purchase.fingerprint,
                            purchase.totalCents, System.currentTimeMillis(),
                        )
                        val tickets = purchase.items.flatMap { item ->
                            List(item.quantity) {
                                IssuedTicketEntity(UUID.randomUUID().toString(), purchase.reference,
                                    item.eventId, item.eventName, item.unitPriceCents)
                            }
                        }
                        dao.insertPurchase(entity)
                        dao.insertTickets(tickets)
                        TicketResult.Success(Receipt(entity.toDomain(), tickets.map { it.toDomain() }))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                TicketResult.Failure(TicketError.STORAGE)
            }
        }
    }

    override fun observePurchases(userId: String): Flow<TicketResult<List<PurchaseSummary>>> {
        if (userId.isBlank()) return flowOf(TicketResult.Failure(TicketError.INVALID_PURCHASE))
        return dao.observePurchases(userId).map { TicketResult.Success(it.map { row -> row.toDomain() }) }
            .storageErrors()
    }

    override fun observeReceipt(userId: String, reference: String): Flow<TicketResult<Receipt?>> {
        if (userId.isBlank() || reference.isBlank()) return flowOf(TicketResult.Failure(TicketError.INVALID_PURCHASE))
        return combine(dao.observePurchase(userId, reference), dao.observeTickets(userId, reference)) { purchase, tickets ->
            TicketResult.Success(purchase?.let { Receipt(it.toDomain(), tickets.map { ticket -> ticket.toDomain() }) })
        }.storageErrors()
    }

    override fun observeTicket(userId: String, id: String): Flow<TicketResult<Ticket?>> {
        if (userId.isBlank() || id.isBlank()) return flowOf(TicketResult.Failure(TicketError.INVALID_PURCHASE))
        return dao.observeTicket(userId, id).map { TicketResult.Success(it?.toDomain()) }.storageErrors()
    }

    private fun <T> Flow<TicketResult<T>>.storageErrors(): Flow<TicketResult<T>> =
        catch { error ->
            if (error is CancellationException) throw error
            emit(TicketResult.Failure(TicketError.STORAGE))
        }
}
