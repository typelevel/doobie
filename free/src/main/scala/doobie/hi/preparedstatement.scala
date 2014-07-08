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

import doobie.util.composite._

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

  /** @group Results */
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

  /** @group Results */
  val getUpdateCount: PreparedStatementIO[Int] =
    Predef.???

  /** @group Results */
  val getWarnings: PreparedStatementIO[SQLWarning] =
    Predef.???

  /** @group Properties */
  val isPoolable: PreparedStatementIO[Boolean] =
    Predef.???

  /** 
   * Set the given composite value, starting at column `n`.
   * @group Parameters 
   */
  def set[A](n: Int, a: A)(implicit A: Composite[A]): PreparedStatementIO[Unit] =
    A.set(n, a)

  /** 
   * Set the given composite value, starting at column `1`.
   * @group Parameters 
   */
  def set[A](a: A)(implicit A: Composite[A]): PreparedStatementIO[Unit] =
    A.set(1, a)

  /** @group Properties */
  def setCursorName(a: String): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Properties */
  def setEscapeProcessing(a: Boolean): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Properties */
  def setFetchDirection(a: Int): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Properties */
  def setFetchSize(a: Int): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Properties */
  def setMaxFieldSize(a: Int): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Properties */
  def setMaxRows(a: Int): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Properties */
  def setPoolable(a: Boolean): PreparedStatementIO[Unit] =
    Predef.???

  /** @group Properties */
  def setQueryTimeout(a: Int): PreparedStatementIO[Unit] =
    Predef.???

}