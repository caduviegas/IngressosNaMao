package com.innaval.ingresosnamao.core.database.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.innaval.ingresosnamao.core.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.dsl.onClose

val migration1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cart_items` (`userId` TEXT NOT NULL, `eventId` TEXT NOT NULL, " +
                "`eventName` TEXT NOT NULL, `unitPriceCents` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, " +
                "PRIMARY KEY(`userId`, `eventId`))"
        )
    }
}

val migration2To3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `ticket_purchases` (`reference` TEXT NOT NULL, `orderId` TEXT NOT NULL, `userId` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `totalCents` INTEGER NOT NULL, `issuedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`reference`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ticket_purchases_orderId` ON `ticket_purchases` (`orderId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `issued_tickets` (`id` TEXT NOT NULL, `purchaseReference` TEXT NOT NULL, `eventId` TEXT NOT NULL, `eventName` TEXT NOT NULL, `unitPriceCents` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`purchaseReference`) REFERENCES `ticket_purchases`(`reference`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_issued_tickets_purchaseReference` ON `issued_tickets` (`purchaseReference`)")
    }
}

fun databaseModule() = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "ingressos.db")
            .addMigrations(migration1To2, migration2To3).build()
    } onClose { it?.close() }
    single { get<AppDatabase>().ticketDao() }
    single { get<AppDatabase>().cartDao() }
    single { get<AppDatabase>().issuedTicketDao() }
}
