package com.innaval.ingresosnamao.feature.ticket.domain

import kotlinx.coroutines.flow.Flow

data class Ticket(val id: String, val eventId: String, val eventName: String, val qrCode: String)

interface TicketRepository {
    fun observeTickets(): Flow<List<Ticket>>
}

class ObserveTicketsUseCase(private val repository: TicketRepository) {
    operator fun invoke(): Flow<List<Ticket>> = repository.observeTickets()
}
