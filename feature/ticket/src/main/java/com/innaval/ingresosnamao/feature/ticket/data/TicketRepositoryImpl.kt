package com.innaval.ingresosnamao.feature.ticket.data

import com.innaval.ingresosnamao.core.database.ticket.TicketDao
import com.innaval.ingresosnamao.core.database.ticket.TicketEntity
import com.innaval.ingresosnamao.feature.ticket.domain.Ticket
import com.innaval.ingresosnamao.feature.ticket.domain.TicketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal fun TicketEntity.toDomain() = Ticket(id, eventId, eventName, qrCode)

internal class TicketRepositoryImpl(private val dao: TicketDao) : TicketRepository {
    override fun observeTickets(): Flow<List<Ticket>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }
}
