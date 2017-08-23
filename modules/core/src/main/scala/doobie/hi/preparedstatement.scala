package doobie.hi

import doobie.enum.jdbctype.JdbcType
import doobie.util.meta.Meta
import doobie.enum.columnnullable.ColumnNullable
import doobie.enum.parameternullable.ParameterNullable
import doobie.enum.parametermode.ParameterMode
import doobie.enum.holdability.Holdability
import doobie.enum.nullability.NullabilityKnown
import doobie.enum.fetchdirection.FetchDirection
import doobie.enum.resultsetconcurrency.ResultSetConcurrency
import doobie.enum.resultsettype.ResultSetType

import doobie.free.{ preparedstatement => PS }
import doobie.free.{ resultset => RS }

import doobie.util.analysis._
import doobie.util.composite._
import doobie.util.stream.repeatEvalChunks

import doobie.syntax.monaderror._

import java.sql.{ ParameterMetaData, ResultSetMetaData, SQLWarning }

import scala.Predef.{ intArrayOps, intWrapper }

import cats.Foldable
import cats.implicits._
import cats.data.Ior
import fs2.Stream
import fs2.Stream.bracket

/**
 * Module of high-level constructors for `PreparedStatementIO` actions. Batching operations are not
 * provided; see the `statement` module for this functionality.
 * @group Modules
 */
object preparedstatement {

  import PS.AsyncPreparedStatementIO // we need this instance ... TODO: re-org
  import RS.AsyncResultSetIO

  // fs2 handler, not public
  private def unrolled[A: Composite](rs: java.sql.ResultSet, chunkSize: Int): Stream[PreparedStatementIO, A] =
    repeatEvalChunks(PS.embed(rs, resultset.getNextChunk[A](chunkSize)))

  /** @group Execution */
  def process[A: Composite](chunkSize: Int): Stream[PreparedStatementIO, A] =
    bracket(PS.executeQuery)(unrolled[A](_, chunkSize), PS.embed(_, RS.close))

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
    fa.toList.foldRight(executeBatch)((a, b) => set(a) *> addBatch *> b).map(_.sum)

  /**
   * Add many sets of parameters.
   * @group Batching
   */
  def addBatches[F[_]: Foldable, A: Composite](fa: F[A]): PreparedStatementIO[Unit] =
    fa.toList.foldRight(().pure[PreparedStatementIO])((a, b) => set(a) *> addBatch *> b)

  /** @group Execution */
  def executeQuery[A](k: ResultSetIO[A]): PreparedStatementIO[A] =
    PS.executeQuery.flatMap(s => PS.embed(s, k guarantee RS.close))

  /** @group Execution */
  val executeUpdate: PreparedStatementIO[Int] =
    PS.executeUpdate

  /** @group Execution */
  def executeUpdateWithUniqueGeneratedKeys[A: Composite]: PreparedStatementIO[A] =
    executeUpdate.flatMap(_ => getUniqueGeneratedKeys[A])

 /** @group Execution */
  def executeUpdateWithGeneratedKeys[A: Composite](chunkSize: Int): Stream[PreparedStatementIO, A] =
    bracket(PS.executeUpdate *> PS.getGeneratedKeys)(unrolled[A](_, chunkSize), PS.embed(_, RS.close))
  /**
   * Compute the column `JdbcMeta` list for this `PreparedStatement`.
   * @group Metadata
   */
  def getColumnJdbcMeta: PreparedStatementIO[List[ColumnMeta]] =
    PS.getMetaData.map {
      case null => Nil // https://github.com/tpolecat/doobie/issues/262
      case md   =>
        (1 to md.getColumnCount).toList.map { i =>
          val j = JdbcType.fromInt(md.getColumnType(i))
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
  def getColumnMappings[A](implicit A: Composite[A]): PreparedStatementIO[List[(Meta[_], NullabilityKnown) Ior ColumnMeta]] =
    getColumnJdbcMeta.map(m => A.meta align m)

  /** @group Properties */
  val getFetchDirection: PreparedStatementIO[FetchDirection] =
    PS.getFetchDirection.map(FetchDirection.unsafeFromInt)

  /** @group Properties */
  val getFetchSize: PreparedStatementIO[Int] =
    PS.getFetchSize

  /** @group Results */
  def getGeneratedKeys[A](k: ResultSetIO[A]): PreparedStatementIO[A] =
    PS.getGeneratedKeys.flatMap(s => PS.embed(s, k guarantee RS.close))

  /** @group Results */
  def getUniqueGeneratedKeys[A: Composite]: PreparedStatementIO[A] =
    getGeneratedKeys(resultset.getUnique[A])

  /**
   * Compute the parameter `JdbcMeta` list for this `PreparedStatement`.
   * @group Metadata
   */
  def getParameterJdbcMeta: PreparedStatementIO[List[ParameterMeta]] =
    PS.getParameterMetaData.map { md =>
      (1 to md.getParameterCount).toList.map { i =>
        val j = JdbcType.fromInt(md.getParameterType(i))
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
  def getParameterMappings[A](implicit A: Composite[A]): PreparedStatementIO[List[(Meta[_], NullabilityKnown) Ior ParameterMeta]] =
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

  /** @group Properties */
  def setQueryTimeout(a: Int): PreparedStatementIO[Unit] =
    PS.setQueryTimeout(a)

}
