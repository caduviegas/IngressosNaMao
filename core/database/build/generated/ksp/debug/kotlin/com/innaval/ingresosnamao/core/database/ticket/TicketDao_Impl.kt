package com.innaval.ingresosnamao.core.database.ticket

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TicketDao_Impl(
  __db: RoomDatabase,
) : TicketDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTicketEntity: EntityInsertAdapter<TicketEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTicketEntity = object : EntityInsertAdapter<TicketEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `tickets` (`id`,`eventId`,`eventName`,`qrCode`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TicketEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.eventId)
        statement.bindText(3, entity.eventName)
        statement.bindText(4, entity.qrCode)
      }
    }
  }

  public override suspend fun upsertAll(tickets: List<TicketEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTicketEntity.insert(_connection, tickets)
  }

  public override fun observeAll(): Flow<List<TicketEntity>> {
    val _sql: String = "SELECT * FROM tickets"
    return createFlow(__db, false, arrayOf("tickets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfEventId: Int = getColumnIndexOrThrow(_stmt, "eventId")
        val _columnIndexOfEventName: Int = getColumnIndexOrThrow(_stmt, "eventName")
        val _columnIndexOfQrCode: Int = getColumnIndexOrThrow(_stmt, "qrCode")
        val _result: MutableList<TicketEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TicketEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpEventId: String
          _tmpEventId = _stmt.getText(_columnIndexOfEventId)
          val _tmpEventName: String
          _tmpEventName = _stmt.getText(_columnIndexOfEventName)
          val _tmpQrCode: String
          _tmpQrCode = _stmt.getText(_columnIndexOfQrCode)
          _item = TicketEntity(_tmpId,_tmpEventId,_tmpEventName,_tmpQrCode)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
