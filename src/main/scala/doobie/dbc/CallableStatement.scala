package doobie
package dbc

import scalaz.effect.IO
import java.sql
import java.sql.{ Blob, Clob, NClob, Date, Time, Timestamp, Ref, RowId, SQLXML }
import java.io.{ Reader, InputStream }
import java.util.Calendar
import java.net.URL
import scala.collection.JavaConverters._

// ok
trait CallableStatementFunctions extends PreparedStatementOps[sql.CallableStatement] {

  def getArray(index: Int): Action[sql.Array] =
    primitive(s"getArray($index)", _.getArray(index))

  def getArray(name: String): Action[sql.Array] =
    primitive(s"getArray($name)", _.getArray(name))

  def getBigDecimal(index: Int): Action[BigDecimal] =
    primitive(s"getBigDecimal($index)", _.getBigDecimal(index))

  @deprecated("Deprecated in JDBC", "0.1")
  def getBigDecimal(index: Int, scale: Int): Action[BigDecimal] =
    primitive(s"getBigDecimal($index, $scale)", _.getBigDecimal(index, scale))

  def getBigDecimal(name: String): Action[BigDecimal] =
    primitive(s"getBigDecimal($name)", _.getBigDecimal(name))

  def getBlob(index: Int): Action[Blob] =
    primitive(s"getBlob($index)", _.getBlob(index))

  def getBlob(name: String): Action[Blob] =
    primitive(s"getBlob($name)", _.getBlob(name))

  def getBoolean(index: Int): Action[Boolean] =
    primitive(s"getBoolean($index)", _.getBoolean(index))

  def getBoolean(name: String): Action[Boolean] =
    primitive(s"getBoolean($name)", _.getBoolean(name))

  def getByte(index: Int): Action[Byte] =
    primitive(s"getByte($index)", _.getByte(index))

  def getByte(name: String): Action[Byte] =
    primitive(s"getByte($name)", _.getByte(name))

  def getBytes(index: Int): Action[Array[Byte]] =
    primitive(s"getBytes($index)", _.getBytes(index))

  def getBytes(name: String): Action[Array[Byte]] =
    primitive(s"getBytes($name)", _.getBytes(name))

  def getCharacterStream(index: Int): Action[Reader] =
    primitive(s"getCharacterStream($index)", _.getCharacterStream(index))

  def getCharacterStream(name: String): Action[Reader] =
    primitive(s"getCharacterStream($name)", _.getCharacterStream(name))

  def getClob(index: Int): Action[Clob] =
    primitive(s"getClob($index)", _.getClob(index))

  def getClob(name: String): Action[Clob] =
    primitive(s"getClob($name)", _.getClob(name))

  def getDate(index: Int): Action[Date] =
    primitive(s"getDate($index)", _.getDate(index))

  def getDate(index: Int, cal: Calendar): Action[Date] =
    primitive(s"getDate($index, $cal)", _.getDate(index, cal))

  def getDate(name: String): Action[Date] =
    primitive(s"getDate($name)", _.getDate(name))

  def getDate(name: String, cal: Calendar): Action[Date] =
    primitive(s"getDate($name, $cal)", _.getDate(name, cal))

  def getDouble(index: Int): Action[Double] =
    primitive(s"getDouble($index)", _.getDouble(index))

  def getDouble(name: String): Action[Double] =
    primitive(s"getDouble($name)", _.getDouble(name))

  def getFloat(index: Int): Action[Float] =
    primitive(s"getFloat($index)", _.getFloat(index))

  def getFloat(name: String): Action[Float] =
    primitive(s"getFloat($name)", _.getFloat(name))

  def getInt(index: Int): Action[Int] =
    primitive(s"getInt($index)", _.getInt(index))

  def getInt(name: String): Action[Int] =
    primitive(s"getInt($name)", _.getInt(name))

  def getLong(index: Int): Action[Long] =
    primitive(s"getLong($index)", _.getLong(index))

  def getLong(name: String): Action[Long] =
    primitive(s"getLong($name)", _.getLong(name))

  def getNCharacterStream(index: Int): Action[Reader] =
    primitive(s"getNCharacterStream($index)", _.getNCharacterStream(index))

