package com.innaval.ingresosnamao.core.database.cart

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
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
public class CartDao_Impl(
  __db: RoomDatabase,
) : CartDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCartEntity: EntityInsertAdapter<CartEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCartEntity = object : EntityInsertAdapter<CartEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `cart_items` (`userId`,`eventId`,`eventName`,`unitPriceCents`,`quantity`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CartEntity) {
        statement.bindText(1, entity.userId)
        statement.bindText(2, entity.eventId)
        statement.bindText(3, entity.eventName)
        statement.bindLong(4, entity.unitPriceCents)
        statement.bindLong(5, entity.quantity.toLong())
      }
    }
  }

  public override suspend fun upsert(item: CartEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCartEntity.insert(_connection, item)
  }

  public override fun observe(userId: String): Flow<List<CartEntity>> {
    val _sql: String = "SELECT * FROM cart_items WHERE userId = ? ORDER BY rowid"
    return createFlow(__db, false, arrayOf("cart_items")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfEventId: Int = getColumnIndexOrThrow(_stmt, "eventId")
        val _columnIndexOfEventName: Int = getColumnIndexOrThrow(_stmt, "eventName")
        val _columnIndexOfUnitPriceCents: Int = getColumnIndexOrThrow(_stmt, "unitPriceCents")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _result: MutableList<CartEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CartEntity
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpEventId: String
          _tmpEventId = _stmt.getText(_columnIndexOfEventId)
          val _tmpEventName: String
          _tmpEventName = _stmt.getText(_columnIndexOfEventName)
          val _tmpUnitPriceCents: Long
          _tmpUnitPriceCents = _stmt.getLong(_columnIndexOfUnitPriceCents)
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
          _item = CartEntity(_tmpUserId,_tmpEventId,_tmpEventName,_tmpUnitPriceCents,_tmpQuantity)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(userId: String): List<CartEntity> {
    val _sql: String = "SELECT * FROM cart_items WHERE userId = ? ORDER BY rowid"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfEventId: Int = getColumnIndexOrThrow(_stmt, "eventId")
        val _columnIndexOfEventName: Int = getColumnIndexOrThrow(_stmt, "eventName")
        val _columnIndexOfUnitPriceCents: Int = getColumnIndexOrThrow(_stmt, "unitPriceCents")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _result: MutableList<CartEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CartEntity
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpEventId: String
          _tmpEventId = _stmt.getText(_columnIndexOfEventId)
          val _tmpEventName: String
          _tmpEventName = _stmt.getText(_columnIndexOfEventName)
          val _tmpUnitPriceCents: Long
          _tmpUnitPriceCents = _stmt.getLong(_columnIndexOfUnitPriceCents)
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
          _item = CartEntity(_tmpUserId,_tmpEventId,_tmpEventName,_tmpUnitPriceCents,_tmpQuantity)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun remove(userId: String, eventId: String): Int {
    val _sql: String = "DELETE FROM cart_items WHERE userId = ? AND eventId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        _argIndex = 2
        _stmt.bindText(_argIndex, eventId)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clear(userId: String) {
    val _sql: String = "DELETE FROM cart_items WHERE userId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
