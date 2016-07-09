package doobie.hi

import doobie.enum.holdability.Holdability
import doobie.enum.transactionisolation.TransactionIsolation
import doobie.enum.fetchdirection.FetchDirection
import doobie.enum.resultsetconcurrency.ResultSetConcurrency
import doobie.enum.resultsettype.ResultSetType

import doobie.syntax.catchable.ToDoobieCatchableOps._

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
 * Module of high-level constructors for `StatementIO` actions.
 * @group Modules
 */
object statement {

  /** @group Typeclass Instances */
  implicit val CatchableStatementIO = S.CatchableStatementIO

  /** @group Batching */
  def addBatch(sql: String): StatementIO[Unit] =
    S.addBatch(sql)

  /** @group Batching */
  val clearBatch: StatementIO[Unit] =
    S.clearBatch

  /** @group Execution */
  val executeBatch: StatementIO[List[Int]] =
    S.executeBatch.map(arr =>
      // `Predef.intArrayOps` is not implicit as of Scala 2.12.0-M3
      // https://github.com/scala/scala/blob/v2.12.0-M3/src/library/scala/Predef.scala#L336
      // TODO replace with `_.toList` after it gets back
      intArrayOps(arr).toList
    )

  /** @group Execution */
  def executeQuery[A](sql: String)(k: ResultSetIO[A]): StatementIO[A] =
    S.executeQuery(sql).flatMap(s => S.lift(s, k ensuring RS.close))

  /** @group Execution */
  def executeUpdate(sql: String): StatementIO[Int] =
    S.executeUpdate(sql)

  /** @group Properties */
  val getFetchDirection: StatementIO[FetchDirection] =
    S.getFetchDirection.map(FetchDirection.unsafeFromInt)

  /** @group Properties */
  val getFetchSize: StatementIO[Int] =
    S.getFetchSize

  /** @group Results */
  def getGeneratedKeys[A](k: ResultSetIO[A]): StatementIO[A] =
    S.getGeneratedKeys.flatMap(s => S.lift(s, k ensuring RS.close))

  /** @group Properties */
  val getMaxFieldSize: StatementIO[Int] =
    S.getMaxFieldSize

  /** @group Properties */
  val getMaxRows: StatementIO[Int] =
    S.getMaxRows
     
  // /** @group Batching */
  // def getMoreResults(a: Int): StatementIO[Boolean] =
  //   Predef.???

  /** @group Batching */
  val getMoreResults: StatementIO[Boolean] =
    S.getMoreResults

  /** @group Properties */
  val getQueryTimeout: StatementIO[Int] =
    S.getQueryTimeout

  /** @group Batching */
  def getResultSet[A](k: ResultSetIO[A]): StatementIO[A] =
    S.getResultSet.flatMap(s => S.lift(s, k))

  /** @group Properties */
  val getResultSetConcurrency: StatementIO[ResultSetConcurrency] =
    S.getResultSetConcurrency.map(ResultSetConcurrency.unsafeFromInt)

  /** @group Properties */
  val getResultSetHoldability: StatementIO[Holdability] =
    S.getResultSetHoldability.map(Holdability.unsafeFromInt)

  /** @group Properties */
  val getResultSetType: StatementIO[ResultSetType] =
    S.getResultSetType.map(ResultSetType.unsafeFromInt)

  /** @group Results */
  val getUpdateCount: StatementIO[Int] =
    S.getUpdateCount

  /** @group Results */
  val getWarnings: StatementIO[SQLWarning] =
    S.getWarnings

  /** @group Properties */
  def setCursorName(name: String): StatementIO[Unit] =
    S.setCursorName(name)

  /** @group Properties */
  def setEscapeProcessing(a: Boolean): StatementIO[Unit] =
    S.setEscapeProcessing(a)

  /** @group Properties */
  def setFetchDirection(fd: FetchDirection): StatementIO[Unit] =
    S.setFetchDirection(fd.toInt)

  /** @group Properties */
  def setFetchSize(n: Int): StatementIO[Unit] =
    S.setFetchSize(n)

  /** @group Properties */
  def setMaxFieldSize(n: Int): StatementIO[Unit] =
    S.setMaxFieldSize(n)

  /** @group Properties */
  def setMaxRows(n: Int): StatementIO[Unit] =
    S.setMaxRows(n)

  /** @group Properties */
  def setQueryTimeout(a: Int): StatementIO[Unit] =
    S.setQueryTimeout(a)

}
