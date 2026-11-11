package com.innaval.ingresosnamao

import android.app.Application
import com.innaval.ingresosnamao.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class IngressosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@IngressosApp)
            allowOverride(false)
            modules(appModules())
        }
    }
}
