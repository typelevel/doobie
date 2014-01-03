package doobie
package dbc

import scalaz._
import scala.collection.JavaConverters._
import scalaz.effect.IO
import scalaz.syntax.monad._
import java.sql
import sql.{ Date, Blob, Clob, Time, Timestamp, Ref, ResultSetMetaData }
import java.io.{ InputStream, Reader }
import java.net.URL
import java.util.{ Calendar }

object resultset extends util.TWorld[java.sql.ResultSet] {

  type ResultSet[+A] = Action[A]

  private[dbc] def run[A](a: ResultSet[A], s: sql.ResultSet): IO[A] = 
    eval(a, s).map(_._2)

  ////// ACTIONS, IN ALPHABETIC ORDER

  def absolute(row: Int): ResultSet[Boolean] =
    effect(_.absolute(row))

  def afterLast: ResultSet[Unit] =
    effect(_.afterLast)

  def beforeFirst: ResultSet[Unit] =
    effect(_.beforeFirst)

  def cancelRowUpdates: ResultSet[Unit] =
    effect(_.cancelRowUpdates)

  def clearWarnings: ResultSet[Unit] =
    effect(_.clearWarnings)

  def close: ResultSet[Unit] =
    effect(_.close)

  def deleteRow: ResultSet[Unit] =
    effect(_.deleteRow)

  def findColumn(colName: String): ResultSet[Int] =
    effect(_.findColumn(colName))

  def first: ResultSet[Boolean] =
    effect(_.first)

  def getArray(i: Int): ResultSet[sql.Array] =
    effect(_.getArray(i))

  def getArray(colName: String): ResultSet[sql.Array] =
    effect(_.getArray(colName))

  def getAsciiStream(index: Int): ResultSet[InputStream] =
    effect(_.getAsciiStream(index))

  def getAsciiStream(colName: String): ResultSet[InputStream] =
    effect(_.getAsciiStream(colName))

  def getBigDecimal(index: Int): ResultSet[BigDecimal] =
    effect(_.getBigDecimal(index)).map(BigDecimal(_))

  @deprecated("Deprecated in JDBC", "0.1")
  def getBigDecimal(index: Int, scale: Int): ResultSet[BigDecimal] =
    effect(_.getBigDecimal(index, scale))

  def getBigDecimal(colName: String): ResultSet[BigDecimal] =
    effect(_.getBigDecimal(colName)).map(BigDecimal(_))

  def getBinaryStream(index: Int): ResultSet[InputStream] =
    effect(_.getBinaryStream(index))

  def getBinaryStream(colName: String): ResultSet[InputStream] =
    effect(_.getBinaryStream(colName))

  def getBlob(i: Int): ResultSet[Blob] =
    effect(_.getBlob(i))

  def getBlob(colName: String): ResultSet[Blob] =
    effect(_.getBlob(colName))

  def getBoolean(index: Int): ResultSet[Boolean] =
    effect(_.getBoolean(index))

  def getBoolean(colName: String): ResultSet[Boolean] =
    effect(_.getBoolean(colName))

  def getByte(index: Int): ResultSet[Byte] =
    effect(_.getByte(index))

  def getByte(colName: String): ResultSet[Byte] =
    effect(_.getByte(colName))

  def getBytes(index: Int): ResultSet[Array[Byte]] =
    effect(_.getBytes(index))

  def getBytes(colName: String): ResultSet[Array[Byte]] =
    effect(_.getBytes(colName))

  def getCharacterStream(index: Int): ResultSet[Reader] =
    effect(_.getCharacterStream(index))

  def getCharacterStream(colName: String): ResultSet[Reader] =
    effect(_.getCharacterStream(colName))

  def getClob(i: Int): ResultSet[Clob] =
    effect(_.getClob(i))

  def getClob(colName: String): ResultSet[Clob] =
    effect(_.getClob(colName))

