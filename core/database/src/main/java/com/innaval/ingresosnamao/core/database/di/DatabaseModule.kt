package com.innaval.ingresosnamao.core.database.di

import androidx.room.Room
import com.innaval.ingresosnamao.core.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { Room.databaseBuilder(androidContext(), AppDatabase::class.java, "ingressos.db").build() }
    single { get<AppDatabase>().ticketDao() }
}
