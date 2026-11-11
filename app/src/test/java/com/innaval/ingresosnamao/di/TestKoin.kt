package com.innaval.ingresosnamao.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

internal fun testKoin(application: Application, vararg overrides: Module): KoinApplication =
    koinApplication {
        androidContext(application)
        allowOverride(true)
        modules(appModules() + overrides)
    }
