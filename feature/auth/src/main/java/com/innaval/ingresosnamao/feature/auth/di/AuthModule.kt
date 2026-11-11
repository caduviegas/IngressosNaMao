package com.innaval.ingresosnamao.feature.auth.di

import com.innaval.ingresosnamao.feature.auth.data.AuthRepositoryImpl
import com.innaval.ingresosnamao.feature.auth.data.FirebaseAuthDataSource
import com.innaval.ingresosnamao.feature.auth.data.FirestoreProfileDataSource
import com.innaval.ingresosnamao.feature.auth.domain.*
import com.innaval.ingresosnamao.feature.auth.presentation.AuthViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun authModule() = module {
    single { FirebaseAuthDataSource() }
    single { FirestoreProfileDataSource() }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    factoryOf(::LoginUseCase)
    factoryOf(::GoogleLoginUseCase)
    factoryOf(::RegisterUseCase)
    factoryOf(::GetCurrentUserUseCase)
    factoryOf(::GetProfileUseCase)
    factoryOf(::SaveProfileUseCase)
    factoryOf(::SignOutUseCase)
    viewModelOf(::AuthViewModel)
}
