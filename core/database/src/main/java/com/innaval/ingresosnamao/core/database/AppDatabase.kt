package com.innaval.ingresosnamao.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.innaval.ingresosnamao.core.database.cart.CartDao
import com.innaval.ingresosnamao.core.database.cart.CartEntity
import com.innaval.ingresosnamao.core.database.ticket.TicketDao
import com.innaval.ingresosnamao.core.database.ticket.TicketEntity

@Database(entities = [TicketEntity::class, CartEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ticketDao(): TicketDao
    abstract fun cartDao(): CartDao
}