  def getConcurrency: ResultSet[ResultSetConcurrency] =
    effect(_.getConcurrency).map(ResultSetConcurrency.unsafeFromInt)

  def getCursorName: ResultSet[String] =
    effect(_.getCursorName)

  def getDate(index: Int): ResultSet[Date] =
    effect(_.getDate(index))

  def getDate(index: Int, cal: Calendar): ResultSet[Date] =
    effect(_.getDate(index, cal))

  def getDate(colName: String): ResultSet[Date] =
    effect(_.getDate(colName))

  def getDate(colName: String, cal: Calendar): ResultSet[Date] =
    effect(_.getDate(colName, cal))

  def getDouble(index: Int): ResultSet[Double] =
    effect(_.getDouble(index))

  def getDouble(colName: String): ResultSet[Double] =
    effect(_.getDouble(colName))

  def getFetchDirection: ResultSet[FetchDirection] =
    effect(_.getFetchDirection).map(FetchDirection.unsafeFromInt)

  def getFetchSize: ResultSet[Int] =
    effect(_.getFetchSize)

  def getFloat(index: Int): ResultSet[Float] =
    effect(_.getFloat(index))

  def getFloat(colName: String): ResultSet[Float] =
    effect(_.getFloat(colName))

  def getInt(index: Int): ResultSet[Int] =
    effect(_.getInt(index))

  def getInt(colName: String): ResultSet[Int] =
    effect(_.getInt(colName))

  def getLong(index: Int): ResultSet[Long] =
    effect(_.getLong(index))

  def getLong(colName: String): ResultSet[Long] =
    effect(_.getLong(colName))

  def getMetaData: ResultSet[ResultSetMetaData] =
    ???

  def getObject(index: Int): ResultSet[Object] =
    effect(_.getObject(index))

  def getObject(i: Int, map: Map[String, Class[_]]): ResultSet[Object] =
    effect(_.getObject(i, map.asJava))

  def getObject(colName: String): ResultSet[Object] =
    effect(_.getObject(colName))

  def getObject(colName: String, map: Map[String, Class[_]]): ResultSet[Object] =
    effect(_.getObject(colName, map.asJava))

  def getRef(i: Int): ResultSet[Ref] =
    effect(_.getRef(i))

  def getRef(colName: String): ResultSet[Ref] =
    effect(_.getRef(colName))

  def getRow: ResultSet[Int] =
    effect(_.getRow)

  def getShort(index: Int): ResultSet[Short] =
    effect(_.getShort(index))

  def getShort(colName: String): ResultSet[Short] =
    effect(_.getShort(colName))

  def getStatement[A](k: Statement[A]): ResultSet[A] =
    for {
      s <- effect(_.getStatement)
      a <- statement.run(k, s).liftIO[ResultSet]
    } yield a
    
  def getString(index: Int): ResultSet[String] =
    effect(_.getString(index))

  def getString(colName: String): ResultSet[String] =
    effect(_.getString(colName))

  def getTime(index: Int): ResultSet[Time] =
    effect(_.getTime(index))

  def getTime(index: Int, cal: Calendar): ResultSet[Time] =
    effect(_.getTime(index, cal))

  def getTime(colName: String): ResultSet[Time] =
    effect(_.getTime(colName))

  def getTime(colName: String, cal: Calendar): ResultSet[Time] =
    effect(_.getTime(colName, cal))

  def getTimestamp(index: Int): ResultSet[Timestamp] =
    effect(_.getTimestamp(index))

  def getTimestamp(index: Int, cal: Calendar): ResultSet[Timestamp] =
    effect(_.getTimestamp(index, cal))

  def getTimestamp(colName: String): ResultSet[Timestamp] =
    effect(_.getTimestamp(colName))

  def getTimestamp(colName: String, cal: Calendar): ResultSet[Timestamp] =
    effect(_.getTimestamp(colName, cal))

  def getType: ResultSet[ResultSetType] =
    effect(_.getType).map(ResultSetType.unsafeFromInt)

