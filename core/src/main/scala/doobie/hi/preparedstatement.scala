package doobie.hi

import doobie.enum.jdbctype.JdbcType
import doobie.util.meta.Meta
import doobie.enum.columnnullable.ColumnNullable
import doobie.enum.parameternullable.ParameterNullable
import doobie.enum.parametermode.ParameterMode
import doobie.enum.holdability.Holdability
import doobie.enum.nullability.NullabilityKnown
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

import doobie.util.analysis._
import doobie.util.composite._
import doobie.util.process.resource

import java.net.URL
import java.util.{ Date, Calendar }
import java.sql.{ ParameterMetaData, ResultSetMetaData, SQLWarning, Time, Timestamp, Ref, RowId }

import scala.collection.immutable.Map
import scala.collection.JavaConverters._
import scala.Predef.intArrayOps

import scalaz.stream.Process
import scalaz.syntax.id._
import scalaz.syntax.enum._
import scalaz.syntax.foldable._
import scalaz.syntax.monad._
import scalaz.syntax.align._
import scalaz.std.anyVal._
import scalaz.std.list._
import scalaz.Foldable
import scalaz.\&/

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

  /** @group Execution */
  def process[A: Composite]: Process[PreparedStatementIO, A] =
    resource(PS.executeQuery)(rs =>
             PS.liftResultSet(rs, RS.close))(rs =>
             PS.liftResultSet(rs, resultset.getNext[A]))

  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): PreparedStatementIO[A] =
    PS.delay(a)

  /** @group Batching */
  val executeBatch: PreparedStatementIO[List[Int]] =
    PS.executeBatch.map(_.toList)

  /** @group Batching */
  val addBatch: PreparedStatementIO[Unit] =
    PS.addBatch

  /**
   * Add many sets of parameters and execute as a batch update, returning total rows updated.
   * @group Batching
   */
  def addBatchesAndExecute[F[_]: Foldable, A: Composite](fa: F[A]): PreparedStatementIO[Int] =
    fa.foldRight(executeBatch)((a, b) => set(a) *> addBatch *> b).map(_.sum)

  /**
   * Add many sets of parameters.
   * @group Batching
   */
  def addBatches[F[_]: Foldable, A: Composite](fa: F[A]): PreparedStatementIO[Unit] =
    fa.foldRight(().point[PreparedStatementIO])((a, b) => set(a) *> addBatch *> b)

  /** @group Execution */
  def executeQuery[A](k: ResultSetIO[A]): PreparedStatementIO[A] =
    PS.executeQuery.flatMap(s => PS.liftResultSet(s, k ensuring RS.close))

  /** @group Execution */
  val executeUpdate: PreparedStatementIO[Int] =
    PS.executeUpdate

  /** @group Execution */
  def executeUpdateWithUniqueGeneratedKeys[A: Composite] =
    executeUpdate.flatMap(_ => getUniqueGeneratedKeys[A])

 /** @group Execution */
  def executeUpdateWithGeneratedKeys[A: Composite]: Process[PreparedStatementIO, A] =
    resource(PS.executeUpdate.flatMap(_ => PS.getGeneratedKeys) : PreparedStatementIO[java.sql.ResultSet])(rs =>
             PS.liftResultSet(rs, RS.close))(rs =>
             PS.liftResultSet(rs, resultset.getNext[A]))

  /**
   * Compute the column `JdbcMeta` list for this `PreparedStatement`.
   * @group Metadata
   */
  def getColumnJdbcMeta: PreparedStatementIO[List[ColumnMeta]] =
    PS.getMetaData.map { md =>
      (1 |-> md.getColumnCount).map { i =>
        val j = JdbcType.unsafeFromInt(md.getColumnType(i))
        val s = md.getColumnTypeName(i)
        val n = ColumnNullable.unsafeFromInt(md.isNullable(i)).toNullability
        val c = md.getColumnName(i)
        ColumnMeta(j, s, n, c)
      }
    }

  /**
   * Compute the column mappings for this `PreparedStatement` by aligning its `JdbcMeta`
   * with the `JdbcMeta` provided by a `Composite` instance.
   * @group Metadata
   */
  def getColumnMappings[A](implicit A: Composite[A]): PreparedStatementIO[List[(Meta[_], NullabilityKnown) \&/ ColumnMeta]] =
    getColumnJdbcMeta.map(m => A.meta align m)

  /** @group Properties */
  val getFetchDirection: PreparedStatementIO[FetchDirection] =
    PS.getFetchDirection.map(FetchDirection.unsafeFromInt)

  /** @group Properties */
  val getFetchSize: PreparedStatementIO[Int] =
    PS.getFetchSize

  /** @group Results */
  def getGeneratedKeys[A](k: ResultSetIO[A]): PreparedStatementIO[A] =
    PS.getGeneratedKeys.flatMap(s => PS.liftResultSet(s, k ensuring RS.close))

  /** @group Results */
  def getUniqueGeneratedKeys[A: Composite]: PreparedStatementIO[A] =
    getGeneratedKeys(resultset.getUnique[A])

  /**
   * Compute the parameter `JdbcMeta` list for this `PreparedStatement`.
   * @group Metadata
   */
  def getParameterJdbcMeta: PreparedStatementIO[List[ParameterMeta]] =
    PS.getParameterMetaData.map { md =>
      (1 |-> md.getParameterCount).map { i =>
        val j = JdbcType.unsafeFromInt(md.getParameterType(i))
        val s = md.getParameterTypeName(i)
        val n = ParameterNullable.unsafeFromInt(md.isNullable(i)).toNullability
        val m = ParameterMode.unsafeFromInt(md.getParameterMode(i))
        ParameterMeta(j, s, n, m)
      }
    }

  /**
   * Compute the parameter mappings for this `PreparedStatement` by aligning its `JdbcMeta`
   * with the `JdbcMeta` provided by a `Composite` instance.
   * @group Metadata
   */
  def getParameterMappings[A](implicit A: Composite[A]): PreparedStatementIO[List[(Meta[_], NullabilityKnown) \&/ ParameterMeta]] =
    getParameterJdbcMeta.map(m => A.meta align m)

  /** @group Properties */
  val getMaxFieldSize: PreparedStatementIO[Int] =
    PS.getMaxFieldSize

  /** @group Properties */
  val getMaxRows: PreparedStatementIO[Int] =
    PS.getMaxRows

  /** @group MetaData */
  val getMetaData: PreparedStatementIO[ResultSetMetaData] =
    PS.getMetaData

  /** @group MetaData */
  val getParameterMetaData: PreparedStatementIO[ParameterMetaData] =
    PS.getParameterMetaData

  /** @group Properties */
  val getQueryTimeout: PreparedStatementIO[Int] =
    PS.getQueryTimeout

  /** @group Properties */
  val getResultSetConcurrency: PreparedStatementIO[ResultSetConcurrency] =
    PS.getResultSetConcurrency.map(ResultSetConcurrency.unsafeFromInt)

  /** @group Properties */
  val getResultSetHoldability: PreparedStatementIO[Holdability] =
    PS.getResultSetHoldability.map(Holdability.unsafeFromInt)

  /** @group Properties */
  val getResultSetType: PreparedStatementIO[ResultSetType] =
    PS.getResultSetType.map(ResultSetType.unsafeFromInt)

  /** @group Results */
  val getWarnings: PreparedStatementIO[SQLWarning] =
    PS.getWarnings

  /**
   * Set the given composite value, starting at column `n`.
   * @group Parameters
   */
  def set[A](n: Int, a: A)(implicit A: Composite[A]): PreparedStatementIO[Unit] =
    A.set(a).eval(n)

  /**
   * Set the given composite value, starting at column `1`.
   * @group Parameters
   */
  def set[A](a: A)(implicit A: Composite[A]): PreparedStatementIO[Unit] =
    A.set(a).eval(1)

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

  /** @group Properties */
  def setQueryTimeout(a: Int): PreparedStatementIO[Unit] =
    PS.setQueryTimeout(a)

}