  def getNCharacterStream(name: String): Action[Reader] =
    primitive(s"getNCharacterStream($name)", _.getNCharacterStream(name))

  def getNClob(index: Int): Action[NClob] =
    primitive(s"getNClob($index)", _.getNClob(index))

  def getNClob(name: String): Action[NClob] =
    primitive(s"getNClob($name)", _.getNClob(name))

  def getNString(index: Int): Action[String] =
    primitive(s"getNString($index)", _.getNString(index))

  def getNString(name: String): Action[String] =
    primitive(s"getNString($name)", _.getNString(name))

  def getObject(index: Int): Action[Object] =
    primitive(s"getObject($index)", _.getObject(index))

  def getObject(index: Int, map: Map[String,Class[_]]): Action[Object] =
    primitive(s"getObject($index, $map)", _.getObject(index, map.asJava))

  def getObject(name: String): Action[Object] =
    primitive(s"getObject($name)", _.getObject(name))

  def getObject(name: String, map: Map[String,Class[_]]): Action[Object] =
    primitive(s"getObject($name, $map)", _.getObject(name, map.asJava))

  def getRef(index: Int): Action[Ref] =
    primitive(s"getRef($index)", _.getRef(index))

  def getRef(name: String): Action[Ref] =
    primitive(s"getRef($name)", _.getRef(name))

  def getRowId(index: Int): Action[RowId] =
    primitive(s"getRowId($index)", _.getRowId(index))

  def getRowId(name: String): Action[RowId] =
    primitive(s"getRowId($name)", _.getRowId(name))

  def getShort(index: Int): Action[Short] =
    primitive(s"getShort($index)", _.getShort(index))

  def getShort(name: String): Action[Short] =
    primitive(s"getShort($name)", _.getShort(name))

  def getSQLXML(index: Int): Action[SQLXML] =
    primitive(s"getSQLXML($index)", _.getSQLXML(index))

  def getSQLXML(name: String): Action[SQLXML] =
    primitive(s"getSQLXML($name)", _.getSQLXML(name))

  def getString(index: Int): Action[String] =
    primitive(s"getString($index)", _.getString(index))

  def getString(name: String): Action[String] =
    primitive(s"getString($name)", _.getString(name))

  def getTime(index: Int): Action[Time] =
    primitive(s"getTime($index)", _.getTime(index))

  def getTime(index: Int, cal: Calendar): Action[Time] =
    primitive(s"getTime($index, $cal)", _.getTime(index, cal))

  def getTime(name: String): Action[Time] =
    primitive(s"getTime($name)", _.getTime(name))

  def getTime(name: String, cal: Calendar): Action[Time] =
    primitive(s"getTime($name, $cal)", _.getTime(name, cal))

  def getTimestamp(index: Int): Action[Timestamp] =
    primitive(s"getTimestamp($index)", _.getTimestamp(index))

  def getTimestamp(index: Int, cal: Calendar): Action[Timestamp] =
    primitive(s"getTimestamp($index, $cal)", _.getTimestamp(index, cal))

  def getTimestamp(name: String): Action[Timestamp] =
    primitive(s"getTimestamp($name)", _.getTimestamp(name))

  def getTimestamp(name: String, cal: Calendar): Action[Timestamp] =
    primitive(s"getTimestamp($name, $cal)", _.getTimestamp(name, cal))

  def getURL(index: Int): Action[URL] =
    primitive(s"getURL($index)", _.getURL(index))

  def getURL(name: String): Action[URL] =
    primitive(s"getURL($name)", _.getURL(name))

  def registerOutParameter(index: Int, sqlType: JdbcType): Action[Unit] =
    primitive(s"registerOutParameter($index, $sqlType)", _.registerOutParameter(index, sqlType.toInt))

  def registerOutParameter(index: Int, sqlType: JdbcType, scale: Int): Action[Unit] =
    primitive(s"registerOutParameter($index, $sqlType, $scale)", _.registerOutParameter(index, sqlType.toInt, scale))

