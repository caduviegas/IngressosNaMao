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

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "7e70680879a09576af9736d4e8f0aa76", "4cc4ed20c2ad2297343b3314fd2a0548") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tickets` (`id` TEXT NOT NULL, `eventId` TEXT NOT NULL, `eventName` TEXT NOT NULL, `qrCode` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '7e70680879a09576af9736d4e8f0aa76')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `tickets`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
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
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "tickets")
  }

  public override fun clearAllTables() {
    super.performClear(false, "tickets")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(TicketDao::class, TicketDao_Impl.getRequiredConverters())
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
}
