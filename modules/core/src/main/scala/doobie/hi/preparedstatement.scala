// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.hi

import doobie.enumerated.JdbcType
import doobie.util.{Get, Put}
import doobie.enumerated.ColumnNullable
import doobie.enumerated.ParameterNullable
import doobie.enumerated.ParameterMode
import doobie.enumerated.Holdability
import doobie.enumerated.Nullability.NullabilityKnown
import doobie.enumerated.FetchDirection
import doobie.enumerated.ResultSetConcurrency
import doobie.enumerated.ResultSetType
import doobie.util.{Read, Write}
import doobie.util.analysis._
import doobie.util.stream.repeatEvalChunks
import doobie.free.{preparedstatement => IFPS, resultset => IFRS}
import doobie.syntax.align._

import java.sql.{ParameterMetaData, ResultSetMetaData, SQLWarning}
import scala.Predef.{intArrayOps, intWrapper}
import cats.Foldable
import cats.implicits._
import cats.effect.syntax.monadCancel._
import cats.data.Ior
import fs2.Stream

/** Module of high-level constructors for `PreparedStatementIO` actions. Batching operations are not provided; see the
  * `statement` module for this functionality.
  * @group Modules
  */

object preparedstatement {
  import implicits._

  // fs2 handler, not public
  private def unrolled[A: Read](rs: java.sql.ResultSet, chunkSize: Int): Stream[PreparedStatementIO, A] =
    repeatEvalChunks(IFPS.embed(rs, resultset.getNextChunk[A](chunkSize)))

  /** @group Execution */
  def stream[A: Read](chunkSize: Int): Stream[PreparedStatementIO, A] =
    Stream.bracket(IFPS.executeQuery)(IFPS.embed(_, IFRS.close)).flatMap(unrolled[A](_, chunkSize))

  /** Non-strict unit for capturing effects.
    * @group Constructors (Lifting)
    */
  def delay[A](a: => A): PreparedStatementIO[A] =
    IFPS.delay(a)

  /** @group Batching */
  val executeBatch: PreparedStatementIO[List[Int]] =
    IFPS.executeBatch.map(_.toIndexedSeq.toList) // intArrayOps does not have `toList` in 2.13

  /** @group Batching */
  @deprecated("Use doobie.FPS.addBatch instead", "1.0.0-RC6")
  val addBatch: PreparedStatementIO[Unit] =
    IFPS.addBatch

  /** Add many sets of parameters and execute as a batch update, returning total rows updated. Note that when an error
    * occurred while executing the batch, your JDBC driver may decide to continue executing the rest of the batch
    * instead of raising a `BatchUpdateException`. Please refer to your JDBC driver's documentation for its exact
    * behaviour. See
    * [[https://docs.oracle.com/en/java/javase/11/docs/api/java.sql/java/sql/Statement.html#executeBatch()]] for more
    * information
    * @group Batching
    */
  @deprecated(
    "Consider using doobie.HC.execute{With/Without}ResultSet" +
      "for logging support, or switch to addBatchesAndExecuteUnlogged instead",
    "1.0.0-RC6")
  def addBatchesAndExecute[F[_]: Foldable, A: Write](fa: F[A]): PreparedStatementIO[Int] =
    addBatchesAndExecuteUnlogged(fa)

  /** Add many sets of parameters and execute as a batch update, returning total rows updated. Note that when an error
    * occurred while executing the batch, your JDBC driver may decide to continue executing the rest of the batch
    * instead of raising a `BatchUpdateException`. Please refer to your JDBC driver's documentation for its exact
    * behaviour. See
    * [[https://docs.oracle.com/en/java/javase/11/docs/api/java.sql/java/sql/Statement.html#executeBatch()]] for more
    * information
    * @group Batching
    */
  def addBatchesAndExecuteUnlogged[F[_]: Foldable, A: Write](fa: F[A]): PreparedStatementIO[Int] =
    fa.toList
      .foldRight(IFPS.executeBatch)((a, b) => set(a) *> IFPS.addBatch *> b)
      .map(_.foldLeft(0)((acc, n) => acc + (n max 0))) // treat negatives (failures) as no rows updated

