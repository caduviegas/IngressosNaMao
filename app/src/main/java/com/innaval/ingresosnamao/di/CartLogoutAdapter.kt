package com.innaval.ingresosnamao.di

import com.innaval.ingresosnamao.feature.cart.domain.CartResult
import com.innaval.ingresosnamao.feature.cart.domain.ClearCartForLogoutUseCase
import com.innaval.ingresosnamao.feature.auth.domain.AuthSessionLogout
import com.innaval.ingresosnamao.feature.profile.domain.ProfileCartCleaner
import com.innaval.ingresosnamao.feature.profile.domain.ProfileError
import com.innaval.ingresosnamao.feature.profile.domain.ProfileResult
import org.koin.dsl.module

internal class CartLogoutAdapter(private val clearCart: ClearCartForLogoutUseCase) : ProfileCartCleaner {
    override suspend fun clear(userId: String): ProfileResult<Unit> = when (clearCart(userId)) {
        is CartResult.Success -> ProfileResult.Success(Unit)
        is CartResult.Failure -> ProfileResult.Failure(ProfileError.CART)
    }
}

internal fun profileIntegrationModule() = module {
    single<ProfileCartCleaner> { CartLogoutAdapter(get()) }
    single<AuthSessionLogout> { AuthLogoutAdapter(get()) }
}
