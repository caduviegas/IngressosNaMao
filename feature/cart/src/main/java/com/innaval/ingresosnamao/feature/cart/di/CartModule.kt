package com.innaval.ingresosnamao.feature.cart.di

import com.innaval.ingresosnamao.feature.cart.data.InMemoryCartRepository
import com.innaval.ingresosnamao.feature.cart.domain.CartRepository
import com.innaval.ingresosnamao.feature.cart.domain.ObserveCartUseCase
import com.innaval.ingresosnamao.feature.cart.presentation.CartViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val cartModule = module {
    single<CartRepository> { InMemoryCartRepository() }
    factoryOf(::ObserveCartUseCase)
    viewModelOf(::CartViewModel)
}