  def getURL(index: Int): ResultSet[URL] =
    effect(_.getURL(index))

  def getURL(colName: String): ResultSet[URL] =
    effect(_.getURL(colName))

  def getWarnings: ResultSet[sql.SQLWarning] = 
    effect(_.getWarnings)

  def insertRow: ResultSet[Unit] =
    effect(_.insertRow)

  def isAfterLast: ResultSet[Boolean] =
    effect(_.isAfterLast)

  def isBeforeFirst: ResultSet[Boolean] =
    effect(_.isBeforeFirst)

  def isFirst: ResultSet[Boolean] =
    effect(_.isFirst)

  def isLast: ResultSet[Boolean] =
    effect(_.isLast)

  def last: ResultSet[Boolean] =
    effect(_.last)

  def moveToCurrentRow: ResultSet[Unit] =
    effect(_.moveToCurrentRow)

  def moveToInsertRow: ResultSet[Unit] =
    effect(_.moveToInsertRow)

  def next: ResultSet[Boolean] =
    effect(_.next)

  def previous: ResultSet[Boolean] =
    effect(_.previous)

  def refreshRow: ResultSet[Unit] =
    effect(_.refreshRow)

  def relative(rows: Int): ResultSet[Boolean] =
    effect(_.relative(rows))

  def rowDeleted: ResultSet[Boolean] =
    effect(_.rowDeleted)

  def rowInserted: ResultSet[Boolean] =
    effect(_.rowInserted)

  def rowUpdated: ResultSet[Boolean] =
    effect(_.rowUpdated)

  def setFetchDirection(direction: FetchDirection): ResultSet[Unit] =
    effect(_.setFetchDirection(direction.toInt))

  def setFetchSize(rows: Int): ResultSet[Unit] =
    effect(_.setFetchSize(rows))

  def updateArray(index: Int, x: sql.Array): ResultSet[Unit] =
    effect(_.updateArray(index, x))

  def updateArray(colName: String, x: sql.Array): ResultSet[Unit] =
    effect(_.updateArray(colName, x))

  def updateAsciiStream(index: Int, x: InputStream, length: Int): ResultSet[Unit] =
    effect(_.updateAsciiStream(index, x, length))

  def updateAsciiStream(colName: String, x: InputStream, length: Int): ResultSet[Unit] =
    effect(_.updateAsciiStream(colName, x, length))

  def updateBigDecimal(index: Int, x: BigDecimal): ResultSet[Unit] =
    effect(_.updateBigDecimal(index, x.bigDecimal))

  def updateBigDecimal(colName: String, x: BigDecimal): ResultSet[Unit] =
    effect(_.updateBigDecimal(colName, x.bigDecimal))

  def updateBinaryStream(index: Int, x: InputStream, length: Int): ResultSet[Unit] =
    effect(_.updateBinaryStream(index, x, length))

  def updateBinaryStream(colName: String, x: InputStream, length: Int): ResultSet[Unit] =
    effect(_.updateBinaryStream(colName, x, length))

  def updateBlob(index: Int, x: Blob): ResultSet[Unit] =
    effect(_.updateBlob(index, x))

  def updateBlob(colName: String, x: Blob): ResultSet[Unit] =
    effect(_.updateBlob(colName, x))

  def updateBoolean(index: Int, x: Boolean): ResultSet[Unit] =
    effect(_.updateBoolean(index, x))

  def updateBoolean(colName: String, x: Boolean): ResultSet[Unit] =
    effect(_.updateBoolean(colName, x))

  def updateByte(index: Int, x: Byte): ResultSet[Unit] =
    effect(_.updateByte(index, x))

  def updateByte(colName: String, x: Byte): ResultSet[Unit] =
    effect(_.updateByte(colName, x))

  def updateBytes(index: Int, x: Array[Byte]): ResultSet[Unit] =
    effect(_.updateBytes(index, x))