  /** Add many sets of parameters.
    * @group Batching
    */
  def addBatches[F[_]: Foldable, A: Write](fa: F[A]): PreparedStatementIO[Unit] =
    fa.toList.foldRight(().pure[PreparedStatementIO])((a, b) => set(a) *> IFPS.addBatch *> b)

  /** @group Execution */
  @deprecated(
    "Consider using doobie.HC.execute{With/Without}ResultSet" +
      "for logging support, or switch to addBatchesAndExecuteUnlogged instead",
    "1.0.0-RC6")
  def executeQuery[A](k: ResultSetIO[A]): PreparedStatementIO[A] =
    executeQueryUnlogged(k)

  def executeQueryUnlogged[A](k: ResultSetIO[A]): PreparedStatementIO[A] =
    IFPS.executeQuery.bracket(s => IFPS.embed(s, k))(s => IFPS.embed(s, IFRS.close))

  /** @group Execution */
  @deprecated("Use doobie.FPS.executeUpdate instead", "1.0.0-RC6")
  val executeUpdate: PreparedStatementIO[Int] =
    IFPS.executeUpdate

  /** @group Execution */
  @deprecated(
    "Consider using doobie.HC.executeWithoutResultSet" +
      "for logging support, or switch to executeUpdateWithUniqueGeneratedKeysUnlogged instead",
    "1.0.0-RC6"
  )
  def executeUpdateWithUniqueGeneratedKeys[A: Read]: PreparedStatementIO[A] =
    executeUpdateWithUniqueGeneratedKeysUnlogged

  def executeUpdateWithUniqueGeneratedKeysUnlogged[A: Read]: PreparedStatementIO[A] =
    IFPS.executeUpdate.flatMap(_ => getUniqueGeneratedKeys[A])

  /** @group Execution */
  @deprecated(
    "Consider using doobie.HC.stream" +
      "for logging support, or switch to executeUpdateWithUniqueGeneratedKeysUnlogged instead",
    "1.0.0-RC6"
  )
  def executeUpdateWithGeneratedKeys[A: Read](chunkSize: Int): Stream[PreparedStatementIO, A] =
    executeUpdateWithGeneratedKeysUnlogged(chunkSize)

  def executeUpdateWithGeneratedKeysUnlogged[A: Read](chunkSize: Int): Stream[PreparedStatementIO, A] =
    Stream.bracket(IFPS.executeUpdate *> IFPS.getGeneratedKeys)(IFPS.embed(_, IFRS.close)).flatMap(unrolled[A](
      _,
      chunkSize))

  /** Compute the column `JdbcMeta` list for this `PreparedStatement`.
    * @group Metadata
    */
  def getColumnJdbcMeta: PreparedStatementIO[List[ColumnMeta]] =
    IFPS.getMetaData.flatMap {
      case null => IFPS.pure(Nil) // https://github.com/tpolecat/doobie/issues/262
      case md =>
        (1 to md.getColumnCount).toList.traverse { i =>
          for {
            n <- ColumnNullable.fromIntF[PreparedStatementIO](md.isNullable(i))
          } yield {
            val j = JdbcType.fromInt(md.getColumnType(i))
            val s = md.getColumnTypeName(i)
            val c = md.getColumnName(i)
            ColumnMeta(j, s, n.toNullability, c)
          }
        }
    }

  /** Compute the column mappings for this `PreparedStatement` by aligning its `JdbcMeta` with the `JdbcMeta` provided
    * by a `Write` instance.
    * @group Metadata
    */
  def getColumnMappings[A](implicit A: Read[A]): PreparedStatementIO[List[(Get[_], NullabilityKnown) Ior ColumnMeta]] =
    getColumnJdbcMeta.map(m => A.gets align m)

  /** @group Properties */
  val getFetchDirection: PreparedStatementIO[FetchDirection] =
    IFPS.getFetchDirection.flatMap(FetchDirection.fromIntF[PreparedStatementIO])

  /** @group Properties */
  val getFetchSize: PreparedStatementIO[Int] =
    IFPS.getFetchSize

  /** @group Results */
  def getGeneratedKeys[A](k: ResultSetIO[A]): PreparedStatementIO[A] =
    IFPS.getGeneratedKeys.bracket(s => IFPS.embed(s, k))(s => IFPS.embed(s, IFRS.close))

