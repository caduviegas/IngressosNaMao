package com.innaval.ingresosnamao.core.common.di

import com.innaval.ingresosnamao.core.common.dispatchers.DefaultDispatcherProvider
import com.innaval.ingresosnamao.core.common.dispatchers.DispatcherProvider
import org.koin.dsl.module

fun commonModule() = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
}
