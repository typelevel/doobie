package doobie.hi

import doobie.enum.holdability._
import doobie.enum.transactionisolation._

import doobie.syntax.catchable._

import doobie.free.{ connection => C }
import doobie.free.{ preparedstatement => PS }
import doobie.free.{ callablestatement => CS }
import doobie.free.{ resultset => RS }
import doobie.free.{ statement => S }
import doobie.free.{ databasemetadata => DMD }

import java.net.URL
import java.util.{ Date, Calendar }
import java.sql.{ ParameterMetaData, SQLWarning, Time, Timestamp, Ref, RowId }

import scala.collection.immutable.Map
import scala.collection.JavaConverters._
import scala.Predef.intArrayOps

import scalaz.syntax.id._

/**
 * Module of high-level constructors for `PreparedStatementIO` actions.
 * @group Modules
 */
object preparedstatement {

  /** @group Typeclass Instances */
  implicit val MonadPreparedStatementIO = PS.MonadPreparedStatementIO

  /** @group Typeclass Instances */
  implicit val CatchablePreparedStatementIO = PS.CatchablePreparedStatementIO

  /** @group Batching */
  val addBatch: PreparedStatementIO[Unit] =
    PS.addBatch

  /** @group Batching */
  def addBatch(sql: String): PreparedStatementIO[Unit] =
    PS.addBatch(sql)

  /** @group Batching */
  val clearBatch: PreparedStatementIO[Unit] =
    PS.clearBatch

  /** @group Query Parameters */
  val clearParameters: PreparedStatementIO[Unit] =
    PS.clearParameters

  /** @group Execution */
  val execute: PreparedStatementIO[Boolean] =
    PS.execute

  /** @group Execution */
  val executeBatch: PreparedStatementIO[List[Int]] =
    PS.executeBatch.map(_.toList)

  /** @group Execution */
  def executeQuery[A](k: ResultSetIO[A]): PreparedStatementIO[A] =
    PS.executeQuery.flatMap(s => PS.liftResultSet(s, k ensuring RS.close))

  /** @group Execution */
  val executeUpdate: PreparedStatementIO[Int] =
    PS.executeUpdate

  /** @group Properties */
  def getConnection[A](k: ConnectionIO[A]): PreparedStatementIO[A] =
    PS.getConnection.flatMap(s => PS.liftConnection(s, k))

  /** @group Properties */
  val getFetchDirection: PreparedStatementIO[Int] =
    Predef.???

  /** @group Properties */
  val getFetchSize: PreparedStatementIO[Int] =
    Predef.???

  /** @group Constructors (Primitives) */
  def getGeneratedKeys[A](k: ResultSetIO[A]): PreparedStatementIO[A] =
    Predef.???

  /** @group Properties */
  val getMaxFieldSize: PreparedStatementIO[Int] =
    Predef.???

  /** @group Properties */
  val getMaxRows: PreparedStatementIO[Int] =
    Predef.???

  /** @group Properties */
  def getMetaData[A](k: ResultSetMetaDataIO[A]): PreparedStatementIO[A] =
    Predef.???

  /** @group Batching */
  def getMoreResults(a: Int): PreparedStatementIO[Boolean] =
    Predef.???

  /** @group Batching */
  val getMoreResults: PreparedStatementIO[Boolean] =
    Predef.???

  /** @group Properties */
  val getParameterMetaData: PreparedStatementIO[ParameterMetaData] =
    Predef.???

  /** @group Properties */
  val getQueryTimeout: PreparedStatementIO[Int] =
    Predef.???

  /** @group Batching */
  def getResultSet[A](k: ResultSetIO[A]): PreparedStatementIO[A] =
    Predef.???

  /** @group Properties */
  val getResultSetConcurrency: PreparedStatementIO[Int] =
    Predef.???

  /** @group Properties */
  val getResultSetHoldability: PreparedStatementIO[Int] =
    Predef.???

  /** @group Properties */
  val getResultSetType: PreparedStatementIO[Int] =
    Predef.???

  /** @group Constructors (Primitives) */
  val getUpdateCount: PreparedStatementIO[Int] =
    Predef.???

  /** @group Constructors (Primitives) */
  val getWarnings: PreparedStatementIO[SQLWarning] =
    Predef.???

  /** @group Properties */
  val isPoolable: PreparedStatementIO[Boolean] =
    Predef.???

  /** @group Query Parameters */
  def setBigDecimal(a: Int, b: BigDecimal): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setBoolean(a: Int, b: Boolean): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setByte(a: Int, b: Byte): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setBytes(a: Int, b: Array[Byte]): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setCursorName(a: String): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setDate(a: Int, b: Date, c: Calendar): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setDate(a: Int, b: Date): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setDouble(a: Int, b: Double): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setEscapeProcessing(a: Boolean): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setFetchDirection(a: Int): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setFetchSize(a: Int): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setFloat(a: Int, b: Float): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setInt(a: Int, b: Int): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setLong(a: Int, b: Long): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setMaxFieldSize(a: Int): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setMaxRows(a: Int): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setNString(a: Int, b: String): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setNull(a: Int, b: Int, c: String): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setNull(a: Int, b: Int): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setPoolable(a: Boolean): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setQueryTimeout(a: Int): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setRef(a: Int, b: Ref): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setRowId(a: Int, b: RowId): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setShort(a: Int, b: Short): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setString(a: Int, b: String): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setTime(a: Int, b: Time): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setTime(a: Int, b: Time, c: Calendar): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setTimestamp(a: Int, b: Timestamp): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setTimestamp(a: Int, b: Timestamp, c: Calendar): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Query Parameters */
  def setURL(a: Int, b: URL): PreparedStatementIO[Unit] =
    Predef.???

}