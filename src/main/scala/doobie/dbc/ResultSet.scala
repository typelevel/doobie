package doobie
package dbc

import scalaz._
import Scalaz._
import scala.collection.JavaConverters._
import scalaz.effect.IO
import java.sql
import sql.{ Date, Blob, Clob, Time, Timestamp, Ref, ResultSetMetaData }
import java.io.{ InputStream, Reader }
import java.net.URL
import java.util.{ Calendar }

object resultset extends DWorld[java.sql.ResultSet] {

  type ResultSet[+A] = Action[A]

  private[dbc] def run[A](a: ResultSet[A], l: Log[LogElement], s: sql.ResultSet): IO[A] = 
    eval(a, l, s).map(_._2)

  ////// ACTIONS, IN ALPHABETIC ORDER

  def absolute(row: Int): ResultSet[Boolean] =
    effect(s"absolute($row)", _.absolute(row))

  def afterLast: ResultSet[Unit] =
    effect(s"afterLast", _.afterLast)

  def beforeFirst: ResultSet[Unit] =
    effect(s"beforeFirst", _.beforeFirst)

  def cancelRowUpdates: ResultSet[Unit] =
    effect(s"cancelRowUpdates", _.cancelRowUpdates)

  def clearWarnings: ResultSet[Unit] =
    effect(s"clearWarnings", _.clearWarnings)

  def close: ResultSet[Unit] =
    effect(s"rs.close", _.close)

  def deleteRow: ResultSet[Unit] =
    effect(s"deleteRow", _.deleteRow)

  def findColumn(colName: String): ResultSet[Int] =
    effect(s"findColumn($colName)", _.findColumn(colName))

  def first: ResultSet[Boolean] =
    effect(s"first", _.first)

  def getArray(i: Int): ResultSet[sql.Array] =
    effect(s"getArray($i)", _.getArray(i))

  def getArray(colName: String): ResultSet[sql.Array] =
    effect(s"getArray($colName)", _.getArray(colName))

  def getAsciiStream(index: Int): ResultSet[InputStream] =
    effect(s"getAsciiStream($index)", _.getAsciiStream(index))

  def getAsciiStream(colName: String): ResultSet[InputStream] =
    effect(s"getAsciiStream($colName)", _.getAsciiStream(colName))

  def getBigDecimal(index: Int): ResultSet[BigDecimal] =
    effect(s"getBigDecimal($index)", _.getBigDecimal(index)).map(BigDecimal(_))

  @deprecated("Deprecated in JDBC", "0.1")
  def getBigDecimal(index: Int, scale: Int): ResultSet[BigDecimal] =
    effect(s"getBigDecimal($index, $scale)", _.getBigDecimal(index, scale))

  def getBigDecimal(colName: String): ResultSet[BigDecimal] =
    effect(s"getBigDecimal($colName)", _.getBigDecimal(colName)).map(BigDecimal(_))

  def getBinaryStream(index: Int): ResultSet[InputStream] =
    effect(s"getBinaryStream($index)", _.getBinaryStream(index))

  def getBinaryStream(colName: String): ResultSet[InputStream] =
    effect(s"getBinaryStream($colName)", _.getBinaryStream(colName))

  def getBlob(i: Int): ResultSet[Blob] =
    effect(s"getBlob($i)", _.getBlob(i))

  def getBlob(colName: String): ResultSet[Blob] =
    effect(s"getBlob($colName)", _.getBlob(colName))

  def getBoolean(index: Int): ResultSet[Boolean] =
    effect(s"getBoolean($index)", _.getBoolean(index))

  def getBoolean(colName: String): ResultSet[Boolean] =
    effect(s"getBoolean($colName)", _.getBoolean(colName))

  def getByte(index: Int): ResultSet[Byte] =
    effect(s"getByte($index)", _.getByte(index))

  def getByte(colName: String): ResultSet[Byte] =
    effect(s"getByte($colName)", _.getByte(colName))

