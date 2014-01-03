package doobie
package dbc

import scalaz.effect.IO
import java.sql
import java.sql.{ Blob, Clob, NClob, Date, Time, Timestamp, Ref, RowId, SQLXML }
import java.io.{ Reader, InputStream }
import java.util.Calendar
import java.net.URL
import scala.collection.JavaConverters._

object callablestatement extends util.TWorld[sql.CallableStatement] with PreparedStatementOps[sql.CallableStatement] {

  type CallableStatement[+A] = Action[A]

  private[dbc] def run[A](a: CallableStatement[A], s: sql.CallableStatement): IO[A] = 
    eval(a, s).map(_._2)
  
  ////// ACTIONS, IN ALPHABETIC ORDER

  def getArray(index: Int): CallableStatement[sql.Array] =
    effect(_.getArray(index))

  def getArray(name: String): CallableStatement[sql.Array] =
    effect(_.getArray(name))

  def getBigDecimal(index: Int): CallableStatement[BigDecimal] =
    effect(_.getBigDecimal(index))

  @deprecated("Deprecated in JDBC", "0.1")
  def getBigDecimal(index: Int, scale: Int): CallableStatement[BigDecimal] =
    effect(_.getBigDecimal(index, scale))

  def getBigDecimal(name: String): CallableStatement[BigDecimal] =
    effect(_.getBigDecimal(name))

  def getBlob(index: Int): CallableStatement[Blob] =
    effect(_.getBlob(index))

  def getBlob(name: String): CallableStatement[Blob] =
    effect(_.getBlob(name))

  def getBoolean(index: Int): CallableStatement[Boolean] =
    effect(_.getBoolean(index))

  def getBoolean(name: String): CallableStatement[Boolean] =
    effect(_.getBoolean(name))

  def getByte(index: Int): CallableStatement[Byte] =
    effect(_.getByte(index))

  def getByte(name: String): CallableStatement[Byte] =
    effect(_.getByte(name))

  def getBytes(index: Int): CallableStatement[Array[Byte]] =
    effect(_.getBytes(index))

  def getBytes(name: String): CallableStatement[Array[Byte]] =
    effect(_.getBytes(name))

  def getCharacterStream(index: Int): CallableStatement[Reader] =
    effect(_.getCharacterStream(index))

  def getCharacterStream(name: String): CallableStatement[Reader] =
    effect(_.getCharacterStream(name))

  def getClob(index: Int): CallableStatement[Clob] =
    effect(_.getClob(index))

  def getClob(name: String): CallableStatement[Clob] =
    effect(_.getClob(name))

  def getDate(index: Int): CallableStatement[Date] =
    effect(_.getDate(index))

  def getDate(index: Int, cal: Calendar): CallableStatement[Date] =
    effect(_.getDate(index, cal))

  def getDate(name: String): CallableStatement[Date] =
    effect(_.getDate(name))

  def getDate(name: String, cal: Calendar): CallableStatement[Date] =
    effect(_.getDate(name, cal))

  def getDouble(index: Int): CallableStatement[Double] =
    effect(_.getDouble(index))

  def getDouble(name: String): CallableStatement[Double] =
    effect(_.getDouble(name))

  def getFloat(index: Int): CallableStatement[Float] =
    effect(_.getFloat(index))

  def getFloat(name: String): CallableStatement[Float] =
    effect(_.getFloat(name))

  def getInt(index: Int): CallableStatement[Int] =
    effect(_.getInt(index))

  def getInt(name: String): CallableStatement[Int] =
    effect(_.getInt(name))

  def getLong(index: Int): CallableStatement[Long] =
    effect(_.getLong(index))

  def getLong(name: String): CallableStatement[Long] =
    effect(_.getLong(name))

  def getNCharacterStream(index: Int): CallableStatement[Reader] =
    effect(_.getNCharacterStream(index))

  def getNCharacterStream(name: String): CallableStatement[Reader] =
    effect(_.getNCharacterStream(name))

  def getNClob(index: Int): CallableStatement[NClob] =
    effect(_.getNClob(index))

  def getNClob(name: String): CallableStatement[NClob] =
    effect(_.getNClob(name))

  def getNString(index: Int): CallableStatement[String] =
    effect(_.getNString(index))

  def getNString(name: String): CallableStatement[String] =
    effect(_.getNString(name))

  def getObject(index: Int): CallableStatement[Object] =
    effect(_.getObject(index))

  def getObject(index: Int, map: Map[String,Class[_]]): CallableStatement[Object] =
    effect(_.getObject(index, map.asJava))