  def registerOutParameter(index: Int, sqlType: JdbcType, typeName: String): Action[Unit] =
    primitive(s"registerOutParameter($index, $sqlType, $typeName)", _.registerOutParameter(index, sqlType.toInt, typeName))

  def registerOutParameter(name: String, sqlType: JdbcType): Action[Unit] =
    primitive(s"registerOutParameter($name, $sqlType)", _.registerOutParameter(name, sqlType.toInt))

  def registerOutParameter(name: String, sqlType: JdbcType, scale: Int): Action[Unit] =
    primitive(s"registerOutParameter($name, $sqlType, $scale)", _.registerOutParameter(name, sqlType.toInt, scale))

  def registerOutParameter(name: String, sqlType: JdbcType, typeName: String): Action[Unit] =
    primitive(s"registerOutParameter($name, $sqlType, $typeName)", _.registerOutParameter(name, sqlType.toInt, typeName))

  def setAsciiStream(name: String, x: InputStream): Action[Unit] =
    primitive(s"setAsciiStream($name, $x)", _.setAsciiStream(name, x))

  def setAsciiStream(name: String, x: InputStream, length: Int): Action[Unit] =
    primitive(s"setAsciiStream($name, $x, $length)", _.setAsciiStream(name, x, length))

  def setAsciiStream(name: String, x: InputStream, length: Long): Action[Unit] =
    primitive(s"setAsciiStream($name, $x, $length)", _.setAsciiStream(name, x, length))

  def setBigDecimal(name: String, x: BigDecimal): Action[Unit] =
    primitive(s"setBigDecimal($name, $x)", _.setBigDecimal(name, x.bigDecimal))

  def setBinaryStream(name: String, x: InputStream): Action[Unit] =
    primitive(s"setBinaryStream($name, $x)", _.setBinaryStream(name, x))

  def setBinaryStream(name: String, x: InputStream, length: Int): Action[Unit] =
    primitive(s"setBinaryStream($name, $x, $length)", _.setBinaryStream(name, x, length))

  def setBinaryStream(name: String, x: InputStream, length: Long): Action[Unit] =
    primitive(s"setBinaryStream($name, $x, $length)", _.setBinaryStream(name, x, length))

  def setBlob(name: String, x: Blob): Action[Unit] =
    primitive(s"setBlob($name, $x)", _.setBlob(name, x))

  def setBlob(name: String, inputStream: InputStream): Action[Unit] =
    primitive(s"setBlob($name, $inputStream)", _.setBlob(name, inputStream))

  def setBlob(name: String, inputStream: InputStream, length: Long): Action[Unit] =
    primitive(s"setBlob($name, $inputStream, $length)", _.setBlob(name, inputStream, length))

  def setBoolean(name: String, x: Boolean): Action[Unit] =
    primitive(s"setBoolean($name, $x)", _.setBoolean(name, x))

  def setByte(name: String, x: Byte): Action[Unit] =
    primitive(s"setByte($name, $x)", _.setByte(name, x))

  def setBytes(name: String, x: Array[Byte]): Action[Unit] =
    primitive(s"setBytes($name, $x)", _.setBytes(name, x))

  def setCharacterStream(name: String, reader: Reader): Action[Unit] =
    primitive(s"setCharacterStream($name, $reader)", _.setCharacterStream(name, reader))

  def setCharacterStream(name: String, reader: Reader, length: Int): Action[Unit] =
    primitive(s"setCharacterStream($name, $reader, $length)", _.setCharacterStream(name, reader, length))

  def setCharacterStream(name: String, reader: Reader, length: Long): Action[Unit] =
    primitive(s"setCharacterStream($name, $reader, $length)", _.setCharacterStream(name, reader, length))

  def setClob(name: String, x: Clob): Action[Unit] =
    primitive(s"setClob($name, $x)", _.setClob(name, x))

  def setClob(name: String, reader: Reader): Action[Unit] =
    primitive(s"setClob($name, $reader)", _.setClob(name, reader))

  def setClob(name: String, reader: Reader, length: Long): Action[Unit] =
    primitive(s"setClob($name, $reader, $length)", _.setClob(name, reader, length))

  def setDate(name: String, x: Date): Action[Unit] =
    primitive(s"setDate($name, $x)", _.setDate(name, x))