  def getBytes(index: Int): ResultSet[Array[Byte]] =
    effect(s"getBytes($index)", _.getBytes(index))

  def getBytes(colName: String): ResultSet[Array[Byte]] =
    effect(s"getBytes($colName)", _.getBytes(colName))

  def getCharacterStream(index: Int): ResultSet[Reader] =
    effect(s"getCharacterStream($index)", _.getCharacterStream(index))

  def getCharacterStream(colName: String): ResultSet[Reader] =
    effect(s"getCharacterStream($colName)", _.getCharacterStream(colName))

  def getClob(i: Int): ResultSet[Clob] =
    effect(s"getClob($i)", _.getClob(i))

  def getClob(colName: String): ResultSet[Clob] =
    effect(s"getClob($colName)", _.getClob(colName))

  def getConcurrency: ResultSet[ResultSetConcurrency] =
    effect(s"getConcurrency", _.getConcurrency).map(ResultSetConcurrency.unsafeFromInt)

  def getCursorName: ResultSet[String] =
    effect(s"getCursorName", _.getCursorName)

  def getDate(index: Int): ResultSet[Date] =
    effect(s"getDate($index)", _.getDate(index))

  def getDate(index: Int, cal: Calendar): ResultSet[Date] =
    effect(s"getDate($index, $cal)", _.getDate(index, cal))

  def getDate(colName: String): ResultSet[Date] =
    effect(s"getDate($colName)", _.getDate(colName))

  def getDate(colName: String, cal: Calendar): ResultSet[Date] =
    effect(s"getDate($colName, $cal)", _.getDate(colName, cal))

  def getDouble(index: Int): ResultSet[Double] =
    effect(s"getDouble($index)", _.getDouble(index))

  def getDouble(colName: String): ResultSet[Double] =
    effect(s"getDouble($colName)", _.getDouble(colName))

  def getFetchDirection: ResultSet[FetchDirection] =
    effect(s"getFetchDirection", _.getFetchDirection).map(FetchDirection.unsafeFromInt)

  def getFetchSize: ResultSet[Int] =
    effect(s"getFetchSize", _.getFetchSize)

  def getFloat(index: Int): ResultSet[Float] =
    effect(s"getFloat($index)", _.getFloat(index))

  def getFloat(colName: String): ResultSet[Float] =
    effect(s"getFloat($colName)", _.getFloat(colName))

  def getInt(index: Int): ResultSet[Int] =
    effect(s"getInt($index)", _.getInt(index))

  def getInt(colName: String): ResultSet[Int] =
    effect(s"getInt($colName)", _.getInt(colName))

  def getLong(index: Int): ResultSet[Long] =
    effect(s"getLong($index)", _.getLong(index))

  def getLong(colName: String): ResultSet[Long] =
    effect(s"getLong($colName)", _.getLong(colName))

  def getMetaData: ResultSet[ResultSetMetaData] =
    ???

  def getObject(index: Int): ResultSet[Object] =
    effect(s"getObject($index)", _.getObject(index))

  def getObject(i: Int, map: Map[String, Class[_]]): ResultSet[Object] =
    effect(s"getObject($i, $map)", _.getObject(i, map.asJava))

  def getObject(colName: String): ResultSet[Object] =
    effect(s"getObject($colName)", _.getObject(colName))

  def getObject(colName: String, map: Map[String, Class[_]]): ResultSet[Object] =
    effect(s"getObject($colName, $map)", _.getObject(colName, map.asJava))

  def getRef(i: Int): ResultSet[Ref] =
    effect(s"getRef($i)", _.getRef(i))

  def getRef(colName: String): ResultSet[Ref] =
    effect(s"getRef($colName)", _.getRef(colName))

  def getRow: ResultSet[Int] =
    effect(s"getRow", _.getRow)

  def getShort(index: Int): ResultSet[Short] =
    effect(s"getShort($index)", _.getShort(index))

  def getShort(colName: String): ResultSet[Short] =
    effect(s"getShort($colName)", _.getShort(colName))

