package com.innaval.ingresosnamao.feature.events.domain

data class Event(val id: String, val imageUrl: String, val name: String, val priceCents: Long)
data class EventList(val events: List<Event>, val possiblyStale: Boolean)

enum class EventsError { CONFIGURATION, UNAVAILABLE, MISSING_DATA, INVALID_DATA }

sealed interface EventsResult {
    data class Success(val data: EventList) : EventsResult
    data class Failure(val error: EventsError) : EventsResult
}

interface EventRepository {
    suspend fun getEvents(): EventsResult
}

class GetEventsUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(): EventsResult = repository.getEvents()
}