  def setDate(name: String, x: Date, cal: Calendar): Action[Unit] =
    primitive(s"setDate($name, $x, $cal)", _.setDate(name, x, cal))

  def setDouble(name: String, x: Double): Action[Unit] =
    primitive(s"setDouble($name, $x)", _.setDouble(name, x))

  def setFloat(name: String, x: Float): Action[Unit] =
    primitive(s"setFloat($name, $x)", _.setFloat(name, x))

  def setInt(name: String, x: Int): Action[Unit] =
    primitive(s"setInt($name, $x)", _.setInt(name, x))

  def setLong(name: String, x: Long): Action[Unit] =
    primitive(s"setLong($name, $x)", _.setLong(name, x))

  def setNCharacterStream(name: String, value: Reader): Action[Unit] =
    primitive(s"setNCharacterStream($name, $value)", _.setNCharacterStream(name, value))

  def setNCharacterStream(name: String, value: Reader, length: Long): Action[Unit] =
    primitive(s"setNCharacterStream($name, $value, $length)", _.setNCharacterStream(name, value, length))

  def setNClob(name: String, value: NClob): Action[Unit] =
    primitive(s"setNClob($name, $value)", _.setNClob(name, value))

  def setNClob(name: String, reader: Reader): Action[Unit] =
    primitive(s"setNClob($name, $reader)", _.setNClob(name, reader))

  def setNClob(name: String, reader: Reader, length: Long): Action[Unit] =
    primitive(s"setNClob($name, $reader, $length)", _.setNClob(name, reader, length))

  def setNString(name: String, value: String): Action[Unit] =
    primitive(s"setNString($name, $value)", _.setNString(name, value))

  def setNull(name: String, sqlType: JdbcType): Action[Unit] =
    primitive(s"setNull($name, $sqlType)", _.setNull(name, sqlType.toInt))

  def setNull(name: String, sqlType: JdbcType, typeName: String): Action[Unit] =
    primitive(s"setNull($name, $sqlType, $typeName)", _.setNull(name, sqlType.toInt, typeName))

  def setObject(name: String, x: Object): Action[Unit] =
    primitive(s"setObject($name, $x)", _.setObject(name, x))

  def setObject(name: String, x: Object, targetSqlType: JdbcType): Action[Unit] =
    primitive(s"setObject($name, $x, $targetSqlType)", _.setObject(name, x, targetSqlType.toInt))

  def setObject(name: String, x: Object, targetSqlType: JdbcType, scale: Int): Action[Unit] =
    primitive(s"setObject($name, $x, $targetSqlType, $scale)", _.setObject(name, x, targetSqlType.toInt, scale))

  def setRowId(name: String, x: RowId): Action[Unit] =
    primitive(s"setRowId($name, $x)", _.setRowId(name, x))

  def setShort(name: String, x: Short): Action[Unit] =
    primitive(s"setShort($name, $x)", _.setShort(name, x))

  def setSQLXML(name: String, xmlObject: SQLXML): Action[Unit] =
    primitive(s"setSQLXML($name, $xmlObject)", _.setSQLXML(name, xmlObject))

  def setString(name: String, x: String): Action[Unit] =
    primitive(s"setString($name, $x)", _.setString(name, x))

  def setTime(name: String, x: Time): Action[Unit] =
    primitive(s"setTime($name, $x)", _.setTime(name, x))

  def setTime(name: String, x: Time, cal: Calendar): Action[Unit] =
    primitive(s"setTime($name, $x, $cal)", _.setTime(name, x, cal))

  def setTimestamp(name: String, x: Timestamp): Action[Unit] =
    primitive(s"setTimestamp($name, $x)", _.setTimestamp(name, x))

  def setTimestamp(name: String, x: Timestamp, cal: Calendar): Action[Unit] =
    primitive(s"setTimestamp($name, $x, $cal)", _.setTimestamp(name, x, cal))

  def setURL(name: String, x: URL): Action[Unit] =
    primitive(s"setURL($name, $x)", _.setURL(name, x))

  def wasNull: Action[Boolean] =
    primitive(s"wasNull", _.wasNull)

}
