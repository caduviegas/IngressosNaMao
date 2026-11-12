package com.innaval.ingresosnamao.core.database.ticket

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "ticket_purchases", indices = [Index(value = ["orderId"], unique = true)])
data class TicketPurchaseEntity(
    @PrimaryKey val reference: String,
    val orderId: String,
    val userId: String,
    val fingerprint: String,
    val totalCents: Long,
    val issuedAtMillis: Long,
)

@Entity(
    tableName = "issued_tickets",
    indices = [Index(value = ["purchaseReference"])],
    foreignKeys = [ForeignKey(
        entity = TicketPurchaseEntity::class,
        parentColumns = ["reference"],
        childColumns = ["purchaseReference"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class IssuedTicketEntity(
    @PrimaryKey val id: String,
    val purchaseReference: String,
    val eventId: String,
    val eventName: String,
    val unitPriceCents: Long,
)

@Dao
interface IssuedTicketDao {
    @Query("SELECT * FROM ticket_purchases WHERE reference = :reference")
    suspend fun findPurchase(reference: String): TicketPurchaseEntity?

    @Query("SELECT * FROM ticket_purchases WHERE orderId = :orderId")
    suspend fun findPurchaseByOrder(orderId: String): TicketPurchaseEntity?

    @Query("SELECT * FROM issued_tickets WHERE purchaseReference = :reference ORDER BY id")
    suspend fun findTickets(reference: String): List<IssuedTicketEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchase(purchase: TicketPurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTickets(tickets: List<IssuedTicketEntity>)

    @Query("SELECT * FROM ticket_purchases WHERE userId = :userId ORDER BY issuedAtMillis DESC, reference")
    fun observePurchases(userId: String): Flow<List<TicketPurchaseEntity>>

    @Query("SELECT * FROM ticket_purchases WHERE userId = :userId AND reference = :reference")
    fun observePurchase(userId: String, reference: String): Flow<TicketPurchaseEntity?>

    @Query("SELECT t.* FROM issued_tickets t JOIN ticket_purchases p ON p.reference = t.purchaseReference WHERE p.userId = :userId AND p.reference = :reference ORDER BY t.eventName, t.id")
    fun observeTickets(userId: String, reference: String): Flow<List<IssuedTicketEntity>>

    @Query("SELECT t.* FROM issued_tickets t JOIN ticket_purchases p ON p.reference = t.purchaseReference WHERE p.userId = :userId AND t.id = :id")
    fun observeTicket(userId: String, id: String): Flow<IssuedTicketEntity?>
}