  def getStatement[A](k: Statement[A]): ResultSet[A] =
    for {
      l <- log
      s <- effect(s"getStatement", _.getStatement)
      a <- statement.run(k, l, s).liftIO[ResultSet]
    } yield a
    
  def getString(index: Int): ResultSet[String] =
    effect(s"getString($index)", _.getString(index))

  def getString(colName: String): ResultSet[String] =
    effect(s"getString($colName)", _.getString(colName))

  def getTime(index: Int): ResultSet[Time] =
    effect(s"getTime($index)", _.getTime(index))

  def getTime(index: Int, cal: Calendar): ResultSet[Time] =
    effect(s"getTime($index, $cal)", _.getTime(index, cal))

  def getTime(colName: String): ResultSet[Time] =
    effect(s"getTime($colName)", _.getTime(colName))

  def getTime(colName: String, cal: Calendar): ResultSet[Time] =
    effect(s"getTime($colName, $cal)", _.getTime(colName, cal))

  def getTimestamp(index: Int): ResultSet[Timestamp] =
    effect(s"getTimestamp($index)", _.getTimestamp(index))

  def getTimestamp(index: Int, cal: Calendar): ResultSet[Timestamp] =
    effect(s"getTimestamp($index, $cal)", _.getTimestamp(index, cal))

  def getTimestamp(colName: String): ResultSet[Timestamp] =
    effect(s"getTimestamp($colName)", _.getTimestamp(colName))

  def getTimestamp(colName: String, cal: Calendar): ResultSet[Timestamp] =
    effect(s"getTimestamp($colName, $cal)", _.getTimestamp(colName, cal))

  def getType: ResultSet[ResultSetType] =
    effect(s"getType", _.getType).map(ResultSetType.unsafeFromInt)

  def getURL(index: Int): ResultSet[URL] =
    effect(s"getURL($index)", _.getURL(index))

  def getURL(colName: String): ResultSet[URL] =
    effect(s"getURL($colName)", _.getURL(colName))

  def getWarnings: ResultSet[sql.SQLWarning] = 
    effect(s"getWarnings", _.getWarnings)

  def insertRow: ResultSet[Unit] =
    effect(s"insertRow", _.insertRow)

  def isAfterLast: ResultSet[Boolean] =
    effect(s"isAfterLast", _.isAfterLast)

  def isBeforeFirst: ResultSet[Boolean] =
    effect(s"isBeforeFirst", _.isBeforeFirst)

  def isFirst: ResultSet[Boolean] =
    effect(s"isFirst", _.isFirst)

  def isLast: ResultSet[Boolean] =
    effect(s"isLast", _.isLast)

  def last: ResultSet[Boolean] =
    effect(s"last", _.last)

  def moveToCurrentRow: ResultSet[Unit] =
    effect(s"moveToCurrentRow", _.moveToCurrentRow)

  def moveToInsertRow: ResultSet[Unit] =
    effect(s"moveToInsertRow", _.moveToInsertRow)

  def next: ResultSet[Boolean] =
    effect(s"next", _.next)

  def previous: ResultSet[Boolean] =
    effect(s"previous", _.previous)

  def refreshRow: ResultSet[Unit] =
    effect(s"refreshRow", _.refreshRow)

  def relative(rows: Int): ResultSet[Boolean] =
    effect(s"relative($rows)", _.relative(rows))

  def rowDeleted: ResultSet[Boolean] =
    effect(s"rowDeleted", _.rowDeleted)

  def rowInserted: ResultSet[Boolean] =
    effect(s"rowInserted", _.rowInserted)

  def rowUpdated: ResultSet[Boolean] =
    effect(s"rowUpdated", _.rowUpdated)

  def setFetchDirection(direction: FetchDirection): ResultSet[Unit] =
    effect(s"setFetchDirection($direction.toInt)", _.setFetchDirection(direction.toInt))