  /** @group Results */
  def getUniqueGeneratedKeys[A: Read]: PreparedStatementIO[A] =
    getGeneratedKeys(resultset.getUnique[A])

  /** Compute the parameter `JdbcMeta` list for this `PreparedStatement`.
    * @group Metadata
    */
  def getParameterJdbcMeta: PreparedStatementIO[List[ParameterMeta]] =
    IFPS.getParameterMetaData.flatMap { md =>
      (1 to md.getParameterCount).toList.traverse { i =>
        for {
          n <- ParameterNullable.fromIntF[PreparedStatementIO](md.isNullable(i))
          m <- ParameterMode.fromIntF[PreparedStatementIO](md.getParameterMode(i))
        } yield {
          val j = JdbcType.fromInt(md.getParameterType(i))
          val s = md.getParameterTypeName(i)
          ParameterMeta(j, s, n.toNullability, m)
        }
      }
    }

  /** Compute the parameter mappings for this `PreparedStatement` by aligning its `JdbcMeta` with the `JdbcMeta`
    * provided by a `Write` instance.
    * @group Metadata
    */
  def getParameterMappings[A](implicit
      A: Write[A]
  ): PreparedStatementIO[List[(Put[_], NullabilityKnown) Ior ParameterMeta]] =
    getParameterJdbcMeta.map(m => A.puts align m)

  /** @group Properties */
  val getMaxFieldSize: PreparedStatementIO[Int] =
    IFPS.getMaxFieldSize

  /** @group Properties */
  val getMaxRows: PreparedStatementIO[Int] =
    IFPS.getMaxRows

  /** @group MetaData */
  val getMetaData: PreparedStatementIO[ResultSetMetaData] =
    IFPS.getMetaData

  /** @group MetaData */
  val getParameterMetaData: PreparedStatementIO[ParameterMetaData] =
    IFPS.getParameterMetaData

  /** @group Properties */
  val getQueryTimeout: PreparedStatementIO[Int] =
    IFPS.getQueryTimeout

  /** @group Properties */
  val getResultSetConcurrency: PreparedStatementIO[ResultSetConcurrency] =
    IFPS.getResultSetConcurrency.flatMap(ResultSetConcurrency.fromIntF[PreparedStatementIO])

  /** @group Properties */
  val getResultSetHoldability: PreparedStatementIO[Holdability] =
    IFPS.getResultSetHoldability.flatMap(Holdability.fromIntF[PreparedStatementIO])

  /** @group Properties */
  val getResultSetType: PreparedStatementIO[ResultSetType] =
    IFPS.getResultSetType.flatMap(ResultSetType.fromIntF[PreparedStatementIO])

  /** @group Results */
  val getWarnings: PreparedStatementIO[SQLWarning] =
    IFPS.getWarnings

  /** Set the given writable value, starting at column `n`.
    * @group Parameters
    */
  def set[A](n: Int, a: A)(implicit A: Write[A]): PreparedStatementIO[Unit] =
    A.set(n, a)

  /** Set the given writable value, starting at column `1`.
    * @group Parameters
    */
  def set[A](a: A)(implicit A: Write[A]): PreparedStatementIO[Unit] =
    A.set(1, a)

  /** @group Properties */
  def setCursorName(name: String): PreparedStatementIO[Unit] =
    IFPS.setCursorName(name)

  /** @group Properties */
  def setEscapeProcessing(a: Boolean): PreparedStatementIO[Unit] =
    IFPS.setEscapeProcessing(a)

  /** @group Properties */
  def setFetchDirection(fd: FetchDirection): PreparedStatementIO[Unit] =
    IFPS.setFetchDirection(fd.toInt)

  /** @group Properties */
  def setFetchSize(n: Int): PreparedStatementIO[Unit] =
    IFPS.setFetchSize(n)

  /** @group Properties */
  def setMaxFieldSize(n: Int): PreparedStatementIO[Unit] =
    IFPS.setMaxFieldSize(n)

  /** @group Properties */
  def setMaxRows(n: Int): PreparedStatementIO[Unit] =
    IFPS.setMaxRows(n)

  /** @group Properties */
  def setQueryTimeout(a: Int): PreparedStatementIO[Unit] =
    IFPS.setQueryTimeout(a)

}
