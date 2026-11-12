package com.innaval.ingresosnamao.feature.profile.di

import com.innaval.ingresosnamao.feature.profile.data.FirebaseProfileAuthSource
import com.innaval.ingresosnamao.feature.profile.data.FirebaseProfileRepository
import com.innaval.ingresosnamao.feature.profile.data.FirestoreProfileDocumentSource
import com.innaval.ingresosnamao.feature.profile.data.ProfileAuthSource
import com.innaval.ingresosnamao.feature.profile.data.ProfileDocumentSource
import com.innaval.ingresosnamao.feature.profile.domain.GetProfileUseCase
import com.innaval.ingresosnamao.feature.profile.domain.LogoutUseCase
import com.innaval.ingresosnamao.feature.profile.domain.ProfileRepository
import com.innaval.ingresosnamao.feature.profile.presentation.ProfileViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun profileModule() = module {
    single<ProfileAuthSource> { FirebaseProfileAuthSource() }
    single<ProfileDocumentSource> { FirestoreProfileDocumentSource() }
    single<ProfileRepository> { FirebaseProfileRepository(get(), get()) }
    factoryOf(::GetProfileUseCase)
    factoryOf(::LogoutUseCase)
    viewModelOf(::ProfileViewModel)
}
