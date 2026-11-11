package com.innaval.ingresosnamao.feature.payment.di

import com.innaval.ingresosnamao.feature.payment.data.PaymentApi
import com.innaval.ingresosnamao.feature.payment.data.PaymentRepositoryImpl
import com.innaval.ingresosnamao.feature.payment.domain.PayUseCase
import com.innaval.ingresosnamao.feature.payment.domain.PaymentRepository
import com.innaval.ingresosnamao.feature.payment.presentation.PaymentViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import retrofit2.Retrofit

val paymentModule = module {
    single { get<Retrofit>().create(PaymentApi::class.java) }
    single<PaymentRepository> { PaymentRepositoryImpl(get(), get()) }
    factoryOf(::PayUseCase)
    viewModelOf(::PaymentViewModel)
}
