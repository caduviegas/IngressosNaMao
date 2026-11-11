package com.innaval.ingresosnamao.feature.ticket.di

import com.innaval.ingresosnamao.feature.ticket.data.TicketRepositoryImpl
import com.innaval.ingresosnamao.feature.ticket.domain.ObserveTicketsUseCase
import com.innaval.ingresosnamao.feature.ticket.domain.TicketRepository
import com.innaval.ingresosnamao.feature.ticket.presentation.TicketsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val ticketModule = module {
    single<TicketRepository> { TicketRepositoryImpl(get()) }
    factoryOf(::ObserveTicketsUseCase)
    viewModelOf(::TicketsViewModel)
}
