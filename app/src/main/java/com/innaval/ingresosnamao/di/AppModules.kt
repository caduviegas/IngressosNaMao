package com.innaval.ingresosnamao.di

import com.innaval.ingresosnamao.BuildConfig
import com.innaval.ingresosnamao.core.common.di.commonModule
import com.innaval.ingresosnamao.core.database.di.databaseModule
import com.innaval.ingresosnamao.core.network.di.NetworkConfig
import com.innaval.ingresosnamao.core.network.di.networkModule
import com.innaval.ingresosnamao.feature.auth.di.authModule
import com.innaval.ingresosnamao.feature.cart.di.cartModule
import com.innaval.ingresosnamao.feature.events.di.eventsModule
import com.innaval.ingresosnamao.feature.payment.di.paymentModule
import com.innaval.ingresosnamao.feature.profile.di.profileModule
import com.innaval.ingresosnamao.feature.ticket.di.ticketModule
import org.koin.dsl.module

private fun configModule() = module {
    single { NetworkConfig(baseUrl = BuildConfig.API_BASE_URL, debug = BuildConfig.DEBUG) }
}

fun appModules() = listOf(
    configModule(),
    commonModule(), networkModule(), databaseModule(),
    authModule(), eventsModule(), cartModule(), paymentModule(), ticketModule(),
    cartAvailabilityModule(), profileModule(), profileIntegrationModule(),
)
