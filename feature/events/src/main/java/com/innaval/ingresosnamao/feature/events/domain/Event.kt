package com.innaval.ingresosnamao.feature.events.domain

import com.innaval.ingresosnamao.core.common.result.AppResult

data class Event(val id: String, val name: String, val venue: String, val priceCents: Long)

interface EventRepository {
    suspend fun getEvents(): AppResult<List<Event>>
}

class GetEventsUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(): AppResult<List<Event>> = repository.getEvents()
}
