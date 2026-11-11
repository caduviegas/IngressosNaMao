package com.innaval.ingresosnamao.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.innaval.ingresosnamao.core.database.cart.CartDao
import com.innaval.ingresosnamao.core.database.cart.CartEntity
import com.innaval.ingresosnamao.core.database.ticket.TicketDao
import com.innaval.ingresosnamao.core.database.ticket.TicketEntity
import com.innaval.ingresosnamao.core.database.ticket.IssuedTicketDao
import com.innaval.ingresosnamao.core.database.ticket.IssuedTicketEntity
import com.innaval.ingresosnamao.core.database.ticket.TicketPurchaseEntity

@Database(entities = [TicketEntity::class, CartEntity::class, TicketPurchaseEntity::class, IssuedTicketEntity::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ticketDao(): TicketDao
    abstract fun cartDao(): CartDao
    abstract fun issuedTicketDao(): IssuedTicketDao
}
