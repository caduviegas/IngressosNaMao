package com.innaval.ingresosnamao.feature.ticket.di

import com.innaval.ingresosnamao.feature.ticket.data.TicketRepositoryImpl
import com.innaval.ingresosnamao.feature.ticket.data.ZxingQrCodeGenerator
import com.innaval.ingresosnamao.feature.ticket.domain.*
import com.innaval.ingresosnamao.feature.ticket.domain.TicketRepository
import com.innaval.ingresosnamao.feature.ticket.presentation.*
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun ticketModule() = module {
    single<TicketRepository> { TicketRepositoryImpl(get(), get(), get()) }
    single<QrCodeGenerator> { ZxingQrCodeGenerator(get()) }
    factoryOf(::IssueTicketsUseCase)
    factoryOf(::ObservePurchasesUseCase)
    factoryOf(::ObserveReceiptUseCase)
    factoryOf(::ObserveTicketUseCase)
    viewModel { (userId: String) -> TicketsViewModel(userId, get()) }
    viewModel { (userId: String, reference: String) -> ReceiptViewModel(userId, reference, get()) }
    viewModel { (userId: String, id: String) -> TicketDetailViewModel(userId, id, get(), get()) }
}
