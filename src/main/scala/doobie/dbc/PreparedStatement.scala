package doobie
package dbc

import scalaz.effect.IO
import java.sql

object preparedstatement extends util.TWorld[sql.PreparedStatement] with PreparedStatementOps[sql.PreparedStatement] {

  type PreparedStatement[+A] = Action[A]

  private[dbc] def run[A](a: PreparedStatement[A], s: sql.PreparedStatement): IO[A] = 
    eval(a, s).map(_._2)
  
}

trait PreparedStatementOps[A <: sql.PreparedStatement] extends StatementOps[A] { this: util.TWorld[A] =>

  import sql.Time
  import sql.Timestamp
  import sql.Ref
  import sql.Blob
  import sql.Date
  import sql.Clob
  import sql.ResultSetMetaData
  import sql.ParameterMetaData
  import java.net.URL
  import java.io.{ InputStream, Reader }
  import java.util.Calendar

  def addBatch: Action[Unit] =
    effect(_.addBatch)

  def clearParameters: Action[Unit] =
    effect(_.clearParameters)

  def execute: Action[Boolean] =
    effect(_.execute)

  def executeQuery[A](k: ResultSet[A]): Action[A] =
    for {
      r <- effect(_.executeQuery)
      a <- resultset.run(k, r).liftIO[Action]
    } yield a

  def executeUpdate: Action[Int] =
    effect(_.executeUpdate)

  def getMetaData: Action[ResultSetMetaData] =
    ???

  def getParameterMetaData: Action[ParameterMetaData] =
    ???

  // def setArray(index: Int, x: sql.Array): Action[Unit] =
  //   effect(_.setArray(index, x))

  // def setAsciiStream(index: Int, x: InputStream, length: Int): Action[Unit] =
  //   effect(_.setAsciiStream(index, x, length))

  def setBigDecimal(index: Int, x: BigDecimal): Action[Unit] =
    effect(_.setBigDecimal(index, x.bigDecimal))

  // def setBinaryStream(index: Int, x: InputStream, length: Int): Action[Unit] =
  //   effect(_.setBinaryStream(index, x, length))

  // def setBlob(index: Int, x: Blob): Action[Unit] =
  //   effect(_.setBlob(index, x))

  def setBoolean(index: Int, x: Boolean): Action[Unit] =
    effect(_.setBoolean(index, x))

  def setByte(index: Int, x: Byte): Action[Unit] =
    effect(_.setByte(index, x))

  // def setBytes(index: Int, x: Array[Byte]): Action[Unit] =
  //   effect(_.setBytes(index, x))

  // def setCharacterStream(index: Int, reader: Reader, length: Int): Action[Unit] =
  //   effect(_.setCharacterStream(index, reader, length))

  // def setClob(index: Int, x: Clob): Action[Unit] =
  //   effect(_.setClob(index, x))

  // def setDate(index: Int, x: Date): Action[Unit] =
  //   effect(_.setDate(index, x))

  // def setDate(index: Int, x: Date, cal: Calendar): Action[Unit] =
  //   effect(_.setDate(index, x, cal))

  def setDouble(index: Int, x: Double): Action[Unit] =
    effect(_.setDouble(index, x))

  def setFloat(index: Int, x: Float): Action[Unit] =
    effect(_.setFloat(index, x))

  def setInt(index: Int, x: Int): Action[Unit] =
    effect(_.setInt(index, x))

  def setLong(index: Int, x: Long): Action[Unit] =
    effect(_.setLong(index, x))

  def setNull(index: Int, sqlType: Int): Action[Unit] =
    effect(_.setNull(index, sqlType))

  def setNull(index: Int, sqlType: Int, typeName: String): Action[Unit] =
    effect(_.setNull(index, sqlType, typeName))

  // def setObject(index: Int, x: Object): Action[Unit] =
  //   effect(_.setObject(index, x))

  // def setObject(index: Int, x: Object, targetSqlType: Int): Action[Unit] =
  //   effect(_.setObject(index, x, targetSqlType))

  // def setObject(index: Int, x: Object, targetSqlType: Int, scale: Int): Action[Unit] =
  //   effect(_.setObject(index, x, targetSqlType, scale))

  // def setRef(index: Int, x: Ref): Action[Unit] =
  //   effect(_.setRef(index, x))

  def setShort(index: Int, x: Short): Action[Unit] =
    effect(_.setShort(index, x))

  def setString(index: Int, x: String): Action[Unit] =
    effect(_.setString(index, x))

  // def setTime(index: Int, x: Time): Action[Unit] =
  //   effect(_.setTime(index, x))

  // def setTime(index: Int, x: Time, cal: Calendar): Action[Unit] =
  //   effect(_.setTime(index, x, cal))

  // def setTimestamp(index: Int, x: Timestamp): Action[Unit] =
  //   effect(_.setTimestamp(index, x))

  // def setTimestamp(index: Int, x: Timestamp, cal: Calendar): Action[Unit] =
  //   effect(_.setTimestamp(index, x, cal))

  def setURL(index: Int, x: URL): Action[Unit] =
    effect(_.setURL(index, x))

}