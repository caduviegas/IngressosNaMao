package com.innaval.ingresosnamao.core.database

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.innaval.ingresosnamao.core.database.cart.CartDao
import com.innaval.ingresosnamao.core.database.cart.CartDao_Impl
import com.innaval.ingresosnamao.core.database.ticket.IssuedTicketDao
import com.innaval.ingresosnamao.core.database.ticket.IssuedTicketDao_Impl
import com.innaval.ingresosnamao.core.database.ticket.TicketDao
import com.innaval.ingresosnamao.core.database.ticket.TicketDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _ticketDao: Lazy<TicketDao> = lazy {
    TicketDao_Impl(this)
  }

  private val _cartDao: Lazy<CartDao> = lazy {
    CartDao_Impl(this)
  }

  private val _issuedTicketDao: Lazy<IssuedTicketDao> = lazy {
    IssuedTicketDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(3, "3b52aa5f8de5d1691e4d43fcc0c4b8d3", "38639810e3abf9a1035113c5055d20d5") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tickets` (`id` TEXT NOT NULL, `eventId` TEXT NOT NULL, `eventName` TEXT NOT NULL, `qrCode` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `cart_items` (`userId` TEXT NOT NULL, `eventId` TEXT NOT NULL, `eventName` TEXT NOT NULL, `unitPriceCents` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, PRIMARY KEY(`userId`, `eventId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `ticket_purchases` (`reference` TEXT NOT NULL, `orderId` TEXT NOT NULL, `userId` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `totalCents` INTEGER NOT NULL, `issuedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`reference`))")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ticket_purchases_orderId` ON `ticket_purchases` (`orderId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `issued_tickets` (`id` TEXT NOT NULL, `purchaseReference` TEXT NOT NULL, `eventId` TEXT NOT NULL, `eventName` TEXT NOT NULL, `unitPriceCents` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`purchaseReference`) REFERENCES `ticket_purchases`(`reference`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_issued_tickets_purchaseReference` ON `issued_tickets` (`purchaseReference`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3b52aa5f8de5d1691e4d43fcc0c4b8d3')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `tickets`")
        connection.execSQL("DROP TABLE IF EXISTS `cart_items`")
        connection.execSQL("DROP TABLE IF EXISTS `ticket_purchases`")
        connection.execSQL("DROP TABLE IF EXISTS `issued_tickets`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsTickets: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTickets.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTickets.put("eventId", TableInfo.Column("eventId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTickets.put("eventName", TableInfo.Column("eventName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTickets.put("qrCode", TableInfo.Column("qrCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTickets: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTickets: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTickets: TableInfo = TableInfo("tickets", _columnsTickets, _foreignKeysTickets, _indicesTickets)
        val _existingTickets: TableInfo = read(connection, "tickets")
        if (!_infoTickets.equals(_existingTickets)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |tickets(com.innaval.ingresosnamao.core.database.ticket.TicketEntity).
              | Expected:
              |""".trimMargin() + _infoTickets + """
              |
              | Found:
              |""".trimMargin() + _existingTickets)
        }
        val _columnsCartItems: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCartItems.put("userId", TableInfo.Column("userId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCartItems.put("eventId", TableInfo.Column("eventId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCartItems.put("eventName", TableInfo.Column("eventName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCartItems.put("unitPriceCents", TableInfo.Column("unitPriceCents", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCartItems.put("quantity", TableInfo.Column("quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCartItems: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCartItems: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCartItems: TableInfo = TableInfo("cart_items", _columnsCartItems, _foreignKeysCartItems, _indicesCartItems)
        val _existingCartItems: TableInfo = read(connection, "cart_items")
        if (!_infoCartItems.equals(_existingCartItems)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |cart_items(com.innaval.ingresosnamao.core.database.cart.CartEntity).
              | Expected:
              |""".trimMargin() + _infoCartItems + """
              |
              | Found:
              |""".trimMargin() + _existingCartItems)
        }
        val _columnsTicketPurchases: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTicketPurchases.put("reference", TableInfo.Column("reference", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTicketPurchases.put("orderId", TableInfo.Column("orderId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTicketPurchases.put("userId", TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTicketPurchases.put("fingerprint", TableInfo.Column("fingerprint", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTicketPurchases.put("totalCents", TableInfo.Column("totalCents", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTicketPurchases.put("issuedAtMillis", TableInfo.Column("issuedAtMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTicketPurchases: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTicketPurchases: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesTicketPurchases.add(TableInfo.Index("index_ticket_purchases_orderId", true, listOf("orderId"), listOf("ASC")))
        val _infoTicketPurchases: TableInfo = TableInfo("ticket_purchases", _columnsTicketPurchases, _foreignKeysTicketPurchases, _indicesTicketPurchases)
        val _existingTicketPurchases: TableInfo = read(connection, "ticket_purchases")
        if (!_infoTicketPurchases.equals(_existingTicketPurchases)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |ticket_purchases(com.innaval.ingresosnamao.core.database.ticket.TicketPurchaseEntity).
              | Expected:
              |""".trimMargin() + _infoTicketPurchases + """
              |
              | Found:
              |""".trimMargin() + _existingTicketPurchases)
        }
        val _columnsIssuedTickets: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsIssuedTickets.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsIssuedTickets.put("purchaseReference", TableInfo.Column("purchaseReference", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsIssuedTickets.put("eventId", TableInfo.Column("eventId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsIssuedTickets.put("eventName", TableInfo.Column("eventName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsIssuedTickets.put("unitPriceCents", TableInfo.Column("unitPriceCents", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysIssuedTickets: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysIssuedTickets.add(TableInfo.ForeignKey("ticket_purchases", "CASCADE", "NO ACTION", listOf("purchaseReference"), listOf("reference")))
        val _indicesIssuedTickets: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesIssuedTickets.add(TableInfo.Index("index_issued_tickets_purchaseReference", false, listOf("purchaseReference"), listOf("ASC")))
        val _infoIssuedTickets: TableInfo = TableInfo("issued_tickets", _columnsIssuedTickets, _foreignKeysIssuedTickets, _indicesIssuedTickets)
        val _existingIssuedTickets: TableInfo = read(connection, "issued_tickets")
        if (!_infoIssuedTickets.equals(_existingIssuedTickets)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |issued_tickets(com.innaval.ingresosnamao.core.database.ticket.IssuedTicketEntity).
              | Expected:
              |""".trimMargin() + _infoIssuedTickets + """
              |
              | Found:
              |""".trimMargin() + _existingIssuedTickets)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "tickets", "cart_items", "ticket_purchases", "issued_tickets")
  }

  public override fun clearAllTables() {
    super.performClear(true, "tickets", "cart_items", "ticket_purchases", "issued_tickets")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(TicketDao::class, TicketDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CartDao::class, CartDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(IssuedTicketDao::class, IssuedTicketDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun ticketDao(): TicketDao = _ticketDao.value

  public override fun cartDao(): CartDao = _cartDao.value

  public override fun issuedTicketDao(): IssuedTicketDao = _issuedTicketDao.value
}
