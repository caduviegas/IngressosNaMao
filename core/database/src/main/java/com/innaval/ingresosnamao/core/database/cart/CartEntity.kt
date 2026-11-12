package com.innaval.ingresosnamao.core.database.cart

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cart_items", primaryKeys = ["userId", "eventId"])
data class CartEntity(
    val userId: String,
    val eventId: String,
    val eventName: String,
    val unitPriceCents: Long,
    val quantity: Int,
)

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items WHERE userId = :userId ORDER BY rowid")
    fun observe(userId: String): Flow<List<CartEntity>>

    @Query("SELECT * FROM cart_items WHERE userId = :userId ORDER BY rowid")
    suspend fun getAll(userId: String): List<CartEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartEntity)

    @Query("DELETE FROM cart_items WHERE userId = :userId AND eventId = :eventId")
    suspend fun remove(userId: String, eventId: String): Int

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clear(userId: String)
}
