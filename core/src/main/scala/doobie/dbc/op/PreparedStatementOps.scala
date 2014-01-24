package doobie.dbc
package op

import enum._
import scalaz._
import Scalaz._
import scalaz.effect.IO
import scalaz.effect.kleisliEffect._
import scalaz.syntax.effect.monadCatchIO._
import java.sql
import java.sql.{ Time, Timestamp, Ref, Blob, Date, Clob }
import java.net.URL
import java.io.{ InputStream, Reader }
import java.util.Calendar

trait PreparedStatementOps[A <: sql.PreparedStatement] extends StatementOps[A] { 

  def addBatch: Action[Unit] =
    primitive(s"addBatch", _.addBatch)

  def clearParameters: Action[Unit] =
    primitive(s"clearParameters", _.clearParameters)

  def execute: Action[Boolean] =
    primitive("execute", _.execute)

  def executeQuery[A](k: ResultSet[A]): Action[A] =
    gosub(primitive("executeQuery", _.executeQuery), k, resultset.close)

  def executeUpdate: Action[Int] =
    primitive(s"executeUpdate", _.executeUpdate)

  def getMetaData[A](k: ResultSetMetaData[A]): Action[A] =
    gosub0(primitive("getMetaData", _.getMetaData), k)

  def getParameterMetaData[A](k: ParameterMetaData[A]): Action[A] =
    gosub0(primitive("getParameterMetaData", _.getParameterMetaData), k)

  def setArray(index: Int, x: sql.Array): Action[Unit] =
    primitive(s"setArray($index, $x)", _.setArray(index, x))

  def setAsciiStream(index: Int, x: InputStream, length: Int): Action[Unit] =
    primitive(s"setAsciiStream($index, $x, $length)", _.setAsciiStream(index, x, length))

  def setBigDecimal(index: Int, x: BigDecimal): Action[Unit] =
    primitive(s"setBigDecimal($index, $x)", _.setBigDecimal(index, x.bigDecimal))

  def setBinaryStream(index: Int, x: InputStream, length: Int): Action[Unit] =
    primitive(s"setBinaryStream($index, $x, $length)", _.setBinaryStream(index, x, length))

  def setBlob(index: Int, x: Blob): Action[Unit] =
    primitive(s"setBlob($index, $x)", _.setBlob(index, x))

  def setBoolean(index: Int, x: Boolean): Action[Unit] =
    primitive(s"setBoolean($index, $x)", _.setBoolean(index, x))

  def setByte(index: Int, x: Byte): Action[Unit] =
    primitive(s"setByte($index, $x)", _.setByte(index, x))

  def setBytes(index: Int, x: Array[Byte]): Action[Unit] =
    primitive(s"setBytes($index, $x)", _.setBytes(index, x))

  def setCharacterStream(index: Int, reader: Reader, length: Int): Action[Unit] =
    primitive(s"setCharacterStream($index, $reader, $length)", _.setCharacterStream(index, reader, length))

  def setClob(index: Int, x: Clob): Action[Unit] =
    primitive(s"setClob($index, $x)", _.setClob(index, x))

  def setDate(index: Int, x: Date): Action[Unit] =
    primitive(s"setDate($index, $x)", _.setDate(index, x))

  def setDate(index: Int, x: Date, cal: Calendar): Action[Unit] =
    primitive(s"setDate($index, $x, $cal)", _.setDate(index, x, cal))

  def setDouble(index: Int, x: Double): Action[Unit] =
    primitive(s"setDouble($index, $x)", _.setDouble(index, x))

  def setFloat(index: Int, x: Float): Action[Unit] =
    primitive(s"setFloat($index, $x)", _.setFloat(index, x))

  def setInt(index: Int, x: Int): Action[Unit] =
    primitive(s"setInt($index, $x)", _.setInt(index, x))

  def setLong(index: Int, x: Long): Action[Unit] =
    primitive(s"setLong($index, $x)", _.setLong(index, x))

  def setNull(index: Int, sqlType: Int): Action[Unit] =
    primitive(s"setNull($index, $sqlType)", _.setNull(index, sqlType))

  def setNull(index: Int, sqlType: Int, typeName: String): Action[Unit] =
    primitive(s"setNull($index, $sqlType, $typeName)", _.setNull(index, sqlType, typeName))

  def setObject(index: Int, x: Object): Action[Unit] =
    primitive(s"setObject($index, $x)", _.setObject(index, x))

  def setObject(index: Int, x: Object, targetSqlType: Int): Action[Unit] =
    primitive(s"setObject($index, $x, $targetSqlType)", _.setObject(index, x, targetSqlType))

  def setObject(index: Int, x: Object, targetSqlType: Int, scale: Int): Action[Unit] =
    primitive(s"setObject($index, $x, $targetSqlType, $scale)", _.setObject(index, x, targetSqlType, scale))

  def setRef(index: Int, x: Ref): Action[Unit] =
    primitive(s"setRef($index, $x)", _.setRef(index, x))

  def setShort(index: Int, x: Short): Action[Unit] =
    primitive(s"setShort($index, $x)", _.setShort(index, x))

  def setString(index: Int, x: String): Action[Unit] =
    primitive(s"setString($index, $x)", _.setString(index, x))

  def setTime(index: Int, x: Time): Action[Unit] =
    primitive(s"setTime($index, $x)", _.setTime(index, x))

  def setTime(index: Int, x: Time, cal: Calendar): Action[Unit] =
    primitive(s"setTime($index, $x, $cal)", _.setTime(index, x, cal))

  def setTimestamp(index: Int, x: Timestamp): Action[Unit] =
    primitive(s"setTimestamp($index, $x)", _.setTimestamp(index, x))

  def setTimestamp(index: Int, x: Timestamp, cal: Calendar): Action[Unit] =
    primitive(s"setTimestamp($index, $x, $cal)", _.setTimestamp(index, x, cal))

  def setURL(index: Int, x: URL): Action[Unit] =
    primitive(s"setURL($index, $x)", _.setURL(index, x))

}