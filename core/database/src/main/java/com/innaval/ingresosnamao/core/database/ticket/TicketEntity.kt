package com.innaval.ingresosnamao.core.database.ticket

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val eventName: String,
    val qrCode: String,
)

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets")
    fun observeAll(): Flow<List<TicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tickets: List<TicketEntity>)
}