  def getObject(name: String): CallableStatement[Object] =
    effect(_.getObject(name))

  def getObject(name: String, map: Map[String,Class[_]]): CallableStatement[Object] =
    effect(_.getObject(name, map.asJava))

  def getRef(index: Int): CallableStatement[Ref] =
    effect(_.getRef(index))

  def getRef(name: String): CallableStatement[Ref] =
    effect(_.getRef(name))

  def getRowId(index: Int): CallableStatement[RowId] =
    effect(_.getRowId(index))

  def getRowId(name: String): CallableStatement[RowId] =
    effect(_.getRowId(name))

  def getShort(index: Int): CallableStatement[Short] =
    effect(_.getShort(index))

  def getShort(name: String): CallableStatement[Short] =
    effect(_.getShort(name))

  def getSQLXML(index: Int): CallableStatement[SQLXML] =
    effect(_.getSQLXML(index))

  def getSQLXML(name: String): CallableStatement[SQLXML] =
    effect(_.getSQLXML(name))

  def getString(index: Int): CallableStatement[String] =
    effect(_.getString(index))

  def getString(name: String): CallableStatement[String] =
    effect(_.getString(name))

  def getTime(index: Int): CallableStatement[Time] =
    effect(_.getTime(index))

  def getTime(index: Int, cal: Calendar): CallableStatement[Time] =
    effect(_.getTime(index, cal))

  def getTime(name: String): CallableStatement[Time] =
    effect(_.getTime(name))

  def getTime(name: String, cal: Calendar): CallableStatement[Time] =
    effect(_.getTime(name, cal))

  def getTimestamp(index: Int): CallableStatement[Timestamp] =
    effect(_.getTimestamp(index))

  def getTimestamp(index: Int, cal: Calendar): CallableStatement[Timestamp] =
    effect(_.getTimestamp(index, cal))

  def getTimestamp(name: String): CallableStatement[Timestamp] =
    effect(_.getTimestamp(name))

  def getTimestamp(name: String, cal: Calendar): CallableStatement[Timestamp] =
    effect(_.getTimestamp(name, cal))

  def getURL(index: Int): CallableStatement[URL] =
    effect(_.getURL(index))

  def getURL(name: String): CallableStatement[URL] =
    effect(_.getURL(name))

  def registerOutParameter(index: Int, sqlType: Int): CallableStatement[Unit] =
    effect(_.registerOutParameter(index, sqlType))

  def registerOutParameter(index: Int, sqlType: Int, scale: Int): CallableStatement[Unit] =
    effect(_.registerOutParameter(index, sqlType, scale))

  def registerOutParameter(index: Int, sqlType: Int, typeName: String): CallableStatement[Unit] =
    effect(_.registerOutParameter(index, sqlType, typeName))

  def registerOutParameter(name: String, sqlType: Int): CallableStatement[Unit] =
    effect(_.registerOutParameter(name, sqlType))

  def registerOutParameter(name: String, sqlType: Int, scale: Int): CallableStatement[Unit] =
    effect(_.registerOutParameter(name, sqlType, scale))

  def registerOutParameter(name: String, sqlType: Int, typeName: String): CallableStatement[Unit] =
    effect(_.registerOutParameter(name, sqlType, typeName))

  def setAsciiStream(name: String, x: InputStream): CallableStatement[Unit] =
    effect(_.setAsciiStream(name, x))

  def setAsciiStream(name: String, x: InputStream, length: Int): CallableStatement[Unit] =
    effect(_.setAsciiStream(name, x, length))

  def setAsciiStream(name: String, x: InputStream, length: Long): CallableStatement[Unit] =
    effect(_.setAsciiStream(name, x, length))

  def setBigDecimal(name: String, x: BigDecimal): CallableStatement[Unit] =
    effect(_.setBigDecimal(name, x.bigDecimal))

  def setBinaryStream(name: String, x: InputStream): CallableStatement[Unit] =
    effect(_.setBinaryStream(name, x))

  def setBinaryStream(name: String, x: InputStream, length: Int): CallableStatement[Unit] =
    effect(_.setBinaryStream(name, x, length))

  def setBinaryStream(name: String, x: InputStream, length: Long): CallableStatement[Unit] =
    effect(_.setBinaryStream(name, x, length))

  def setBlob(name: String, x: Blob): CallableStatement[Unit] =
    effect(_.setBlob(name, x))

  def setBlob(name: String, inputStream: InputStream): CallableStatement[Unit] =
    effect(_.setBlob(name, inputStream))

  def setBlob(name: String, inputStream: InputStream, length: Long): CallableStatement[Unit] =
    effect(_.setBlob(name, inputStream, length))

