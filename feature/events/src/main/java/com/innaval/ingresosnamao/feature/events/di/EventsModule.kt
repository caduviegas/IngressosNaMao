package com.innaval.ingresosnamao.feature.events.di

import com.innaval.ingresosnamao.feature.events.data.EventRepositoryImpl
import com.innaval.ingresosnamao.feature.events.data.EventsJsonParser
import com.innaval.ingresosnamao.feature.events.data.EventsRemoteDataSource
import com.innaval.ingresosnamao.feature.events.domain.EventRepository
import com.innaval.ingresosnamao.feature.events.domain.GetEventsUseCase
import com.innaval.ingresosnamao.feature.events.presentation.EventsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun eventsModule() = module {
    single { EventsRemoteDataSource() }
    single { EventsJsonParser() }
    single<EventRepository> { EventRepositoryImpl(get(), get(), get()) }
    factoryOf(::GetEventsUseCase)
    viewModelOf(::EventsViewModel)
}