  def setFetchSize(rows: Int): ResultSet[Unit] =
    effect(s"setFetchSize($rows)", _.setFetchSize(rows))

  def updateArray(index: Int, x: sql.Array): ResultSet[Unit] =
    effect(s"updateArray($index, $$x)", _.updateArray(index, x))

  def updateArray(colName: String, x: sql.Array): ResultSet[Unit] =
    effect(s"updateArray($colName, $$x)", _.updateArray(colName, x))

  def updateAsciiStream(index: Int, x: InputStream, length: Int): ResultSet[Unit] =
    effect(s"updateAsciiStream($index, $x, $length)", _.updateAsciiStream(index, x, length))

  def updateAsciiStream(colName: String, x: InputStream, length: Int): ResultSet[Unit] =
    effect(s"updateAsciiStream($colName, $x, $length)", _.updateAsciiStream(colName, x, length))

  def updateBigDecimal(index: Int, x: BigDecimal): ResultSet[Unit] =
    effect(s"updateBigDecimal($index, $$x)", _.updateBigDecimal(index, x.bigDecimal))

  def updateBigDecimal(colName: String, x: BigDecimal): ResultSet[Unit] =
    effect(s"updateBigDecimal($colName, $$x)", _.updateBigDecimal(colName, x.bigDecimal))

  def updateBinaryStream(index: Int, x: InputStream, length: Int): ResultSet[Unit] =
    effect(s"updateBinaryStream($index, $x, $length)", _.updateBinaryStream(index, x, length))

  def updateBinaryStream(colName: String, x: InputStream, length: Int): ResultSet[Unit] =
    effect(s"updateBinaryStream($colName, $x, $length)", _.updateBinaryStream(colName, x, length))

  def updateBlob(index: Int, x: Blob): ResultSet[Unit] =
    effect(s"updateBlob($index, $$x)", _.updateBlob(index, x))

  def updateBlob(colName: String, x: Blob): ResultSet[Unit] =
    effect(s"updateBlob($colName, $$x)", _.updateBlob(colName, x))

  def updateBoolean(index: Int, x: Boolean): ResultSet[Unit] =
    effect(s"updateBoolean($index, $$x)", _.updateBoolean(index, x))

  def updateBoolean(colName: String, x: Boolean): ResultSet[Unit] =
    effect(s"updateBoolean($colName, $$x)", _.updateBoolean(colName, x))

  def updateByte(index: Int, x: Byte): ResultSet[Unit] =
    effect(s"updateByte($index, $$x)", _.updateByte(index, x))

  def updateByte(colName: String, x: Byte): ResultSet[Unit] =
    effect(s"updateByte($colName, $$x)", _.updateByte(colName, x))

  def updateBytes(index: Int, x: Array[Byte]): ResultSet[Unit] =
    effect(s"updateBytes($index, $$x)", _.updateBytes(index, x))

  def updateBytes(colName: String, x: Array[Byte]): ResultSet[Unit] =
    effect(s"updateBytes($colName, $$x)", _.updateBytes(colName, x))

  def updateCharacterStream(index: Int, x: Reader, length: Int): ResultSet[Unit] =
    effect(s"updateCharacterStream($index, $x, $length)", _.updateCharacterStream(index, x, length))

  def updateCharacterStream(colName: String, reader: Reader, length: Int): ResultSet[Unit] =
    effect(s"updateCharacterStream($colName, $reader, $length)", _.updateCharacterStream(colName, reader, length))

  def updateClob(index: Int, x: Clob): ResultSet[Unit] =
    effect(s"updateClob($index, $$x)", _.updateClob(index, x))

  def updateClob(colName: String, x: Clob): ResultSet[Unit] =
    effect(s"updateClob($colName, $$x)", _.updateClob(colName, x))

  def updateDate(index: Int, x: Date): ResultSet[Unit] =
    effect(s"updateDate($index, $$x)", _.updateDate(index, x))