  def setBoolean(name: String, x: Boolean): CallableStatement[Unit] =
    effect(_.setBoolean(name, x))

  def setByte(name: String, x: Byte): CallableStatement[Unit] =
    effect(_.setByte(name, x))

  def setBytes(name: String, x: Array[Byte]): CallableStatement[Unit] =
    effect(_.setBytes(name, x))

  def setCharacterStream(name: String, reader: Reader): CallableStatement[Unit] =
    effect(_.setCharacterStream(name, reader))

  def setCharacterStream(name: String, reader: Reader, length: Int): CallableStatement[Unit] =
    effect(_.setCharacterStream(name, reader, length))

  def setCharacterStream(name: String, reader: Reader, length: Long): CallableStatement[Unit] =
    effect(_.setCharacterStream(name, reader, length))

  def setClob(name: String, x: Clob): CallableStatement[Unit] =
    effect(_.setClob(name, x))

  def setClob(name: String, reader: Reader): CallableStatement[Unit] =
    effect(_.setClob(name, reader))

  def setClob(name: String, reader: Reader, length: Long): CallableStatement[Unit] =
    effect(_.setClob(name, reader, length))

  def setDate(name: String, x: Date): CallableStatement[Unit] =
    effect(_.setDate(name, x))

  def setDate(name: String, x: Date, cal: Calendar): CallableStatement[Unit] =
    effect(_.setDate(name, x, cal))

  def setDouble(name: String, x: Double): CallableStatement[Unit] =
    effect(_.setDouble(name, x))

  def setFloat(name: String, x: Float): CallableStatement[Unit] =
    effect(_.setFloat(name, x))

  def setInt(name: String, x: Int): CallableStatement[Unit] =
    effect(_.setInt(name, x))

  def setLong(name: String, x: Long): CallableStatement[Unit] =
    effect(_.setLong(name, x))

  def setNCharacterStream(name: String, value: Reader): CallableStatement[Unit] =
    effect(_.setNCharacterStream(name, value))

  def setNCharacterStream(name: String, value: Reader, length: Long): CallableStatement[Unit] =
    effect(_.setNCharacterStream(name, value, length))

  def setNClob(name: String, value: NClob): CallableStatement[Unit] =
    effect(_.setNClob(name, value))

  def setNClob(name: String, reader: Reader): CallableStatement[Unit] =
    effect(_.setNClob(name, reader))

  def setNClob(name: String, reader: Reader, length: Long): CallableStatement[Unit] =
    effect(_.setNClob(name, reader, length))

  def setNString(name: String, value: String): CallableStatement[Unit] =
    effect(_.setNString(name, value))

  def setNull(name: String, sqlType: Int): CallableStatement[Unit] =
    effect(_.setNull(name, sqlType))

  def setNull(name: String, sqlType: Int, typeName: String): CallableStatement[Unit] =
    effect(_.setNull(name, sqlType, typeName))

  def setObject(name: String, x: Object): CallableStatement[Unit] =
    effect(_.setObject(name, x))

  def setObject(name: String, x: Object, targetSqlType: Int): CallableStatement[Unit] =
    effect(_.setObject(name, x, targetSqlType))

  def setObject(name: String, x: Object, targetSqlType: Int, scale: Int): CallableStatement[Unit] =
    effect(_.setObject(name, x, targetSqlType, scale))

  def setRowId(name: String, x: RowId): CallableStatement[Unit] =
    effect(_.setRowId(name, x))

  def setShort(name: String, x: Short): CallableStatement[Unit] =
    effect(_.setShort(name, x))

  def setSQLXML(name: String, xmlObject: SQLXML): CallableStatement[Unit] =
    effect(_.setSQLXML(name, xmlObject))

  def setString(name: String, x: String): CallableStatement[Unit] =
    effect(_.setString(name, x))

  def setTime(name: String, x: Time): CallableStatement[Unit] =
    effect(_.setTime(name, x))

  def setTime(name: String, x: Time, cal: Calendar): CallableStatement[Unit] =
    effect(_.setTime(name, x, cal))

  def setTimestamp(name: String, x: Timestamp): CallableStatement[Unit] =
    effect(_.setTimestamp(name, x))

  def setTimestamp(name: String, x: Timestamp, cal: Calendar): CallableStatement[Unit] =
    effect(_.setTimestamp(name, x, cal))

  def setURL(name: String, x: URL): CallableStatement[Unit] =
    effect(_.setURL(name, x))

  def wasNull: CallableStatement[Boolean] =
    effect(_.wasNull)

}
