package com.innaval.ingresosnamao.di

import com.innaval.ingresosnamao.feature.cart.domain.AvailableEvent
import com.innaval.ingresosnamao.feature.cart.domain.CartAvailability
import com.innaval.ingresosnamao.feature.cart.domain.CartError
import com.innaval.ingresosnamao.feature.cart.domain.CartResult
import com.innaval.ingresosnamao.feature.events.domain.EventsResult
import com.innaval.ingresosnamao.feature.events.domain.GetEventsUseCase
import org.koin.dsl.module

internal class CartAvailabilityAdapter(private val events: GetEventsUseCase) : CartAvailability {
    override suspend fun load(): CartResult<List<AvailableEvent>> = when (val result = events.fresh()) {
        is EventsResult.Failure -> CartResult.Failure(CartError.STOCK_UNAVAILABLE)
        is EventsResult.Success -> if (result.data.possiblyStale) {
            CartResult.Failure(CartError.STOCK_UNAVAILABLE)
        } else {
            CartResult.Success(result.data.events.map { AvailableEvent(it.id, it.priceCents, it.quantity) })
        }
    }
}

internal fun cartAvailabilityModule() = module {
    single<CartAvailability> { CartAvailabilityAdapter(get()) }
}
