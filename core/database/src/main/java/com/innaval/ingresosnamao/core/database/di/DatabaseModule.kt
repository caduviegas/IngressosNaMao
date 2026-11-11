package com.innaval.ingresosnamao.core.database.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.innaval.ingresosnamao.core.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val migration1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cart_items` (`userId` TEXT NOT NULL, `eventId` TEXT NOT NULL, " +
                "`eventName` TEXT NOT NULL, `unitPriceCents` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, " +
                "PRIMARY KEY(`userId`, `eventId`))"
        )
    }
}

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "ingressos.db")
            .addMigrations(migration1To2).build()
    }
    single { get<AppDatabase>().ticketDao() }
    single { get<AppDatabase>().cartDao() }
}
