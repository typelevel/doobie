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
    primitive(s"absolute($row)", _.absolute(row))

  def afterLast: ResultSet[Unit] =
    primitive(s"afterLast", _.afterLast)

  def beforeFirst: ResultSet[Unit] =
    primitive(s"beforeFirst", _.beforeFirst)

  def cancelRowUpdates: ResultSet[Unit] =
    primitive(s"cancelRowUpdates", _.cancelRowUpdates)

  def clearWarnings: ResultSet[Unit] =
    primitive(s"clearWarnings", _.clearWarnings)

  def close: ResultSet[Unit] =
    primitive(s"rs.close", _.close)

  def deleteRow: ResultSet[Unit] =
    primitive(s"deleteRow", _.deleteRow)

  def findColumn(colName: String): ResultSet[Int] =
    primitive(s"findColumn($colName)", _.findColumn(colName))

  def first: ResultSet[Boolean] =
    primitive(s"first", _.first)

  def getArray(i: Int): ResultSet[sql.Array] =
    primitive(s"getArray($i)", _.getArray(i))

  def getArray(colName: String): ResultSet[sql.Array] =
    primitive(s"getArray($colName)", _.getArray(colName))

  def getAsciiStream(index: Int): ResultSet[InputStream] =
    primitive(s"getAsciiStream($index)", _.getAsciiStream(index))

  def getAsciiStream(colName: String): ResultSet[InputStream] =
    primitive(s"getAsciiStream($colName)", _.getAsciiStream(colName))

  def getBigDecimal(index: Int): ResultSet[BigDecimal] =
    primitive(s"getBigDecimal($index)", _.getBigDecimal(index)).map(BigDecimal(_))

  @deprecated("Deprecated in JDBC", "0.1")
  def getBigDecimal(index: Int, scale: Int): ResultSet[BigDecimal] =
    primitive(s"getBigDecimal($index, $scale)", _.getBigDecimal(index, scale))

  def getBigDecimal(colName: String): ResultSet[BigDecimal] =
    primitive(s"getBigDecimal($colName)", _.getBigDecimal(colName)).map(BigDecimal(_))

  def getBinaryStream(index: Int): ResultSet[InputStream] =
    primitive(s"getBinaryStream($index)", _.getBinaryStream(index))

  def getBinaryStream(colName: String): ResultSet[InputStream] =
    primitive(s"getBinaryStream($colName)", _.getBinaryStream(colName))

  def getBlob(i: Int): ResultSet[Blob] =
    primitive(s"getBlob($i)", _.getBlob(i))

  def getBlob(colName: String): ResultSet[Blob] =
    primitive(s"getBlob($colName)", _.getBlob(colName))

  def getBoolean(index: Int): ResultSet[Boolean] =
    primitive(s"getBoolean($index)", _.getBoolean(index))

  def getBoolean(colName: String): ResultSet[Boolean] =
    primitive(s"getBoolean($colName)", _.getBoolean(colName))

  def getByte(index: Int): ResultSet[Byte] =
    primitive(s"getByte($index)", _.getByte(index))

  def getByte(colName: String): ResultSet[Byte] =
    primitive(s"getByte($colName)", _.getByte(colName))

  def getBytes(index: Int): ResultSet[Array[Byte]] =
    primitive(s"getBytes($index)", _.getBytes(index))

  def getBytes(colName: String): ResultSet[Array[Byte]] =
    primitive(s"getBytes($colName)", _.getBytes(colName))

  def getCharacterStream(index: Int): ResultSet[Reader] =
    primitive(s"getCharacterStream($index)", _.getCharacterStream(index))

  def getCharacterStream(colName: String): ResultSet[Reader] =
    primitive(s"getCharacterStream($colName)", _.getCharacterStream(colName))

  def getClob(i: Int): ResultSet[Clob] =
    primitive(s"getClob($i)", _.getClob(i))

  def getClob(colName: String): ResultSet[Clob] =
    primitive(s"getClob($colName)", _.getClob(colName))

  def getConcurrency: ResultSet[ResultSetConcurrency] =
    primitive(s"getConcurrency", _.getConcurrency).map(ResultSetConcurrency.unsafeFromInt)

  def getCursorName: ResultSet[String] =
    primitive(s"getCursorName", _.getCursorName)

  def getDate(index: Int): ResultSet[Date] =
    primitive(s"getDate($index)", _.getDate(index))

  def getDate(index: Int, cal: Calendar): ResultSet[Date] =
    primitive(s"getDate($index, $cal)", _.getDate(index, cal))

  def getDate(colName: String): ResultSet[Date] =
    primitive(s"getDate($colName)", _.getDate(colName))

  def getDate(colName: String, cal: Calendar): ResultSet[Date] =
    primitive(s"getDate($colName, $cal)", _.getDate(colName, cal))

  def getDouble(index: Int): ResultSet[Double] =
    primitive(s"getDouble($index)", _.getDouble(index))

  def getDouble(colName: String): ResultSet[Double] =
    primitive(s"getDouble($colName)", _.getDouble(colName))

  def getFetchDirection: ResultSet[FetchDirection] =
    primitive(s"getFetchDirection", _.getFetchDirection).map(FetchDirection.unsafeFromInt)

  def getFetchSize: ResultSet[Int] =
    primitive(s"getFetchSize", _.getFetchSize)

  def getFloat(index: Int): ResultSet[Float] =
    primitive(s"getFloat($index)", _.getFloat(index))

  def getFloat(colName: String): ResultSet[Float] =
    primitive(s"getFloat($colName)", _.getFloat(colName))

  def getInt(index: Int): ResultSet[Int] =
    primitive(s"getInt($index)", _.getInt(index))

  def getInt(colName: String): ResultSet[Int] =
    primitive(s"getInt($colName)", _.getInt(colName))

  def getLong(index: Int): ResultSet[Long] =
    primitive(s"getLong($index)", _.getLong(index))

  def getLong(colName: String): ResultSet[Long] =
    primitive(s"getLong($colName)", _.getLong(colName))

  def getMetaData: ResultSet[ResultSetMetaData] =
    ???

  def getObject(index: Int): ResultSet[Object] =
    primitive(s"getObject($index)", _.getObject(index))

  def getObject(i: Int, map: Map[String, Class[_]]): ResultSet[Object] =
    primitive(s"getObject($i, $map)", _.getObject(i, map.asJava))

  def getObject(colName: String): ResultSet[Object] =
    primitive(s"getObject($colName)", _.getObject(colName))

  def getObject(colName: String, map: Map[String, Class[_]]): ResultSet[Object] =
    primitive(s"getObject($colName, $map)", _.getObject(colName, map.asJava))

  def getRef(i: Int): ResultSet[Ref] =
    primitive(s"getRef($i)", _.getRef(i))

  def getRef(colName: String): ResultSet[Ref] =
    primitive(s"getRef($colName)", _.getRef(colName))

  def getRow: ResultSet[Int] =
    primitive(s"getRow", _.getRow)

  def getShort(index: Int): ResultSet[Short] =
    primitive(s"getShort($index)", _.getShort(index))

  def getShort(colName: String): ResultSet[Short] =
    primitive(s"getShort($colName)", _.getShort(colName))

  def getStatement[A](k: Statement[A]): ResultSet[A] =
    for {
      l <- log
      s <- primitive(s"getStatement", _.getStatement)
      a <- statement.run(k, l, s).liftIO[ResultSet]
    } yield a
    
  def getString(index: Int): ResultSet[String] =
    primitive(s"getString($index)", _.getString(index))

  def getString(colName: String): ResultSet[String] =
    primitive(s"getString($colName)", _.getString(colName))

  def getTime(index: Int): ResultSet[Time] =
    primitive(s"getTime($index)", _.getTime(index))

  def getTime(index: Int, cal: Calendar): ResultSet[Time] =
    primitive(s"getTime($index, $cal)", _.getTime(index, cal))

  def getTime(colName: String): ResultSet[Time] =
    primitive(s"getTime($colName)", _.getTime(colName))

  def getTime(colName: String, cal: Calendar): ResultSet[Time] =
    primitive(s"getTime($colName, $cal)", _.getTime(colName, cal))

  def getTimestamp(index: Int): ResultSet[Timestamp] =
    primitive(s"getTimestamp($index)", _.getTimestamp(index))

  def getTimestamp(index: Int, cal: Calendar): ResultSet[Timestamp] =
    primitive(s"getTimestamp($index, $cal)", _.getTimestamp(index, cal))

  def getTimestamp(colName: String): ResultSet[Timestamp] =
    primitive(s"getTimestamp($colName)", _.getTimestamp(colName))

  def getTimestamp(colName: String, cal: Calendar): ResultSet[Timestamp] =
    primitive(s"getTimestamp($colName, $cal)", _.getTimestamp(colName, cal))

  def getType: ResultSet[ResultSetType] =
    primitive(s"getType", _.getType).map(ResultSetType.unsafeFromInt)

  def getURL(index: Int): ResultSet[URL] =
    primitive(s"getURL($index)", _.getURL(index))

  def getURL(colName: String): ResultSet[URL] =
    primitive(s"getURL($colName)", _.getURL(colName))

  def getWarnings: ResultSet[sql.SQLWarning] = 
    primitive(s"getWarnings", _.getWarnings)

  def insertRow: ResultSet[Unit] =
    primitive(s"insertRow", _.insertRow)

  def isAfterLast: ResultSet[Boolean] =
    primitive(s"isAfterLast", _.isAfterLast)

  def isBeforeFirst: ResultSet[Boolean] =
    primitive(s"isBeforeFirst", _.isBeforeFirst)

  def isFirst: ResultSet[Boolean] =
    primitive(s"isFirst", _.isFirst)

  def isLast: ResultSet[Boolean] =
    primitive(s"isLast", _.isLast)

  def last: ResultSet[Boolean] =
    primitive(s"last", _.last)

  def moveToCurrentRow: ResultSet[Unit] =
    primitive(s"moveToCurrentRow", _.moveToCurrentRow)

  def moveToInsertRow: ResultSet[Unit] =
    primitive(s"moveToInsertRow", _.moveToInsertRow)

  def next: ResultSet[Boolean] =
    primitive(s"next", _.next)

  def previous: ResultSet[Boolean] =
    primitive(s"previous", _.previous)

  def refreshRow: ResultSet[Unit] =
    primitive(s"refreshRow", _.refreshRow)

  def relative(rows: Int): ResultSet[Boolean] =
    primitive(s"relative($rows)", _.relative(rows))

  def rowDeleted: ResultSet[Boolean] =
    primitive(s"rowDeleted", _.rowDeleted)

  def rowInserted: ResultSet[Boolean] =
    primitive(s"rowInserted", _.rowInserted)

  def rowUpdated: ResultSet[Boolean] =
    primitive(s"rowUpdated", _.rowUpdated)

  def setFetchDirection(direction: FetchDirection): ResultSet[Unit] =
    primitive(s"setFetchDirection($direction.toInt)", _.setFetchDirection(direction.toInt))

  def setFetchSize(rows: Int): ResultSet[Unit] =
    primitive(s"setFetchSize($rows)", _.setFetchSize(rows))

  def updateArray(index: Int, x: sql.Array): ResultSet[Unit] =
    primitive(s"updateArray($index, $$x)", _.updateArray(index, x))

  def updateArray(colName: String, x: sql.Array): ResultSet[Unit] =
    primitive(s"updateArray($colName, $$x)", _.updateArray(colName, x))

  def updateAsciiStream(index: Int, x: InputStream, length: Int): ResultSet[Unit] =
    primitive(s"updateAsciiStream($index, $x, $length)", _.updateAsciiStream(index, x, length))

  def updateAsciiStream(colName: String, x: InputStream, length: Int): ResultSet[Unit] =
    primitive(s"updateAsciiStream($colName, $x, $length)", _.updateAsciiStream(colName, x, length))

  def updateBigDecimal(index: Int, x: BigDecimal): ResultSet[Unit] =
    primitive(s"updateBigDecimal($index, $$x)", _.updateBigDecimal(index, x.bigDecimal))

  def updateBigDecimal(colName: String, x: BigDecimal): ResultSet[Unit] =
    primitive(s"updateBigDecimal($colName, $$x)", _.updateBigDecimal(colName, x.bigDecimal))

  def updateBinaryStream(index: Int, x: InputStream, length: Int): ResultSet[Unit] =
    primitive(s"updateBinaryStream($index, $x, $length)", _.updateBinaryStream(index, x, length))

  def updateBinaryStream(colName: String, x: InputStream, length: Int): ResultSet[Unit] =
    primitive(s"updateBinaryStream($colName, $x, $length)", _.updateBinaryStream(colName, x, length))

  def updateBlob(index: Int, x: Blob): ResultSet[Unit] =
    primitive(s"updateBlob($index, $$x)", _.updateBlob(index, x))

  def updateBlob(colName: String, x: Blob): ResultSet[Unit] =
    primitive(s"updateBlob($colName, $$x)", _.updateBlob(colName, x))

  def updateBoolean(index: Int, x: Boolean): ResultSet[Unit] =
    primitive(s"updateBoolean($index, $$x)", _.updateBoolean(index, x))

  def updateBoolean(colName: String, x: Boolean): ResultSet[Unit] =
    primitive(s"updateBoolean($colName, $$x)", _.updateBoolean(colName, x))

  def updateByte(index: Int, x: Byte): ResultSet[Unit] =
    primitive(s"updateByte($index, $$x)", _.updateByte(index, x))

  def updateByte(colName: String, x: Byte): ResultSet[Unit] =
    primitive(s"updateByte($colName, $$x)", _.updateByte(colName, x))

  def updateBytes(index: Int, x: Array[Byte]): ResultSet[Unit] =
    primitive(s"updateBytes($index, $$x)", _.updateBytes(index, x))

  def updateBytes(colName: String, x: Array[Byte]): ResultSet[Unit] =
    primitive(s"updateBytes($colName, $$x)", _.updateBytes(colName, x))

  def updateCharacterStream(index: Int, x: Reader, length: Int): ResultSet[Unit] =
    primitive(s"updateCharacterStream($index, $x, $length)", _.updateCharacterStream(index, x, length))

  def updateCharacterStream(colName: String, reader: Reader, length: Int): ResultSet[Unit] =
    primitive(s"updateCharacterStream($colName, $reader, $length)", _.updateCharacterStream(colName, reader, length))

  def updateClob(index: Int, x: Clob): ResultSet[Unit] =
    primitive(s"updateClob($index, $$x)", _.updateClob(index, x))

  def updateClob(colName: String, x: Clob): ResultSet[Unit] =
    primitive(s"updateClob($colName, $$x)", _.updateClob(colName, x))

  def updateDate(index: Int, x: Date): ResultSet[Unit] =
    primitive(s"updateDate($index, $$x)", _.updateDate(index, x))

  def updateDate(colName: String, x: Date): ResultSet[Unit] =
    primitive(s"updateDate($colName, $$x)", _.updateDate(colName, x))

  def updateDouble(index: Int, x: Double): ResultSet[Unit] =
    primitive(s"updateDouble($index, $$x)", _.updateDouble(index, x))

  def updateDouble(colName: String, x: Double): ResultSet[Unit] =
    primitive(s"updateDouble($colName, $$x)", _.updateDouble(colName, x))

  def updateFloat(index: Int, x: Float): ResultSet[Unit] =
    primitive(s"updateFloat($index, $$x)", _.updateFloat(index, x))

  def updateFloat(colName: String, x: Float): ResultSet[Unit] =
    primitive(s"updateFloat($colName, $$x)", _.updateFloat(colName, x))

  def updateInt(index: Int, x: Int): ResultSet[Unit] =
    primitive(s"updateInt($index, $x)", _.updateInt(index, x))

  def updateInt(colName: String, x: Int): ResultSet[Unit] =
    primitive(s"updateInt($colName, $x)", _.updateInt(colName, x))

  def updateLong(index: Int, x: Long): ResultSet[Unit] =
    primitive(s"updateLong($index, $x)", _.updateLong(index, x))

  def updateLong(colName: String, x: Long): ResultSet[Unit] =
    primitive(s"updateLong($colName, $x)", _.updateLong(colName, x))

  def updateNull(index: Int): ResultSet[Unit] =
    primitive(s"updateNull($index)", _.updateNull(index))

  def updateNull(colName: String): ResultSet[Unit] =
    primitive(s"updateNull($colName)", _.updateNull(colName))

  def updateObject(index: Int, x: Object): ResultSet[Unit] =
    primitive(s"updateObject($index, $x)", _.updateObject(index, x))

  def updateObject(index: Int, x: Object, scale: Int): ResultSet[Unit] =
    primitive(s"updateObject($index, $x, $scale)", _.updateObject(index, x, scale))

  def updateObject(colName: String, x: Object): ResultSet[Unit] =
    primitive(s"updateObject($colName, $x)", _.updateObject(colName, x))

  def updateObject(colName: String, x: Object, scale: Int): ResultSet[Unit] =
    primitive(s"updateObject($colName, $x, $scale)", _.updateObject(colName, x, scale))

  def updateRef(index: Int, x: Ref): ResultSet[Unit] =
    primitive(s"updateRef($index, $x)", _.updateRef(index, x))

  def updateRef(colName: String, x: Ref): ResultSet[Unit] =
    primitive(s"updateRef($colName, $x)", _.updateRef(colName, x))

  def updateRow: ResultSet[Unit] =
    primitive(s"updateRow", _.updateRow)

  def updateShort(index: Int, x: Short): ResultSet[Unit] =
    primitive(s"updateShort($index, $x)", _.updateShort(index, x))

  def updateShort(colName: String, x: Short): ResultSet[Unit] =
    primitive(s"updateShort($colName, $x)", _.updateShort(colName, x))

  def updateString(index: Int, x: String): ResultSet[Unit] =
    primitive(s"updateString($index, $x)", _.updateString(index, x))

  def updateString(colName: String, x: String): ResultSet[Unit] =
    primitive(s"updateString($colName, $x)", _.updateString(colName, x))

  def updateTime(index: Int, x: Time): ResultSet[Unit] =
    primitive(s"updateTime($index, $x)", _.updateTime(index, x))

  def updateTime(colName: String, x: Time): ResultSet[Unit] =
    primitive(s"updateTime($colName, $x)", _.updateTime(colName, x))

  def updateTimestamp(index: Int, x: Timestamp): ResultSet[Unit] =
    primitive(s"updateTimestamp($index, $x)", _.updateTimestamp(index, x))

  def updateTimestamp(colName: String, x: Timestamp): ResultSet[Unit] =
    primitive(s"updateTimestamp($colName, $x)", _.updateTimestamp(colName, x))

  def wasNull: ResultSet[Boolean] =
    primitive(s"wasNull", _.wasNull)

}