package com.innaval.ingresosnamao.feature.events.di

import com.innaval.ingresosnamao.feature.events.data.EventRepositoryImpl
import com.innaval.ingresosnamao.feature.events.data.EventsApi
import com.innaval.ingresosnamao.feature.events.domain.EventRepository
import com.innaval.ingresosnamao.feature.events.domain.GetEventsUseCase
import com.innaval.ingresosnamao.feature.events.presentation.EventsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit

val eventsModule = module {
    single { get<Retrofit>().create(EventsApi::class.java) }
    single<EventRepository> { EventRepositoryImpl(get(), get()) }
    factoryOf(::GetEventsUseCase)
    viewModelOf(::EventsViewModel)
}