  def updateDate(colName: String, x: Date): ResultSet[Unit] =
    effect(s"updateDate($colName, $$x)", _.updateDate(colName, x))

  def updateDouble(index: Int, x: Double): ResultSet[Unit] =
    effect(s"updateDouble($index, $$x)", _.updateDouble(index, x))

  def updateDouble(colName: String, x: Double): ResultSet[Unit] =
    effect(s"updateDouble($colName, $$x)", _.updateDouble(colName, x))

  def updateFloat(index: Int, x: Float): ResultSet[Unit] =
    effect(s"updateFloat($index, $$x)", _.updateFloat(index, x))

  def updateFloat(colName: String, x: Float): ResultSet[Unit] =
    effect(s"updateFloat($colName, $$x)", _.updateFloat(colName, x))

  def updateInt(index: Int, x: Int): ResultSet[Unit] =
    effect(s"updateInt($index, $x)", _.updateInt(index, x))

  def updateInt(colName: String, x: Int): ResultSet[Unit] =
    effect(s"updateInt($colName, $x)", _.updateInt(colName, x))

  def updateLong(index: Int, x: Long): ResultSet[Unit] =
    effect(s"updateLong($index, $x)", _.updateLong(index, x))

  def updateLong(colName: String, x: Long): ResultSet[Unit] =
    effect(s"updateLong($colName, $x)", _.updateLong(colName, x))

  def updateNull(index: Int): ResultSet[Unit] =
    effect(s"updateNull($index)", _.updateNull(index))

  def updateNull(colName: String): ResultSet[Unit] =
    effect(s"updateNull($colName)", _.updateNull(colName))

  def updateObject(index: Int, x: Object): ResultSet[Unit] =
    effect(s"updateObject($index, $x)", _.updateObject(index, x))

  def updateObject(index: Int, x: Object, scale: Int): ResultSet[Unit] =
    effect(s"updateObject($index, $x, $scale)", _.updateObject(index, x, scale))

  def updateObject(colName: String, x: Object): ResultSet[Unit] =
    effect(s"updateObject($colName, $x)", _.updateObject(colName, x))

  def updateObject(colName: String, x: Object, scale: Int): ResultSet[Unit] =
    effect(s"updateObject($colName, $x, $scale)", _.updateObject(colName, x, scale))

  def updateRef(index: Int, x: Ref): ResultSet[Unit] =
    effect(s"updateRef($index, $x)", _.updateRef(index, x))

  def updateRef(colName: String, x: Ref): ResultSet[Unit] =
    effect(s"updateRef($colName, $x)", _.updateRef(colName, x))

  def updateRow: ResultSet[Unit] =
    effect(s"updateRow", _.updateRow)

  def updateShort(index: Int, x: Short): ResultSet[Unit] =
    effect(s"updateShort($index, $x)", _.updateShort(index, x))

  def updateShort(colName: String, x: Short): ResultSet[Unit] =
    effect(s"updateShort($colName, $x)", _.updateShort(colName, x))

  def updateString(index: Int, x: String): ResultSet[Unit] =
    effect(s"updateString($index, $x)", _.updateString(index, x))

  def updateString(colName: String, x: String): ResultSet[Unit] =
    effect(s"updateString($colName, $x)", _.updateString(colName, x))

  def updateTime(index: Int, x: Time): ResultSet[Unit] =
    effect(s"updateTime($index, $x)", _.updateTime(index, x))

  def updateTime(colName: String, x: Time): ResultSet[Unit] =
    effect(s"updateTime($colName, $x)", _.updateTime(colName, x))

  def updateTimestamp(index: Int, x: Timestamp): ResultSet[Unit] =
    effect(s"updateTimestamp($index, $x)", _.updateTimestamp(index, x))

  def updateTimestamp(colName: String, x: Timestamp): ResultSet[Unit] =
    effect(s"updateTimestamp($colName, $x)", _.updateTimestamp(colName, x))

  def wasNull: ResultSet[Boolean] =
    effect(s"wasNull", _.wasNull)

}