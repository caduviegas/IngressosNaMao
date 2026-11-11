package com.innaval.ingresosnamao.feature.cart.di

import com.innaval.ingresosnamao.feature.cart.data.CartLocalDataSource
import com.innaval.ingresosnamao.feature.cart.data.RoomCartRepository
import com.innaval.ingresosnamao.feature.cart.domain.*
import com.innaval.ingresosnamao.feature.cart.presentation.CartViewModel
import com.innaval.ingresosnamao.feature.cart.presentation.CartCleanupViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val cartModule = module {
    single { CartLocalDataSource(get(), get()) }
    single<CartRepository> { RoomCartRepository(get(), get()) }
    factoryOf(::ObserveCartUseCase)
    factoryOf(::GetCartUseCase)
    factoryOf(::AddToCartUseCase)
    factoryOf(::SetCartQuantityUseCase)
    factoryOf(::RemoveFromCartUseCase)
    factoryOf(::ClearCartUseCase)
    viewModel { (userId: String) -> CartViewModel(userId, get(), get(), get(), get()) }
    viewModel { (userId: String, fingerprint: String) -> CartCleanupViewModel(userId, fingerprint, get()) }
}
