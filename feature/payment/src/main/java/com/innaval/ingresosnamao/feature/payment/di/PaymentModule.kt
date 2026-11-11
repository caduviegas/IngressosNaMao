package com.innaval.ingresosnamao.feature.payment.di

import com.innaval.ingresosnamao.feature.payment.data.CieloGateway
import com.innaval.ingresosnamao.feature.payment.data.PaymentRepositoryImpl
import com.innaval.ingresosnamao.feature.payment.domain.PaymentGateway
import com.innaval.ingresosnamao.feature.payment.domain.StartPaymentUseCase
import com.innaval.ingresosnamao.feature.payment.domain.PaymentRepository
import com.innaval.ingresosnamao.feature.payment.presentation.PaymentViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

val paymentModule = module {
    single<PaymentGateway> { CieloGateway(androidContext()) }
    single<PaymentRepository> { PaymentRepositoryImpl(androidContext(), get()) }
    factoryOf(::StartPaymentUseCase)
    viewModelOf(::PaymentViewModel)
}