  def updateBytes(colName: String, x: Array[Byte]): ResultSet[Unit] =
    effect(_.updateBytes(colName, x))

  def updateCharacterStream(index: Int, x: Reader, length: Int): ResultSet[Unit] =
    effect(_.updateCharacterStream(index, x, length))

  def updateCharacterStream(colName: String, reader: Reader, length: Int): ResultSet[Unit] =
    effect(_.updateCharacterStream(colName, reader, length))

  def updateClob(index: Int, x: Clob): ResultSet[Unit] =
    effect(_.updateClob(index, x))

  def updateClob(colName: String, x: Clob): ResultSet[Unit] =
    effect(_.updateClob(colName, x))

  def updateDate(index: Int, x: Date): ResultSet[Unit] =
    effect(_.updateDate(index, x))

  def updateDate(colName: String, x: Date): ResultSet[Unit] =
    effect(_.updateDate(colName, x))

  def updateDouble(index: Int, x: Double): ResultSet[Unit] =
    effect(_.updateDouble(index, x))

  def updateDouble(colName: String, x: Double): ResultSet[Unit] =
    effect(_.updateDouble(colName, x))

  def updateFloat(index: Int, x: Float): ResultSet[Unit] =
    effect(_.updateFloat(index, x))

  def updateFloat(colName: String, x: Float): ResultSet[Unit] =
    effect(_.updateFloat(colName, x))

  def updateInt(index: Int, x: Int): ResultSet[Unit] =
    effect(_.updateInt(index, x))

  def updateInt(colName: String, x: Int): ResultSet[Unit] =
    effect(_.updateInt(colName, x))

  def updateLong(index: Int, x: Long): ResultSet[Unit] =
    effect(_.updateLong(index, x))

  def updateLong(colName: String, x: Long): ResultSet[Unit] =
    effect(_.updateLong(colName, x))

  def updateNull(index: Int): ResultSet[Unit] =
    effect(_.updateNull(index))

  def updateNull(colName: String): ResultSet[Unit] =
    effect(_.updateNull(colName))

  def updateObject(index: Int, x: Object): ResultSet[Unit] =
    effect(_.updateObject(index, x))

  def updateObject(index: Int, x: Object, scale: Int): ResultSet[Unit] =
    effect(_.updateObject(index, x, scale))

  def updateObject(colName: String, x: Object): ResultSet[Unit] =
    effect(_.updateObject(colName, x))

  def updateObject(colName: String, x: Object, scale: Int): ResultSet[Unit] =
    effect(_.updateObject(colName, x, scale))

  def updateRef(index: Int, x: Ref): ResultSet[Unit] =
    effect(_.updateRef(index, x))

  def updateRef(colName: String, x: Ref): ResultSet[Unit] =
    effect(_.updateRef(colName, x))

  def updateRow: ResultSet[Unit] =
    effect(_.updateRow)

  def updateShort(index: Int, x: Short): ResultSet[Unit] =
    effect(_.updateShort(index, x))

  def updateShort(colName: String, x: Short): ResultSet[Unit] =
    effect(_.updateShort(colName, x))

  def updateString(index: Int, x: String): ResultSet[Unit] =
    effect(_.updateString(index, x))

  def updateString(colName: String, x: String): ResultSet[Unit] =
    effect(_.updateString(colName, x))

  def updateTime(index: Int, x: Time): ResultSet[Unit] =
    effect(_.updateTime(index, x))

  def updateTime(colName: String, x: Time): ResultSet[Unit] =
    effect(_.updateTime(colName, x))

  def updateTimestamp(index: Int, x: Timestamp): ResultSet[Unit] =
    effect(_.updateTimestamp(index, x))

  def updateTimestamp(colName: String, x: Timestamp): ResultSet[Unit] =
    effect(_.updateTimestamp(colName, x))

  def wasNull: ResultSet[Boolean] =
    effect(_.wasNull)

}