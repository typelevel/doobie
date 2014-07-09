package doobie.hi

import doobie.enum.holdability.Holdability
import doobie.enum.transactionisolation.TransactionIsolation
import doobie.enum.fetchdirection.FetchDirection
import doobie.enum.resultsetconcurrency.ResultSetConcurrency
import doobie.enum.resultsettype.ResultSetType

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
import java.sql.{ ParameterMetaData, ResultSetMetaData, SQLWarning, Time, Timestamp, Ref, RowId }

import scala.collection.immutable.Map
import scala.collection.JavaConverters._
import scala.Predef.intArrayOps

import scalaz.syntax.id._

/**
 * Module of high-level constructors for `PreparedStatementIO` actions. Batching operations are not
 * provided; see the `statement` module for this functionality.
 * @group Modules
 */
object preparedstatement {

  /** @group Typeclass Instances */
  implicit val MonadPreparedStatementIO = PS.MonadPreparedStatementIO

  /** @group Typeclass Instances */
  implicit val CatchablePreparedStatementIO = PS.CatchablePreparedStatementIO

  // /** @group Batching */
  // val addBatch: PreparedStatementIO[Unit] =
  //   PS.addBatch

  // /** @group Batching */
  // def addBatch(sql: String): PreparedStatementIO[Unit] =
  //   PS.addBatch(sql)

  // /** @group Batching */
  // val clearBatch: PreparedStatementIO[Unit] =
  //   PS.clearBatch

  // /** @group Parameters */
  // val clearParameters: PreparedStatementIO[Unit] =
  //   PS.clearParameters

  // /** @group Execution */
  // val execute: PreparedStatementIO[Boolean] =
  //   PS.execute

  // /** @group Execution */
  // val executeBatch: PreparedStatementIO[List[Int]] =
  //   PS.executeBatch.map(_.toList)

  /** @group Execution */
  def executeQuery[A](k: ResultSetIO[A]): PreparedStatementIO[A] =
    PS.executeQuery.flatMap(s => PS.liftResultSet(s, k ensuring RS.close))

  /** @group Execution */
  val executeUpdate: PreparedStatementIO[Int] =
    PS.executeUpdate

  // /** @group Properties */
  // def getConnection[A](k: ConnectionIO[A]): PreparedStatementIO[A] =
  //   PS.getConnection.flatMap(s => PS.liftConnection(s, k))

  /** @group Properties */
  val getFetchDirection: PreparedStatementIO[FetchDirection] =
    PS.getFetchDirection.map(FetchDirection.unsafeFromInt)

  /** @group Properties */
  val getFetchSize: PreparedStatementIO[Int] =
    PS.getFetchSize

  /** @group Results */
  def getGeneratedKeys[A](k: ResultSetIO[A]): PreparedStatementIO[A] =
    PS.getGeneratedKeys.flatMap(s => PS.liftResultSet(s, k ensuring RS.close))

  /** @group Properties */
  val getMaxFieldSize: PreparedStatementIO[Int] =
    PS.getMaxFieldSize

  /** @group Properties */
  val getMaxRows: PreparedStatementIO[Int] =
    PS.getMaxRows
     
  /** @group MetaData */
  val getMetaData: PreparedStatementIO[ResultSetMetaData] =
    PS.getMetaData

  // /** @group Batching */
  // def getMoreResults(a: Int): PreparedStatementIO[Boolean] =
  //   Predef.???

  // /** @group Batching */
  // val getMoreResults: PreparedStatementIO[Boolean] =
  //   PS.getMoreResults

  /** @group MetaData */
  val getParameterMetaData: PreparedStatementIO[ParameterMetaData] =
    PS.getParameterMetaData

  /** @group Properties */
  val getQueryTimeout: PreparedStatementIO[Int] =
    PS.getQueryTimeout

  // /** @group Batching */
  // def getResultSet[A](k: ResultSetIO[A]): PreparedStatementIO[A] =
  //   PS.getResultSet.flatMap(s => PS.liftResultSet(s, k))

  /** @group Properties */
  val getResultSetConcurrency: PreparedStatementIO[ResultSetConcurrency] =
    PS.getResultSetConcurrency.map(ResultSetConcurrency.unsafeFromInt)

  /** @group Properties */
  val getResultSetHoldability: PreparedStatementIO[Holdability] =
    PS.getResultSetHoldability.map(Holdability.unsafeFromInt)

  /** @group Properties */
  val getResultSetType: PreparedStatementIO[ResultSetType] =
    PS.getResultSetType.map(ResultSetType.unsafeFromInt)

  // /** @group Results */
  // val getUpdateCount: PreparedStatementIO[Int] =
  //   PS.getUpdateCount

  /** @group Results */
  val getWarnings: PreparedStatementIO[SQLWarning] =
    PS.getWarnings

  // /** @group Properties */
  // val isPoolable: PreparedStatementIO[Boolean] =
  //   PS.isPoolable

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
  def setCursorName(name: String): PreparedStatementIO[Unit] =
    PS.setCursorName(name)

  /** @group Properties */
  def setEscapeProcessing(a: Boolean): PreparedStatementIO[Unit] =
    PS.setEscapeProcessing(a)

  /** @group Properties */
  def setFetchDirection(fd: FetchDirection): PreparedStatementIO[Unit] =
    PS.setFetchDirection(fd.toInt)

  /** @group Properties */
  def setFetchSize(n: Int): PreparedStatementIO[Unit] =
    PS.setFetchSize(n)

  /** @group Properties */
  def setMaxFieldSize(n: Int): PreparedStatementIO[Unit] =
    PS.setMaxFieldSize(n)

  /** @group Properties */
  def setMaxRows(n: Int): PreparedStatementIO[Unit] =
    PS.setMaxRows(n)

  // /** @group Properties */
  // def setPoolable(a: Boolean): PreparedStatementIO[Unit] =
  //   PS.setPoolable(a)

  /** @group Properties */
  def setQueryTimeout(a: Int): PreparedStatementIO[Unit] =
    PS.setQueryTimeout(a)

}