package com.innaval.ingresosnamao.feature.cart.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.innaval.ingresosnamao.core.common.dispatchers.DispatcherProvider
import com.innaval.ingresosnamao.core.database.AppDatabase
import com.innaval.ingresosnamao.core.database.di.migration1To2
import com.innaval.ingresosnamao.core.database.di.migration2To3
import com.innaval.ingresosnamao.feature.cart.domain.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CartRoomTest {
    private lateinit var context: Context
    private val databaseName = "cart-room-test.db"
    private val opened = mutableListOf<AppDatabase>()
    private val dispatchers = mockk<DispatcherProvider> { every { io } returns Dispatchers.IO }

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
    }

    @After fun cleanup() {
        opened.forEach { it.close() }
        context.deleteDatabase(databaseName)
    }

    private fun open(): Pair<AppDatabase, RoomCartRepository> {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(migration1To2, migration2To3)
            .allowMainThreadQueries()
            .build()
        opened.add(db)
        return db to RoomCartRepository(CartLocalDataSource(db, db.cartDao()), dispatchers)
    }

    @Test fun persistsRestoresAndIsolatesAccounts() = runBlocking {
        val (db, repo) = open()
        val item = CartItem("event", "Show", 100, 1)
        assertEquals(CartResult.Success(Unit), repo.add("alice", item))
        assertEquals(CartResult.Success(Unit), repo.add("alice", item.copy(eventName = "Novo", unitPriceCents = 150)))
        assertEquals(CartResult.Success(Unit), repo.add("bob", item))
        assertEquals(CartResult.Success(Unit), repo.setQuantity("alice", "event", 3))
        db.close()
        opened.remove(db)
        val (_, reopened) = open()
        assertEquals(
            CartResult.Success(CartSnapshot(listOf(item.copy(eventName = "Novo", unitPriceCents = 150, quantity = 3)), 450)),
            reopened.observe("alice").first(),
        )
        assertEquals(CartResult.Success(CartSnapshot(listOf(item), 100)), reopened.get("bob"))
        assertEquals(CartResult.Success(Unit), reopened.remove("alice", "event"))
        assertEquals(CartResult.Success(CartSnapshot(emptyList(), 0)), reopened.get("alice"))
        assertEquals(CartResult.Success(CartSnapshot(listOf(item), 100)), reopened.get("bob"))
        val fingerprint = cartFingerprint(CartSnapshot(listOf(item), 100))
        assertEquals(CartResult.Success(Unit), reopened.clear("bob", fingerprint))
        assertEquals(CartResult.Success(Unit), reopened.clear("bob", fingerprint))
        assertEquals(CartResult.Success(CartSnapshot(emptyList(), 0)), reopened.get("bob"))
    }

    @Test fun overflowRejectsMutationWithoutChangingStoredCart() = runBlocking {
        val (_, repo) = open()
        val item = CartItem("event", "Show", Long.MAX_VALUE, 1)
        assertEquals(CartResult.Success(Unit), repo.add("u", item))
        assertEquals(CartResult.Failure(CartError.LIMIT_EXCEEDED), repo.setQuantity("u", "event", 2))
        assertEquals(CartResult.Failure(CartError.LIMIT_EXCEEDED), repo.add("u", item))
        assertEquals(CartResult.Success(CartSnapshot(listOf(item), Long.MAX_VALUE)), repo.get("u"))
    }

    @Test fun concurrentAdditionsDoNotLoseUpdates() = runBlocking {
        val (_, repo) = open()
        val item = CartItem("event", "Show", 100, 1)
        val results = coroutineScope { List(10) { async { repo.add("u", item) } }.awaitAll() }
        assertEquals(List(10) { CartResult.Success(Unit) }, results)
        assertEquals(CartResult.Success(CartSnapshot(listOf(item.copy(quantity = 10)), 1000)), repo.get("u"))
    }

    @Test fun paidCleanupDoesNotRemoveItemsChangedAfterCheckout() = runBlocking {
        val (_, repo) = open()
        val item = CartItem("event", "Show", 100, 1)
        assertEquals(CartResult.Success(Unit), repo.add("u", item))
        val fingerprint = cartFingerprint(CartSnapshot(listOf(item), 100))
        assertEquals(CartResult.Success(Unit), repo.add("u", item))
        assertEquals(CartResult.Failure(CartError.CART_CHANGED), repo.clear("u", fingerprint))
        assertEquals(CartResult.Success(CartSnapshot(listOf(item.copy(quantity = 2)), 200)), repo.get("u"))
    }

    @Test fun migrationPreservesExistingTickets() = runBlocking {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS tickets (id TEXT NOT NULL PRIMARY KEY, eventId TEXT NOT NULL, eventName TEXT NOT NULL, qrCode TEXT NOT NULL)")
            db.execSQL("INSERT INTO tickets (id, eventId, eventName, qrCode) VALUES ('ticket', 'event', 'Show', 'qr')")
            db.version = 1
        }
        val (db, repo) = open()
        assertEquals("ticket", db.ticketDao().observeAll().first().single().id)
        assertEquals(CartResult.Success(CartSnapshot(emptyList(), 0)), repo.get("u"))
        assertEquals(CartResult.Success(Unit), repo.add("u", CartItem("event", "Show", 100, 1)))
        assertEquals("ticket", db.ticketDao().observeAll().first().single().id)
    }
}
