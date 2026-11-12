package com.innaval.ingresosnamao.feature.ticket.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.innaval.ingresosnamao.core.common.dispatchers.DispatcherProvider
import com.innaval.ingresosnamao.core.database.AppDatabase
import com.innaval.ingresosnamao.core.database.di.migration1To2
import com.innaval.ingresosnamao.core.database.di.migration2To3
import com.innaval.ingresosnamao.feature.ticket.domain.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TicketRoomTest {
    private lateinit var context: Context
    private val databaseName = "ticket-flow-test.db"
    private val databases = mutableListOf<AppDatabase>()
    private val dispatchers = mockk<DispatcherProvider> { every { io } returns Dispatchers.IO }
    private val purchase = SimulatedPurchase(
        "reference", "order", "alice", "fingerprint", 300,
        listOf(PurchaseItem("event", "Show", 100, 3)),
    )

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
    }

    @After fun cleanup() {
        databases.forEach { it.close() }
        context.deleteDatabase(databaseName)
    }

    private fun open(): Pair<AppDatabase, TicketRepositoryImpl> {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(migration1To2, migration2To3).allowMainThreadQueries().build()
        databases.add(db)
        return db to TicketRepositoryImpl(db, db.issuedTicketDao(), dispatchers)
    }

    @Test fun givenConcurrentAndRepeatedIssuanceWhenSavedThenIsIdempotentAndIsolated() = runBlocking {
        val (db, repo) = open()
        val results = coroutineScope { List(8) { async { repo.issue(purchase) } }.awaitAll() }
        assertTrue(results.all { it is TicketResult.Success })
        val receipts = results.map { (it as TicketResult.Success).value }
        assertEquals(1, receipts.map { receipt -> receipt.tickets.map { it.id }.toSet() }.distinct().size)
        val receipt = receipts.first()
        assertEquals(3, receipt.tickets.map { it.id }.toSet().size)
        assertEquals(TicketResult.Success(null), repo.observeReceipt("bob", "reference").first())
        assertEquals(TicketResult.Success(null), repo.observeTicket("bob", receipt.tickets.first().id).first())
        assertEquals(TicketResult.Success(emptyList<PurchaseSummary>()), repo.observePurchases("bob").first())
        assertEquals(TicketResult.Failure(TicketError.CONFLICT), repo.issue(purchase.copy(userId = "bob")))
        assertEquals(TicketResult.Failure(TicketError.CONFLICT), repo.issue(purchase.copy(reference = "other")))
        assertEquals(TicketResult.Failure(TicketError.CONFLICT), repo.issue(purchase.copy(items = listOf(PurchaseItem("other", "Show", 100, 3)))))
        db.close()
        databases.remove(db)
        val (_, reopened) = open()
        val restored = reopened.issue(purchase) as TicketResult.Success
        assertEquals(receipt.tickets.map { it.id }.toSet(), restored.value.tickets.map { it.id }.toSet())
    }

    @Test fun givenInsertionFailureWhenIssuingThenRollsBackAndAllowsRetry() = runBlocking {
        val (db, repo) = open()
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_ticket BEFORE INSERT ON issued_tickets BEGIN SELECT RAISE(ABORT, 'test'); END"
        )
        assertEquals(TicketResult.Failure(TicketError.STORAGE), repo.issue(purchase))
        assertNull(db.issuedTicketDao().findPurchase(purchase.reference))
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_ticket")
        assertTrue(repo.issue(purchase) is TicketResult.Success)
    }

    @Test fun givenLegacyTicketsAndCartWhenMigratingThenPreservesAndIsolatesData() = runBlocking {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("CREATE TABLE tickets (id TEXT NOT NULL PRIMARY KEY, eventId TEXT NOT NULL, eventName TEXT NOT NULL, qrCode TEXT NOT NULL)")
            db.execSQL("INSERT INTO tickets VALUES ('old', 'event', 'Show', 'qr')")
            db.execSQL("CREATE TABLE cart_items (userId TEXT NOT NULL, eventId TEXT NOT NULL, eventName TEXT NOT NULL, unitPriceCents INTEGER NOT NULL, quantity INTEGER NOT NULL, PRIMARY KEY(userId, eventId))")
            db.execSQL("INSERT INTO cart_items VALUES ('alice', 'event', 'Show', 100, 2)")
            db.version = 2
        }
        val (db, repo) = open()
        assertEquals("old", db.ticketDao().observeAll().first().single().id)
        assertEquals(2, db.cartDao().getAll("alice").single().quantity)
        assertEquals(TicketResult.Success(emptyList<PurchaseSummary>()), repo.observePurchases("alice").first())
        assertTrue(repo.issue(purchase) is TicketResult.Success)
    }
}
