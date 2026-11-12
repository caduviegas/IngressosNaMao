package com.innaval.ingresosnamao.core.database.ticket

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
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
public class IssuedTicketDao_Impl(
  __db: RoomDatabase,
) : IssuedTicketDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTicketPurchaseEntity: EntityInsertAdapter<TicketPurchaseEntity>

  private val __insertAdapterOfIssuedTicketEntity: EntityInsertAdapter<IssuedTicketEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTicketPurchaseEntity = object : EntityInsertAdapter<TicketPurchaseEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `ticket_purchases` (`reference`,`orderId`,`userId`,`fingerprint`,`totalCents`,`issuedAtMillis`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TicketPurchaseEntity) {
        statement.bindText(1, entity.reference)
        statement.bindText(2, entity.orderId)
        statement.bindText(3, entity.userId)
        statement.bindText(4, entity.fingerprint)
        statement.bindLong(5, entity.totalCents)
        statement.bindLong(6, entity.issuedAtMillis)
      }
    }
    this.__insertAdapterOfIssuedTicketEntity = object : EntityInsertAdapter<IssuedTicketEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `issued_tickets` (`id`,`purchaseReference`,`eventId`,`eventName`,`unitPriceCents`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: IssuedTicketEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.purchaseReference)
        statement.bindText(3, entity.eventId)
        statement.bindText(4, entity.eventName)
        statement.bindLong(5, entity.unitPriceCents)
      }
    }
  }

  public override suspend fun insertPurchase(purchase: TicketPurchaseEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTicketPurchaseEntity.insert(_connection, purchase)
  }

  public override suspend fun insertTickets(tickets: List<IssuedTicketEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfIssuedTicketEntity.insert(_connection, tickets)
  }

  public override suspend fun findPurchase(reference: String): TicketPurchaseEntity? {
    val _sql: String = "SELECT * FROM ticket_purchases WHERE reference = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, reference)
        val _columnIndexOfReference: Int = getColumnIndexOrThrow(_stmt, "reference")
        val _columnIndexOfOrderId: Int = getColumnIndexOrThrow(_stmt, "orderId")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfFingerprint: Int = getColumnIndexOrThrow(_stmt, "fingerprint")
        val _columnIndexOfTotalCents: Int = getColumnIndexOrThrow(_stmt, "totalCents")
        val _columnIndexOfIssuedAtMillis: Int = getColumnIndexOrThrow(_stmt, "issuedAtMillis")
        val _result: TicketPurchaseEntity?
        if (_stmt.step()) {
          val _tmpReference: String
          _tmpReference = _stmt.getText(_columnIndexOfReference)
          val _tmpOrderId: String
          _tmpOrderId = _stmt.getText(_columnIndexOfOrderId)
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpFingerprint: String
          _tmpFingerprint = _stmt.getText(_columnIndexOfFingerprint)
          val _tmpTotalCents: Long
          _tmpTotalCents = _stmt.getLong(_columnIndexOfTotalCents)
          val _tmpIssuedAtMillis: Long
          _tmpIssuedAtMillis = _stmt.getLong(_columnIndexOfIssuedAtMillis)
          _result = TicketPurchaseEntity(_tmpReference,_tmpOrderId,_tmpUserId,_tmpFingerprint,_tmpTotalCents,_tmpIssuedAtMillis)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun findPurchaseByOrder(orderId: String): TicketPurchaseEntity? {
    val _sql: String = "SELECT * FROM ticket_purchases WHERE orderId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, orderId)
        val _columnIndexOfReference: Int = getColumnIndexOrThrow(_stmt, "reference")
        val _columnIndexOfOrderId: Int = getColumnIndexOrThrow(_stmt, "orderId")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfFingerprint: Int = getColumnIndexOrThrow(_stmt, "fingerprint")
        val _columnIndexOfTotalCents: Int = getColumnIndexOrThrow(_stmt, "totalCents")
        val _columnIndexOfIssuedAtMillis: Int = getColumnIndexOrThrow(_stmt, "issuedAtMillis")
        val _result: TicketPurchaseEntity?
        if (_stmt.step()) {
          val _tmpReference: String
          _tmpReference = _stmt.getText(_columnIndexOfReference)
          val _tmpOrderId: String
          _tmpOrderId = _stmt.getText(_columnIndexOfOrderId)
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpFingerprint: String
          _tmpFingerprint = _stmt.getText(_columnIndexOfFingerprint)
          val _tmpTotalCents: Long
          _tmpTotalCents = _stmt.getLong(_columnIndexOfTotalCents)
          val _tmpIssuedAtMillis: Long
          _tmpIssuedAtMillis = _stmt.getLong(_columnIndexOfIssuedAtMillis)
          _result = TicketPurchaseEntity(_tmpReference,_tmpOrderId,_tmpUserId,_tmpFingerprint,_tmpTotalCents,_tmpIssuedAtMillis)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun findTickets(reference: String): List<IssuedTicketEntity> {
    val _sql: String = "SELECT * FROM issued_tickets WHERE purchaseReference = ? ORDER BY id"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, reference)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPurchaseReference: Int = getColumnIndexOrThrow(_stmt, "purchaseReference")
        val _columnIndexOfEventId: Int = getColumnIndexOrThrow(_stmt, "eventId")
        val _columnIndexOfEventName: Int = getColumnIndexOrThrow(_stmt, "eventName")
        val _columnIndexOfUnitPriceCents: Int = getColumnIndexOrThrow(_stmt, "unitPriceCents")
        val _result: MutableList<IssuedTicketEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: IssuedTicketEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpPurchaseReference: String
          _tmpPurchaseReference = _stmt.getText(_columnIndexOfPurchaseReference)
          val _tmpEventId: String
          _tmpEventId = _stmt.getText(_columnIndexOfEventId)
          val _tmpEventName: String
          _tmpEventName = _stmt.getText(_columnIndexOfEventName)
          val _tmpUnitPriceCents: Long
          _tmpUnitPriceCents = _stmt.getLong(_columnIndexOfUnitPriceCents)
          _item = IssuedTicketEntity(_tmpId,_tmpPurchaseReference,_tmpEventId,_tmpEventName,_tmpUnitPriceCents)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observePurchases(userId: String): Flow<List<TicketPurchaseEntity>> {
    val _sql: String = "SELECT * FROM ticket_purchases WHERE userId = ? ORDER BY issuedAtMillis DESC, reference"
    return createFlow(__db, false, arrayOf("ticket_purchases")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        val _columnIndexOfReference: Int = getColumnIndexOrThrow(_stmt, "reference")
        val _columnIndexOfOrderId: Int = getColumnIndexOrThrow(_stmt, "orderId")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfFingerprint: Int = getColumnIndexOrThrow(_stmt, "fingerprint")
        val _columnIndexOfTotalCents: Int = getColumnIndexOrThrow(_stmt, "totalCents")
        val _columnIndexOfIssuedAtMillis: Int = getColumnIndexOrThrow(_stmt, "issuedAtMillis")
        val _result: MutableList<TicketPurchaseEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TicketPurchaseEntity
          val _tmpReference: String
          _tmpReference = _stmt.getText(_columnIndexOfReference)
          val _tmpOrderId: String
          _tmpOrderId = _stmt.getText(_columnIndexOfOrderId)
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpFingerprint: String
          _tmpFingerprint = _stmt.getText(_columnIndexOfFingerprint)
          val _tmpTotalCents: Long
          _tmpTotalCents = _stmt.getLong(_columnIndexOfTotalCents)
          val _tmpIssuedAtMillis: Long
          _tmpIssuedAtMillis = _stmt.getLong(_columnIndexOfIssuedAtMillis)
          _item = TicketPurchaseEntity(_tmpReference,_tmpOrderId,_tmpUserId,_tmpFingerprint,_tmpTotalCents,_tmpIssuedAtMillis)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observePurchase(userId: String, reference: String): Flow<TicketPurchaseEntity?> {
    val _sql: String = "SELECT * FROM ticket_purchases WHERE userId = ? AND reference = ?"
    return createFlow(__db, false, arrayOf("ticket_purchases")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        _argIndex = 2
        _stmt.bindText(_argIndex, reference)
        val _columnIndexOfReference: Int = getColumnIndexOrThrow(_stmt, "reference")
        val _columnIndexOfOrderId: Int = getColumnIndexOrThrow(_stmt, "orderId")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfFingerprint: Int = getColumnIndexOrThrow(_stmt, "fingerprint")
        val _columnIndexOfTotalCents: Int = getColumnIndexOrThrow(_stmt, "totalCents")
        val _columnIndexOfIssuedAtMillis: Int = getColumnIndexOrThrow(_stmt, "issuedAtMillis")
        val _result: TicketPurchaseEntity?
        if (_stmt.step()) {
          val _tmpReference: String
          _tmpReference = _stmt.getText(_columnIndexOfReference)
          val _tmpOrderId: String
          _tmpOrderId = _stmt.getText(_columnIndexOfOrderId)
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpFingerprint: String
          _tmpFingerprint = _stmt.getText(_columnIndexOfFingerprint)
          val _tmpTotalCents: Long
          _tmpTotalCents = _stmt.getLong(_columnIndexOfTotalCents)
          val _tmpIssuedAtMillis: Long
          _tmpIssuedAtMillis = _stmt.getLong(_columnIndexOfIssuedAtMillis)
          _result = TicketPurchaseEntity(_tmpReference,_tmpOrderId,_tmpUserId,_tmpFingerprint,_tmpTotalCents,_tmpIssuedAtMillis)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeTickets(userId: String, reference: String): Flow<List<IssuedTicketEntity>> {
    val _sql: String = "SELECT t.* FROM issued_tickets t JOIN ticket_purchases p ON p.reference = t.purchaseReference WHERE p.userId = ? AND p.reference = ? ORDER BY t.eventName, t.id"
    return createFlow(__db, false, arrayOf("issued_tickets", "ticket_purchases")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        _argIndex = 2
        _stmt.bindText(_argIndex, reference)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPurchaseReference: Int = getColumnIndexOrThrow(_stmt, "purchaseReference")
        val _columnIndexOfEventId: Int = getColumnIndexOrThrow(_stmt, "eventId")
        val _columnIndexOfEventName: Int = getColumnIndexOrThrow(_stmt, "eventName")
        val _columnIndexOfUnitPriceCents: Int = getColumnIndexOrThrow(_stmt, "unitPriceCents")
        val _result: MutableList<IssuedTicketEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: IssuedTicketEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpPurchaseReference: String
          _tmpPurchaseReference = _stmt.getText(_columnIndexOfPurchaseReference)
          val _tmpEventId: String
          _tmpEventId = _stmt.getText(_columnIndexOfEventId)
          val _tmpEventName: String
          _tmpEventName = _stmt.getText(_columnIndexOfEventName)
          val _tmpUnitPriceCents: Long
          _tmpUnitPriceCents = _stmt.getLong(_columnIndexOfUnitPriceCents)
          _item = IssuedTicketEntity(_tmpId,_tmpPurchaseReference,_tmpEventId,_tmpEventName,_tmpUnitPriceCents)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeTicket(userId: String, id: String): Flow<IssuedTicketEntity?> {
    val _sql: String = "SELECT t.* FROM issued_tickets t JOIN ticket_purchases p ON p.reference = t.purchaseReference WHERE p.userId = ? AND t.id = ?"
    return createFlow(__db, false, arrayOf("issued_tickets", "ticket_purchases")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPurchaseReference: Int = getColumnIndexOrThrow(_stmt, "purchaseReference")
        val _columnIndexOfEventId: Int = getColumnIndexOrThrow(_stmt, "eventId")
        val _columnIndexOfEventName: Int = getColumnIndexOrThrow(_stmt, "eventName")
        val _columnIndexOfUnitPriceCents: Int = getColumnIndexOrThrow(_stmt, "unitPriceCents")
        val _result: IssuedTicketEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpPurchaseReference: String
          _tmpPurchaseReference = _stmt.getText(_columnIndexOfPurchaseReference)
          val _tmpEventId: String
          _tmpEventId = _stmt.getText(_columnIndexOfEventId)
          val _tmpEventName: String
          _tmpEventName = _stmt.getText(_columnIndexOfEventName)
          val _tmpUnitPriceCents: Long
          _tmpUnitPriceCents = _stmt.getLong(_columnIndexOfUnitPriceCents)
          _result = IssuedTicketEntity(_tmpId,_tmpPurchaseReference,_tmpEventId,_tmpEventName,_tmpUnitPriceCents)
        } else {
          _result = null
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
