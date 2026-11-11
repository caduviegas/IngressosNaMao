package com.innaval.ingresosnamao.feature.events.data

import com.innaval.ingresosnamao.core.common.dispatchers.DispatcherProvider
import com.innaval.ingresosnamao.core.common.result.AppResult
import com.innaval.ingresosnamao.core.common.result.map
import com.innaval.ingresosnamao.core.common.result.runCatchingApp
import com.innaval.ingresosnamao.feature.events.domain.Event
import com.innaval.ingresosnamao.feature.events.domain.EventRepository
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
internal data class EventDto(val id: String, val name: String, val venue: String, val priceCents: Long)

internal interface EventsApi {
    @GET("events")
    suspend fun getEvents(): List<EventDto>
}

internal fun EventDto.toDomain() = Event(id, name, venue, priceCents)

internal class EventRepositoryImpl(
    private val api: EventsApi,
    private val dispatchers: DispatcherProvider,
) : EventRepository {
    override suspend fun getEvents(): AppResult<List<Event>> = withContext(dispatchers.io) {
        runCatchingApp { api.getEvents() }.map { list -> list.map { it.toDomain() } }
    }
}
