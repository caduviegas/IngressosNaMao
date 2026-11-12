package com.innaval.ingresosnamao.feature.events.data

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.innaval.ingresosnamao.core.common.dispatchers.DispatcherProvider
import com.innaval.ingresosnamao.feature.events.domain.Event
import com.innaval.ingresosnamao.feature.events.domain.EventList
import com.innaval.ingresosnamao.feature.events.domain.EventRepository
import com.innaval.ingresosnamao.feature.events.domain.EventsError
import com.innaval.ingresosnamao.feature.events.domain.EventsResult
import java.math.BigDecimal
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val EVENTS_KEY = "IngressosDisponiveis"

internal class EventsRemoteDataSource {
    private val config: FirebaseRemoteConfig get() = FirebaseRemoteConfig.getInstance()

    suspend fun fetchAndActivate() {
        config.fetchAndActivate().await()
    }

    suspend fun fetchFreshAndActivate() {
        config.fetch(0).await()
        config.activate().await()
    }

    fun activatedEvents(): String? {
        val value = config.getValue(EVENTS_KEY)
        return if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) value.asString() else null
    }
}

internal class EventsJsonParser {
    fun parse(value: String): List<Event> {
        val array = Json.parseToJsonElement(value) as? JsonArray
            ?: throw IllegalArgumentException("Expected an array")
        val ids = mutableSetOf<String>()
        return array.map { element ->
            val obj = element as? JsonObject ?: throw IllegalArgumentException("Expected an event")
            fun requiredString(key: String): String {
                val primitive = obj[key] as? JsonPrimitive
                    ?: throw IllegalArgumentException("Missing $key")
                require(primitive.isString)
                return primitive.content.trim().also { require(it.isNotEmpty()) }
            }
            val sku = obj["sku"] as? JsonPrimitive ?: throw IllegalArgumentException("Missing sku")
            require(!sku.isString)
            val id = sku.content.toLongOrNull()?.takeIf { it >= 0 }?.toString()
                ?: throw IllegalArgumentException("Invalid sku")
            require(ids.add(id))
            val imageUrl = requiredString("imageUrl")
            val uri = URI.create(imageUrl)
            require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank() && uri.userInfo == null)
            val name = requiredString("name")
            val price = obj["price"] as? JsonPrimitive ?: throw IllegalArgumentException("Missing price")
            val decimal = price.content.toBigDecimalOrNull() ?: throw IllegalArgumentException("Invalid price")
            require(decimal >= BigDecimal.ZERO)
            val priceCents = decimal.movePointRight(2).longValueExact()
            val quantity = obj["quantity"] as? JsonPrimitive ?: throw IllegalArgumentException("Missing quantity")
            require(!quantity.isString)
            val available = quantity.content.toIntOrNull()?.takeIf { it >= 0 }
                ?: throw IllegalArgumentException("Invalid quantity")
            Event(id, imageUrl, name, priceCents, available)
        }
    }
}

internal class EventRepositoryImpl(
    private val source: EventsRemoteDataSource,
    private val parser: EventsJsonParser,
    private val dispatchers: DispatcherProvider,
) : EventRepository {
    override suspend fun getEvents(): EventsResult = load(fresh = false)
    override suspend fun getFreshEvents(): EventsResult = load(fresh = true)

    private suspend fun load(fresh: Boolean): EventsResult = withContext(dispatchers.io) {
        val fetchFailed = try {
            if (fresh) source.fetchFreshAndActivate() else source.fetchAndActivate()
            false
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            return@withContext EventsResult.Failure(EventsError.CONFIGURATION)
        } catch (e: Exception) {
            true
        }
        val value = try {
            source.activatedEvents()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            return@withContext EventsResult.Failure(EventsError.CONFIGURATION)
        } catch (e: Exception) {
            return@withContext EventsResult.Failure(EventsError.UNAVAILABLE)
        }
        if (value == null) {
            return@withContext EventsResult.Failure(if (fetchFailed) EventsError.UNAVAILABLE else EventsError.MISSING_DATA)
        }
        try {
            EventsResult.Success(EventList(parser.parse(value), possiblyStale = fetchFailed))
        } catch (e: SerializationException) {
            EventsResult.Failure(EventsError.INVALID_DATA)
        } catch (e: IllegalArgumentException) {
            EventsResult.Failure(EventsError.INVALID_DATA)
        } catch (e: ArithmeticException) {
            EventsResult.Failure(EventsError.INVALID_DATA)
        }
    }
}
