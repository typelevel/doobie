package doobie.free

#+scalaz
import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
#-scalaz
#+cats
import cats.~>
import cats.data.Kleisli
import cats.free.{ Free => F }
import scala.util.{ Either => \/ }
#-cats
#+fs2
import fs2.util.{ Catchable, Suspendable }
import fs2.interop.cats._
#-fs2

import doobie.util.capture._
import doobie.free.kleislitrans._

import java.io.InputStream
import java.io.Reader
import java.lang.Class
import java.lang.Object
import java.lang.String
import java.math.BigDecimal
import java.net.URL
import java.sql.Blob
import java.sql.CallableStatement
import java.sql.Clob
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.Date
import java.sql.Driver
import java.sql.NClob
import java.sql.ParameterMetaData
import java.sql.PreparedStatement
import java.sql.Ref
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.RowId
import java.sql.SQLData
import java.sql.SQLInput
import java.sql.SQLOutput
import java.sql.SQLType
import java.sql.SQLWarning
import java.sql.SQLXML
import java.sql.Statement
import java.sql.Time
import java.sql.Timestamp
import java.sql.{ Array => SqlArray }
import java.util.Calendar

import nclob.NClobIO
import blob.BlobIO
import clob.ClobIO
import databasemetadata.DatabaseMetaDataIO
import driver.DriverIO
import ref.RefIO
import sqldata.SQLDataIO
import sqlinput.SQLInputIO
import sqloutput.SQLOutputIO
import connection.ConnectionIO
import statement.StatementIO
import preparedstatement.PreparedStatementIO
import callablestatement.CallableStatementIO
import resultset.ResultSetIO

/**
 * Algebra and free monad for primitive operations over a `java.sql.PreparedStatement`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly
 * for library developers. End users will prefer a safer, higher-level API such as that provided
 * in the `doobie.hi` package.
 *
 * `PreparedStatementIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `PreparedStatementOp` to another monad via
 * `Free#foldMap`.
 *
 * The library provides a natural transformation to `Kleisli[M, PreparedStatement, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: PreparedStatementIO[Foo] = ...
 *
 * // A JDBC object
 * val s: PreparedStatement = ...
 *
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object preparedstatement extends PreparedStatementInstances {

  /**
   * Sum type of primitive operations over a `java.sql.PreparedStatement`.
   * @group Algebra
   */
  sealed trait PreparedStatementOp[A] {
#+scalaz
    protected def primitive[M[_]: Monad: Capture](f: PreparedStatement => A): Kleisli[M, PreparedStatement, A] =
      Kleisli((s: PreparedStatement) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, PreparedStatement, A]
#-scalaz
#+fs2
    protected def primitive[M[_]: Catchable: Suspendable](f: PreparedStatement => A): Kleisli[M, PreparedStatement, A] =
      Kleisli((s: PreparedStatement) => Predef.implicitly[Suspendable[M]].delay(f(s)))
    def defaultTransK[M[_]: Catchable: Suspendable]: Kleisli[M, PreparedStatement, A]
#-fs2
  }

  /**
   * Module of constructors for `PreparedStatementOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `preparedstatement` module.
   * @group Algebra
   */
  object PreparedStatementOp {

    // This algebra has a default interpreter
    implicit val PreparedStatementKleisliTrans: KleisliTrans.Aux[PreparedStatementOp, PreparedStatement] =
      new KleisliTrans[PreparedStatementOp] {
        type J = PreparedStatement
#+scalaz
        def interpK[M[_]: Monad: Catchable: Capture]: PreparedStatementOp ~> Kleisli[M, PreparedStatement, ?] =
#-scalaz
#+fs2
        def interpK[M[_]: Catchable: Suspendable]: PreparedStatementOp ~> Kleisli[M, PreparedStatement, ?] =
#-fs2
          new (PreparedStatementOp ~> Kleisli[M, PreparedStatement, ?]) {
            def apply[A](op: PreparedStatementOp[A]): Kleisli[M, PreparedStatement, A] =
              op.defaultTransK[M]
          }
      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F[Op, A], mod: KleisliTrans.Aux[Op, J]) extends PreparedStatementOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = Kleisli(_ => mod.transK[M].apply(action).run(j))
#-fs2
    }

    // Combinators
    case class Attempt[A](action: PreparedStatementIO[A]) extends PreparedStatementOp[Throwable \/ A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] =
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] =
#-fs2
        Predef.implicitly[Catchable[Kleisli[M, PreparedStatement, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends PreparedStatementOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_ => a())
#-fs2
    }
    case class Raw[A](f: PreparedStatement => A) extends PreparedStatementOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(f)
#-fs2
    }

    // Primitive Operations
#+scalaz
    case object AddBatch extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.addBatch())
    }
    case class  AddBatch1(a: String) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.addBatch(a))
    }
    case object Cancel extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.cancel())
    }
    case object ClearBatch extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.clearBatch())
    }
    case object ClearParameters extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.clearParameters())
    }
    case object ClearWarnings extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.clearWarnings())
    }
    case object Close extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.close())
    }
    case object CloseOnCompletion extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.closeOnCompletion())
    }
    case object Execute extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute())
    }
    case class  Execute1(a: String) extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a))
    }
    case class  Execute2(a: String, b: Array[Int]) extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a, b))
    }
    case class  Execute3(a: String, b: Array[String]) extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a, b))
    }
    case class  Execute4(a: String, b: Int) extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a, b))
    }
    case object ExecuteBatch extends PreparedStatementOp[Array[Int]] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeBatch())
    }
    case object ExecuteLargeBatch extends PreparedStatementOp[Array[Long]] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeLargeBatch())
    }
    case object ExecuteLargeUpdate extends PreparedStatementOp[Long] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeLargeUpdate())
    }
    case class  ExecuteLargeUpdate1(a: String) extends PreparedStatementOp[Long] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeLargeUpdate(a))
    }
    case class  ExecuteLargeUpdate2(a: String, b: Array[Int]) extends PreparedStatementOp[Long] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeLargeUpdate(a, b))
    }
    case class  ExecuteLargeUpdate3(a: String, b: Array[String]) extends PreparedStatementOp[Long] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeLargeUpdate(a, b))
    }
    case class  ExecuteLargeUpdate4(a: String, b: Int) extends PreparedStatementOp[Long] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeLargeUpdate(a, b))
    }
    case object ExecuteQuery extends PreparedStatementOp[ResultSet] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeQuery())
    }
    case class  ExecuteQuery1(a: String) extends PreparedStatementOp[ResultSet] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeQuery(a))
    }
    case object ExecuteUpdate extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate())
    }
    case class  ExecuteUpdate1(a: String) extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a))
    }
    case class  ExecuteUpdate2(a: String, b: Array[Int]) extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate3(a: String, b: Array[String]) extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate4(a: String, b: Int) extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
    }
    case object GetConnection extends PreparedStatementOp[Connection] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getConnection())
    }
    case object GetFetchDirection extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFetchDirection())
    }
    case object GetFetchSize extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFetchSize())
    }
    case object GetGeneratedKeys extends PreparedStatementOp[ResultSet] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getGeneratedKeys())
    }
    case object GetLargeMaxRows extends PreparedStatementOp[Long] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getLargeMaxRows())
    }
    case object GetLargeUpdateCount extends PreparedStatementOp[Long] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getLargeUpdateCount())
    }
    case object GetMaxFieldSize extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxFieldSize())
    }
    case object GetMaxRows extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxRows())
    }
    case object GetMetaData extends PreparedStatementOp[ResultSetMetaData] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMetaData())
    }
    case object GetMoreResults extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMoreResults())
    }
    case class  GetMoreResults1(a: Int) extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMoreResults(a))
    }
    case object GetParameterMetaData extends PreparedStatementOp[ParameterMetaData] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getParameterMetaData())
    }
    case object GetQueryTimeout extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getQueryTimeout())
    }
    case object GetResultSet extends PreparedStatementOp[ResultSet] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSet())
    }
    case object GetResultSetConcurrency extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSetConcurrency())
    }
    case object GetResultSetHoldability extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSetHoldability())
    }
    case object GetResultSetType extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSetType())
    }
    case object GetUpdateCount extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getUpdateCount())
    }
    case object GetWarnings extends PreparedStatementOp[SQLWarning] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getWarnings())
    }
    case object IsCloseOnCompletion extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isCloseOnCompletion())
    }
    case object IsClosed extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isClosed())
    }
    case object IsPoolable extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isPoolable())
    }
    case class  IsWrapperFor(a: Class[_]) extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isWrapperFor(a))
    }
    case class  SetArray(a: Int, b: SqlArray) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setArray(a, b))
    }
    case class  SetAsciiStream(a: Int, b: InputStream) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a, b))
    }
    case class  SetAsciiStream1(a: Int, b: InputStream, c: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a, b, c))
    }
    case class  SetAsciiStream2(a: Int, b: InputStream, c: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a, b, c))
    }
    case class  SetBigDecimal(a: Int, b: BigDecimal) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBigDecimal(a, b))
    }
    case class  SetBinaryStream(a: Int, b: InputStream) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBinaryStream(a, b))
    }
    case class  SetBinaryStream1(a: Int, b: InputStream, c: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBinaryStream(a, b, c))
    }
    case class  SetBinaryStream2(a: Int, b: InputStream, c: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBinaryStream(a, b, c))
    }
    case class  SetBlob(a: Int, b: Blob) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBlob(a, b))
    }
    case class  SetBlob1(a: Int, b: InputStream) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBlob(a, b))
    }
    case class  SetBlob2(a: Int, b: InputStream, c: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBlob(a, b, c))
    }
    case class  SetBoolean(a: Int, b: Boolean) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBoolean(a, b))
    }
    case class  SetByte(a: Int, b: Byte) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setByte(a, b))
    }
    case class  SetBytes(a: Int, b: Array[Byte]) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBytes(a, b))
    }
    case class  SetCharacterStream(a: Int, b: Reader) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a, b))
    }
    case class  SetCharacterStream1(a: Int, b: Reader, c: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a, b, c))
    }
    case class  SetCharacterStream2(a: Int, b: Reader, c: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a, b, c))
    }
    case class  SetClob(a: Int, b: Clob) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setClob(a, b))
    }
    case class  SetClob1(a: Int, b: Reader) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setClob(a, b))
    }
    case class  SetClob2(a: Int, b: Reader, c: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setClob(a, b, c))
    }
    case class  SetCursorName(a: String) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCursorName(a))
    }
    case class  SetDate(a: Int, b: Date) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setDate(a, b))
    }
    case class  SetDate1(a: Int, b: Date, c: Calendar) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setDate(a, b, c))
    }
    case class  SetDouble(a: Int, b: Double) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setDouble(a, b))
    }
    case class  SetEscapeProcessing(a: Boolean) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setEscapeProcessing(a))
    }
    case class  SetFetchDirection(a: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setFetchDirection(a))
    }
    case class  SetFetchSize(a: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setFetchSize(a))
    }
    case class  SetFloat(a: Int, b: Float) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setFloat(a, b))
    }
    case class  SetInt(a: Int, b: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setInt(a, b))
    }
    case class  SetLargeMaxRows(a: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setLargeMaxRows(a))
    }
    case class  SetLong(a: Int, b: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setLong(a, b))
    }
    case class  SetMaxFieldSize(a: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setMaxFieldSize(a))
    }
    case class  SetMaxRows(a: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setMaxRows(a))
    }
    case class  SetNCharacterStream(a: Int, b: Reader) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNCharacterStream(a, b))
    }
    case class  SetNCharacterStream1(a: Int, b: Reader, c: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNCharacterStream(a, b, c))
    }
    case class  SetNClob(a: Int, b: NClob) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNClob(a, b))
    }
    case class  SetNClob1(a: Int, b: Reader) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNClob(a, b))
    }
    case class  SetNClob2(a: Int, b: Reader, c: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNClob(a, b, c))
    }
    case class  SetNString(a: Int, b: String) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNString(a, b))
    }
    case class  SetNull(a: Int, b: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNull(a, b))
    }
    case class  SetNull1(a: Int, b: Int, c: String) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNull(a, b, c))
    }
    case class  SetObject(a: Int, b: AnyRef) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a, b))
    }
    case class  SetObject1(a: Int, b: AnyRef, c: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a, b, c))
    }
    case class  SetObject2(a: Int, b: AnyRef, c: Int, d: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a, b, c, d))
    }
    case class  SetObject3(a: Int, b: AnyRef, c: SQLType) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a, b, c))
    }
    case class  SetObject4(a: Int, b: AnyRef, c: SQLType, d: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a, b, c, d))
    }
    case class  SetPoolable(a: Boolean) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setPoolable(a))
    }
    case class  SetQueryTimeout(a: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setQueryTimeout(a))
    }
    case class  SetRef(a: Int, b: Ref) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setRef(a, b))
    }
    case class  SetRowId(a: Int, b: RowId) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setRowId(a, b))
    }
    case class  SetSQLXML(a: Int, b: SQLXML) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setSQLXML(a, b))
    }
    case class  SetShort(a: Int, b: Short) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setShort(a, b))
    }
    case class  SetString(a: Int, b: String) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setString(a, b))
    }
    case class  SetTime(a: Int, b: Time) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTime(a, b))
    }
    case class  SetTime1(a: Int, b: Time, c: Calendar) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTime(a, b, c))
    }
    case class  SetTimestamp(a: Int, b: Timestamp) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTimestamp(a, b))
    }
    case class  SetTimestamp1(a: Int, b: Timestamp, c: Calendar) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTimestamp(a, b, c))
    }
    case class  SetURL(a: Int, b: URL) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setURL(a, b))
    }
    case class  SetUnicodeStream(a: Int, b: InputStream, c: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setUnicodeStream(a, b, c))
    }
    case class  Unwrap[T](a: Class[T]) extends PreparedStatementOp[T] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.unwrap(a))
    }
#-scalaz
#+fs2
    case object AddBatch extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.addBatch())
    }
    case class  AddBatch1(a: String) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.addBatch(a))
    }
    case object Cancel extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.cancel())
    }
    case object ClearBatch extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.clearBatch())
    }
    case object ClearParameters extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.clearParameters())
    }
    case object ClearWarnings extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.clearWarnings())
    }
    case object Close extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.close())
    }
    case object CloseOnCompletion extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.closeOnCompletion())
    }
    case object Execute extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.execute())
    }
    case class  Execute1(a: String) extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.execute(a))
    }
    case class  Execute2(a: String, b: Array[Int]) extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.execute(a, b))
    }
    case class  Execute3(a: String, b: Array[String]) extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.execute(a, b))
    }
    case class  Execute4(a: String, b: Int) extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.execute(a, b))
    }
    case object ExecuteBatch extends PreparedStatementOp[Array[Int]] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.executeBatch())
    }
    case object ExecuteLargeBatch extends PreparedStatementOp[Array[Long]] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.executeLargeBatch())
    }
    case object ExecuteLargeUpdate extends PreparedStatementOp[Long] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.executeLargeUpdate())
    }
    case class  ExecuteLargeUpdate1(a: String) extends PreparedStatementOp[Long] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.executeLargeUpdate(a))
    }
    case class  ExecuteLargeUpdate2(a: String, b: Array[Int]) extends PreparedStatementOp[Long] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.executeLargeUpdate(a, b))
    }
    case class  ExecuteLargeUpdate3(a: String, b: Array[String]) extends PreparedStatementOp[Long] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.executeLargeUpdate(a, b))
    }
    case class  ExecuteLargeUpdate4(a: String, b: Int) extends PreparedStatementOp[Long] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.executeLargeUpdate(a, b))
    }
    case object ExecuteQuery extends PreparedStatementOp[ResultSet] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.executeQuery())
    }
    case class  ExecuteQuery1(a: String) extends PreparedStatementOp[ResultSet] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.executeQuery(a))
    }
    case object ExecuteUpdate extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.executeUpdate())
    }
    case class  ExecuteUpdate1(a: String) extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.executeUpdate(a))
    }
    case class  ExecuteUpdate2(a: String, b: Array[Int]) extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate3(a: String, b: Array[String]) extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate4(a: String, b: Int) extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.executeUpdate(a, b))
    }
    case object GetConnection extends PreparedStatementOp[Connection] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getConnection())
    }
    case object GetFetchDirection extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getFetchDirection())
    }
    case object GetFetchSize extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getFetchSize())
    }
    case object GetGeneratedKeys extends PreparedStatementOp[ResultSet] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getGeneratedKeys())
    }
    case object GetLargeMaxRows extends PreparedStatementOp[Long] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getLargeMaxRows())
    }
    case object GetLargeUpdateCount extends PreparedStatementOp[Long] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getLargeUpdateCount())
    }
    case object GetMaxFieldSize extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getMaxFieldSize())
    }
    case object GetMaxRows extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getMaxRows())
    }
    case object GetMetaData extends PreparedStatementOp[ResultSetMetaData] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getMetaData())
    }
    case object GetMoreResults extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getMoreResults())
    }
    case class  GetMoreResults1(a: Int) extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getMoreResults(a))
    }
    case object GetParameterMetaData extends PreparedStatementOp[ParameterMetaData] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getParameterMetaData())
    }
    case object GetQueryTimeout extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getQueryTimeout())
    }
    case object GetResultSet extends PreparedStatementOp[ResultSet] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getResultSet())
    }
    case object GetResultSetConcurrency extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getResultSetConcurrency())
    }
    case object GetResultSetHoldability extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getResultSetHoldability())
    }
    case object GetResultSetType extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getResultSetType())
    }
    case object GetUpdateCount extends PreparedStatementOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getUpdateCount())
    }
    case object GetWarnings extends PreparedStatementOp[SQLWarning] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getWarnings())
    }
    case object IsCloseOnCompletion extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.isCloseOnCompletion())
    }
    case object IsClosed extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.isClosed())
    }
    case object IsPoolable extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.isPoolable())
    }
    case class  IsWrapperFor(a: Class[_]) extends PreparedStatementOp[Boolean] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.isWrapperFor(a))
    }
    case class  SetArray(a: Int, b: SqlArray) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setArray(a, b))
    }
    case class  SetAsciiStream(a: Int, b: InputStream) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setAsciiStream(a, b))
    }
    case class  SetAsciiStream1(a: Int, b: InputStream, c: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setAsciiStream(a, b, c))
    }
    case class  SetAsciiStream2(a: Int, b: InputStream, c: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setAsciiStream(a, b, c))
    }
    case class  SetBigDecimal(a: Int, b: BigDecimal) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setBigDecimal(a, b))
    }
    case class  SetBinaryStream(a: Int, b: InputStream) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setBinaryStream(a, b))
    }
    case class  SetBinaryStream1(a: Int, b: InputStream, c: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setBinaryStream(a, b, c))
    }
    case class  SetBinaryStream2(a: Int, b: InputStream, c: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setBinaryStream(a, b, c))
    }
    case class  SetBlob(a: Int, b: Blob) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setBlob(a, b))
    }
    case class  SetBlob1(a: Int, b: InputStream) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setBlob(a, b))
    }
    case class  SetBlob2(a: Int, b: InputStream, c: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setBlob(a, b, c))
    }
    case class  SetBoolean(a: Int, b: Boolean) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setBoolean(a, b))
    }
    case class  SetByte(a: Int, b: Byte) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setByte(a, b))
    }
    case class  SetBytes(a: Int, b: Array[Byte]) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setBytes(a, b))
    }
    case class  SetCharacterStream(a: Int, b: Reader) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setCharacterStream(a, b))
    }
    case class  SetCharacterStream1(a: Int, b: Reader, c: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setCharacterStream(a, b, c))
    }
    case class  SetCharacterStream2(a: Int, b: Reader, c: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setCharacterStream(a, b, c))
    }
    case class  SetClob(a: Int, b: Clob) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setClob(a, b))
    }
    case class  SetClob1(a: Int, b: Reader) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setClob(a, b))
    }
    case class  SetClob2(a: Int, b: Reader, c: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setClob(a, b, c))
    }
    case class  SetCursorName(a: String) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setCursorName(a))
    }
    case class  SetDate(a: Int, b: Date) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setDate(a, b))
    }
    case class  SetDate1(a: Int, b: Date, c: Calendar) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setDate(a, b, c))
    }
    case class  SetDouble(a: Int, b: Double) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setDouble(a, b))
    }
    case class  SetEscapeProcessing(a: Boolean) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setEscapeProcessing(a))
    }
    case class  SetFetchDirection(a: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setFetchDirection(a))
    }
    case class  SetFetchSize(a: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setFetchSize(a))
    }
    case class  SetFloat(a: Int, b: Float) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setFloat(a, b))
    }
    case class  SetInt(a: Int, b: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setInt(a, b))
    }
    case class  SetLargeMaxRows(a: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setLargeMaxRows(a))
    }
    case class  SetLong(a: Int, b: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setLong(a, b))
    }
    case class  SetMaxFieldSize(a: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setMaxFieldSize(a))
    }
    case class  SetMaxRows(a: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setMaxRows(a))
    }
    case class  SetNCharacterStream(a: Int, b: Reader) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setNCharacterStream(a, b))
    }
    case class  SetNCharacterStream1(a: Int, b: Reader, c: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setNCharacterStream(a, b, c))
    }
    case class  SetNClob(a: Int, b: NClob) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setNClob(a, b))
    }
    case class  SetNClob1(a: Int, b: Reader) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setNClob(a, b))
    }
    case class  SetNClob2(a: Int, b: Reader, c: Long) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setNClob(a, b, c))
    }
    case class  SetNString(a: Int, b: String) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setNString(a, b))
    }
    case class  SetNull(a: Int, b: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setNull(a, b))
    }
    case class  SetNull1(a: Int, b: Int, c: String) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setNull(a, b, c))
    }
    case class  SetObject(a: Int, b: AnyRef) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setObject(a, b))
    }
    case class  SetObject1(a: Int, b: AnyRef, c: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setObject(a, b, c))
    }
    case class  SetObject2(a: Int, b: AnyRef, c: Int, d: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setObject(a, b, c, d))
    }
    case class  SetObject3(a: Int, b: AnyRef, c: SQLType) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setObject(a, b, c))
    }
    case class  SetObject4(a: Int, b: AnyRef, c: SQLType, d: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setObject(a, b, c, d))
    }
    case class  SetPoolable(a: Boolean) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setPoolable(a))
    }
    case class  SetQueryTimeout(a: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setQueryTimeout(a))
    }
    case class  SetRef(a: Int, b: Ref) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setRef(a, b))
    }
    case class  SetRowId(a: Int, b: RowId) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setRowId(a, b))
    }
    case class  SetSQLXML(a: Int, b: SQLXML) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setSQLXML(a, b))
    }
    case class  SetShort(a: Int, b: Short) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setShort(a, b))
    }
    case class  SetString(a: Int, b: String) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setString(a, b))
    }
    case class  SetTime(a: Int, b: Time) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setTime(a, b))
    }
    case class  SetTime1(a: Int, b: Time, c: Calendar) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setTime(a, b, c))
    }
    case class  SetTimestamp(a: Int, b: Timestamp) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setTimestamp(a, b))
    }
    case class  SetTimestamp1(a: Int, b: Timestamp, c: Calendar) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setTimestamp(a, b, c))
    }
    case class  SetURL(a: Int, b: URL) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setURL(a, b))
    }
    case class  SetUnicodeStream(a: Int, b: InputStream, c: Int) extends PreparedStatementOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setUnicodeStream(a, b, c))
    }
    case class  Unwrap[T](a: Class[T]) extends PreparedStatementOp[T] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.unwrap(a))
    }
#-fs2

  }
  import PreparedStatementOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[PreparedStatementOp]]; abstractly, a computation that consumes
   * a `java.sql.PreparedStatement` and produces a value of type `A`.
   * @group Algebra
   */
  type PreparedStatementIO[A] = F[PreparedStatementOp, A]

  /**
   * Catchable instance for [[PreparedStatementIO]].
   * @group Typeclass Instances
   */
  implicit val CatchablePreparedStatementIO: Catchable[PreparedStatementIO] =
    new Catchable[PreparedStatementIO] {
#+fs2
      def pure[A](a: A): PreparedStatementIO[A] = preparedstatement.delay(a)
      override def map[A, B](fa: PreparedStatementIO[A])(f: A => B): PreparedStatementIO[B] = fa.map(f)
      def flatMap[A, B](fa: PreparedStatementIO[A])(f: A => PreparedStatementIO[B]): PreparedStatementIO[B] = fa.flatMap(f)
#-fs2
      def attempt[A](f: PreparedStatementIO[A]): PreparedStatementIO[Throwable \/ A] = preparedstatement.attempt(f)
      def fail[A](err: Throwable): PreparedStatementIO[A] = preparedstatement.delay(throw err)
    }

#+scalaz
  /**
   * Capture instance for [[PreparedStatementIO]].
   * @group Typeclass Instances
   */
  implicit val CapturePreparedStatementIO: Capture[PreparedStatementIO] =
    new Capture[PreparedStatementIO] {
      def apply[A](a: => A): PreparedStatementIO[A] = preparedstatement.delay(a)
    }
#-scalaz

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): PreparedStatementIO[A] =
    F.liftF(Lift(j, action, mod))

  /**
   * Lift a PreparedStatementIO[A] into an exception-capturing PreparedStatementIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: PreparedStatementIO[A]): PreparedStatementIO[Throwable \/ A] =
    F.liftF[PreparedStatementOp, Throwable \/ A](Attempt(a))

  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): PreparedStatementIO[A] =
    F.liftF(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying PreparedStatement.
   * @group Constructors (Lifting)
   */
  def raw[A](f: PreparedStatement => A): PreparedStatementIO[A] =
    F.liftF(Raw(f))

  /**
   * @group Constructors (Primitives)
   */
  val addBatch: PreparedStatementIO[Unit] =
    F.liftF(AddBatch)

  /**
   * @group Constructors (Primitives)
   */
  def addBatch(a: String): PreparedStatementIO[Unit] =
    F.liftF(AddBatch1(a))

  /**
   * @group Constructors (Primitives)
   */
  val cancel: PreparedStatementIO[Unit] =
    F.liftF(Cancel)

  /**
   * @group Constructors (Primitives)
   */
  val clearBatch: PreparedStatementIO[Unit] =
    F.liftF(ClearBatch)

  /**
   * @group Constructors (Primitives)
   */
  val clearParameters: PreparedStatementIO[Unit] =
    F.liftF(ClearParameters)

  /**
   * @group Constructors (Primitives)
   */
  val clearWarnings: PreparedStatementIO[Unit] =
    F.liftF(ClearWarnings)

  /**
   * @group Constructors (Primitives)
   */
  val close: PreparedStatementIO[Unit] =
    F.liftF(Close)

  /**
   * @group Constructors (Primitives)
   */
  val closeOnCompletion: PreparedStatementIO[Unit] =
    F.liftF(CloseOnCompletion)

  /**
   * @group Constructors (Primitives)
   */
  val execute: PreparedStatementIO[Boolean] =
    F.liftF(Execute)

  /**
   * @group Constructors (Primitives)
   */
  def execute(a: String): PreparedStatementIO[Boolean] =
    F.liftF(Execute1(a))

  /**
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Array[Int]): PreparedStatementIO[Boolean] =
    F.liftF(Execute2(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Array[String]): PreparedStatementIO[Boolean] =
    F.liftF(Execute3(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Int): PreparedStatementIO[Boolean] =
    F.liftF(Execute4(a, b))

  /**
   * @group Constructors (Primitives)
   */
  val executeBatch: PreparedStatementIO[Array[Int]] =
    F.liftF(ExecuteBatch)

  /**
   * @group Constructors (Primitives)
   */
  val executeLargeBatch: PreparedStatementIO[Array[Long]] =
    F.liftF(ExecuteLargeBatch)

  /**
   * @group Constructors (Primitives)
   */
  val executeLargeUpdate: PreparedStatementIO[Long] =
    F.liftF(ExecuteLargeUpdate)

  /**
   * @group Constructors (Primitives)
   */
  def executeLargeUpdate(a: String): PreparedStatementIO[Long] =
    F.liftF(ExecuteLargeUpdate1(a))

  /**
   * @group Constructors (Primitives)
   */
  def executeLargeUpdate(a: String, b: Array[Int]): PreparedStatementIO[Long] =
    F.liftF(ExecuteLargeUpdate2(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def executeLargeUpdate(a: String, b: Array[String]): PreparedStatementIO[Long] =
    F.liftF(ExecuteLargeUpdate3(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def executeLargeUpdate(a: String, b: Int): PreparedStatementIO[Long] =
    F.liftF(ExecuteLargeUpdate4(a, b))

  /**
   * @group Constructors (Primitives)
   */
  val executeQuery: PreparedStatementIO[ResultSet] =
    F.liftF(ExecuteQuery)

  /**
   * @group Constructors (Primitives)
   */
  def executeQuery(a: String): PreparedStatementIO[ResultSet] =
    F.liftF(ExecuteQuery1(a))

  /**
   * @group Constructors (Primitives)
   */
  val executeUpdate: PreparedStatementIO[Int] =
    F.liftF(ExecuteUpdate)

  /**
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String): PreparedStatementIO[Int] =
    F.liftF(ExecuteUpdate1(a))

  /**
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Array[Int]): PreparedStatementIO[Int] =
    F.liftF(ExecuteUpdate2(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Array[String]): PreparedStatementIO[Int] =
    F.liftF(ExecuteUpdate3(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Int): PreparedStatementIO[Int] =
    F.liftF(ExecuteUpdate4(a, b))

  /**
   * @group Constructors (Primitives)
   */
  val getConnection: PreparedStatementIO[Connection] =
    F.liftF(GetConnection)

  /**
   * @group Constructors (Primitives)
   */
  val getFetchDirection: PreparedStatementIO[Int] =
    F.liftF(GetFetchDirection)

  /**
   * @group Constructors (Primitives)
   */
  val getFetchSize: PreparedStatementIO[Int] =
    F.liftF(GetFetchSize)

  /**
   * @group Constructors (Primitives)
   */
  val getGeneratedKeys: PreparedStatementIO[ResultSet] =
    F.liftF(GetGeneratedKeys)

  /**
   * @group Constructors (Primitives)
   */
  val getLargeMaxRows: PreparedStatementIO[Long] =
    F.liftF(GetLargeMaxRows)

  /**
   * @group Constructors (Primitives)
   */
  val getLargeUpdateCount: PreparedStatementIO[Long] =
    F.liftF(GetLargeUpdateCount)

  /**
   * @group Constructors (Primitives)
   */
  val getMaxFieldSize: PreparedStatementIO[Int] =
    F.liftF(GetMaxFieldSize)

  /**
   * @group Constructors (Primitives)
   */
  val getMaxRows: PreparedStatementIO[Int] =
    F.liftF(GetMaxRows)

  /**
   * @group Constructors (Primitives)
   */
  val getMetaData: PreparedStatementIO[ResultSetMetaData] =
    F.liftF(GetMetaData)

  /**
   * @group Constructors (Primitives)
   */
  val getMoreResults: PreparedStatementIO[Boolean] =
    F.liftF(GetMoreResults)

  /**
   * @group Constructors (Primitives)
   */
  def getMoreResults(a: Int): PreparedStatementIO[Boolean] =
    F.liftF(GetMoreResults1(a))

  /**
   * @group Constructors (Primitives)
   */
  val getParameterMetaData: PreparedStatementIO[ParameterMetaData] =
    F.liftF(GetParameterMetaData)

  /**
   * @group Constructors (Primitives)
   */
  val getQueryTimeout: PreparedStatementIO[Int] =
    F.liftF(GetQueryTimeout)

  /**
   * @group Constructors (Primitives)
   */
  val getResultSet: PreparedStatementIO[ResultSet] =
    F.liftF(GetResultSet)

  /**
   * @group Constructors (Primitives)
   */
  val getResultSetConcurrency: PreparedStatementIO[Int] =
    F.liftF(GetResultSetConcurrency)

  /**
   * @group Constructors (Primitives)
   */
  val getResultSetHoldability: PreparedStatementIO[Int] =
    F.liftF(GetResultSetHoldability)

  /**
   * @group Constructors (Primitives)
   */
  val getResultSetType: PreparedStatementIO[Int] =
    F.liftF(GetResultSetType)

  /**
   * @group Constructors (Primitives)
   */
  val getUpdateCount: PreparedStatementIO[Int] =
    F.liftF(GetUpdateCount)

  /**
   * @group Constructors (Primitives)
   */
  val getWarnings: PreparedStatementIO[SQLWarning] =
    F.liftF(GetWarnings)

  /**
   * @group Constructors (Primitives)
   */
  val isCloseOnCompletion: PreparedStatementIO[Boolean] =
    F.liftF(IsCloseOnCompletion)

  /**
   * @group Constructors (Primitives)
   */
  val isClosed: PreparedStatementIO[Boolean] =
    F.liftF(IsClosed)

  /**
   * @group Constructors (Primitives)
   */
  val isPoolable: PreparedStatementIO[Boolean] =
    F.liftF(IsPoolable)

  /**
   * @group Constructors (Primitives)
   */
  def isWrapperFor(a: Class[_]): PreparedStatementIO[Boolean] =
    F.liftF(IsWrapperFor(a))

  /**
   * @group Constructors (Primitives)
   */
  def setArray(a: Int, b: SqlArray): PreparedStatementIO[Unit] =
    F.liftF(SetArray(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: Int, b: InputStream): PreparedStatementIO[Unit] =
    F.liftF(SetAsciiStream(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: Int, b: InputStream, c: Int): PreparedStatementIO[Unit] =
    F.liftF(SetAsciiStream1(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: Int, b: InputStream, c: Long): PreparedStatementIO[Unit] =
    F.liftF(SetAsciiStream2(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setBigDecimal(a: Int, b: BigDecimal): PreparedStatementIO[Unit] =
    F.liftF(SetBigDecimal(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: Int, b: InputStream): PreparedStatementIO[Unit] =
    F.liftF(SetBinaryStream(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: Int, b: InputStream, c: Int): PreparedStatementIO[Unit] =
    F.liftF(SetBinaryStream1(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: Int, b: InputStream, c: Long): PreparedStatementIO[Unit] =
    F.liftF(SetBinaryStream2(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setBlob(a: Int, b: Blob): PreparedStatementIO[Unit] =
    F.liftF(SetBlob(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setBlob(a: Int, b: InputStream): PreparedStatementIO[Unit] =
    F.liftF(SetBlob1(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setBlob(a: Int, b: InputStream, c: Long): PreparedStatementIO[Unit] =
    F.liftF(SetBlob2(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setBoolean(a: Int, b: Boolean): PreparedStatementIO[Unit] =
    F.liftF(SetBoolean(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setByte(a: Int, b: Byte): PreparedStatementIO[Unit] =
    F.liftF(SetByte(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setBytes(a: Int, b: Array[Byte]): PreparedStatementIO[Unit] =
    F.liftF(SetBytes(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: Int, b: Reader): PreparedStatementIO[Unit] =
    F.liftF(SetCharacterStream(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: Int, b: Reader, c: Int): PreparedStatementIO[Unit] =
    F.liftF(SetCharacterStream1(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: Int, b: Reader, c: Long): PreparedStatementIO[Unit] =
    F.liftF(SetCharacterStream2(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setClob(a: Int, b: Clob): PreparedStatementIO[Unit] =
    F.liftF(SetClob(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setClob(a: Int, b: Reader): PreparedStatementIO[Unit] =
    F.liftF(SetClob1(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setClob(a: Int, b: Reader, c: Long): PreparedStatementIO[Unit] =
    F.liftF(SetClob2(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setCursorName(a: String): PreparedStatementIO[Unit] =
    F.liftF(SetCursorName(a))

  /**
   * @group Constructors (Primitives)
   */
  def setDate(a: Int, b: Date): PreparedStatementIO[Unit] =
    F.liftF(SetDate(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setDate(a: Int, b: Date, c: Calendar): PreparedStatementIO[Unit] =
    F.liftF(SetDate1(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setDouble(a: Int, b: Double): PreparedStatementIO[Unit] =
    F.liftF(SetDouble(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setEscapeProcessing(a: Boolean): PreparedStatementIO[Unit] =
    F.liftF(SetEscapeProcessing(a))

  /**
   * @group Constructors (Primitives)
   */
  def setFetchDirection(a: Int): PreparedStatementIO[Unit] =
    F.liftF(SetFetchDirection(a))

  /**
   * @group Constructors (Primitives)
   */
  def setFetchSize(a: Int): PreparedStatementIO[Unit] =
    F.liftF(SetFetchSize(a))

  /**
   * @group Constructors (Primitives)
   */
  def setFloat(a: Int, b: Float): PreparedStatementIO[Unit] =
    F.liftF(SetFloat(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setInt(a: Int, b: Int): PreparedStatementIO[Unit] =
    F.liftF(SetInt(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setLargeMaxRows(a: Long): PreparedStatementIO[Unit] =
    F.liftF(SetLargeMaxRows(a))

  /**
   * @group Constructors (Primitives)
   */
  def setLong(a: Int, b: Long): PreparedStatementIO[Unit] =
    F.liftF(SetLong(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setMaxFieldSize(a: Int): PreparedStatementIO[Unit] =
    F.liftF(SetMaxFieldSize(a))

  /**
   * @group Constructors (Primitives)
   */
  def setMaxRows(a: Int): PreparedStatementIO[Unit] =
    F.liftF(SetMaxRows(a))

  /**
   * @group Constructors (Primitives)
   */
  def setNCharacterStream(a: Int, b: Reader): PreparedStatementIO[Unit] =
    F.liftF(SetNCharacterStream(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setNCharacterStream(a: Int, b: Reader, c: Long): PreparedStatementIO[Unit] =
    F.liftF(SetNCharacterStream1(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setNClob(a: Int, b: NClob): PreparedStatementIO[Unit] =
    F.liftF(SetNClob(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setNClob(a: Int, b: Reader): PreparedStatementIO[Unit] =
    F.liftF(SetNClob1(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setNClob(a: Int, b: Reader, c: Long): PreparedStatementIO[Unit] =
    F.liftF(SetNClob2(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setNString(a: Int, b: String): PreparedStatementIO[Unit] =
    F.liftF(SetNString(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setNull(a: Int, b: Int): PreparedStatementIO[Unit] =
    F.liftF(SetNull(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setNull(a: Int, b: Int, c: String): PreparedStatementIO[Unit] =
    F.liftF(SetNull1(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setObject(a: Int, b: AnyRef): PreparedStatementIO[Unit] =
    F.liftF(SetObject(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setObject(a: Int, b: AnyRef, c: Int): PreparedStatementIO[Unit] =
    F.liftF(SetObject1(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setObject(a: Int, b: AnyRef, c: Int, d: Int): PreparedStatementIO[Unit] =
    F.liftF(SetObject2(a, b, c, d))

  /**
   * @group Constructors (Primitives)
   */
  def setObject(a: Int, b: AnyRef, c: SQLType): PreparedStatementIO[Unit] =
    F.liftF(SetObject3(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setObject(a: Int, b: AnyRef, c: SQLType, d: Int): PreparedStatementIO[Unit] =
    F.liftF(SetObject4(a, b, c, d))

  /**
   * @group Constructors (Primitives)
   */
  def setPoolable(a: Boolean): PreparedStatementIO[Unit] =
    F.liftF(SetPoolable(a))

  /**
   * @group Constructors (Primitives)
   */
  def setQueryTimeout(a: Int): PreparedStatementIO[Unit] =
    F.liftF(SetQueryTimeout(a))

  /**
   * @group Constructors (Primitives)
   */
  def setRef(a: Int, b: Ref): PreparedStatementIO[Unit] =
    F.liftF(SetRef(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setRowId(a: Int, b: RowId): PreparedStatementIO[Unit] =
    F.liftF(SetRowId(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setSQLXML(a: Int, b: SQLXML): PreparedStatementIO[Unit] =
    F.liftF(SetSQLXML(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setShort(a: Int, b: Short): PreparedStatementIO[Unit] =
    F.liftF(SetShort(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setString(a: Int, b: String): PreparedStatementIO[Unit] =
    F.liftF(SetString(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setTime(a: Int, b: Time): PreparedStatementIO[Unit] =
    F.liftF(SetTime(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setTime(a: Int, b: Time, c: Calendar): PreparedStatementIO[Unit] =
    F.liftF(SetTime1(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setTimestamp(a: Int, b: Timestamp): PreparedStatementIO[Unit] =
    F.liftF(SetTimestamp(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setTimestamp(a: Int, b: Timestamp, c: Calendar): PreparedStatementIO[Unit] =
    F.liftF(SetTimestamp1(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def setURL(a: Int, b: URL): PreparedStatementIO[Unit] =
    F.liftF(SetURL(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setUnicodeStream(a: Int, b: InputStream, c: Int): PreparedStatementIO[Unit] =
    F.liftF(SetUnicodeStream(a, b, c))

  /**
   * @group Constructors (Primitives)
   */
  def unwrap[T](a: Class[T]): PreparedStatementIO[T] =
    F.liftF(Unwrap(a))

 /**
  * Natural transformation from `PreparedStatementOp` to `Kleisli` for the given `M`, consuming a `java.sql.PreparedStatement`.
  * @group Algebra
  */
#+scalaz
  def interpK[M[_]: Monad: Catchable: Capture]: PreparedStatementOp ~> Kleisli[M, PreparedStatement, ?] =
   PreparedStatementOp.PreparedStatementKleisliTrans.interpK
#-scalaz
#+fs2
  def interpK[M[_]: Catchable: Suspendable]: PreparedStatementOp ~> Kleisli[M, PreparedStatement, ?] =
   PreparedStatementOp.PreparedStatementKleisliTrans.interpK
#-fs2

 /**
  * Natural transformation from `PreparedStatementIO` to `Kleisli` for the given `M`, consuming a `java.sql.PreparedStatement`.
  * @group Algebra
  */
#+scalaz
  def transK[M[_]: Monad: Catchable: Capture]: PreparedStatementIO ~> Kleisli[M, PreparedStatement, ?] =
   PreparedStatementOp.PreparedStatementKleisliTrans.transK
#-scalaz
#+fs2
  def transK[M[_]: Catchable: Suspendable]: PreparedStatementIO ~> Kleisli[M, PreparedStatement, ?] =
   PreparedStatementOp.PreparedStatementKleisliTrans.transK
#-fs2

 /**
  * Natural transformation from `PreparedStatementIO` to `M`, given a `java.sql.PreparedStatement`.
  * @group Algebra
  */
#+scalaz
 def trans[M[_]: Monad: Catchable: Capture](c: PreparedStatement): PreparedStatementIO ~> M =
   PreparedStatementOp.PreparedStatementKleisliTrans.trans[M](c)
#-scalaz
#+fs2
 def trans[M[_]: Catchable: Suspendable](c: PreparedStatement): PreparedStatementIO ~> M =
   PreparedStatementOp.PreparedStatementKleisliTrans.trans[M](c)
#-fs2

  /**
   * Syntax for `PreparedStatementIO`.
   * @group Algebra
   */
  implicit class PreparedStatementIOOps[A](ma: PreparedStatementIO[A]) {
#+scalaz
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, PreparedStatement, A] =
      PreparedStatementOp.PreparedStatementKleisliTrans.transK[M].apply(ma)
#-scalaz
#+fs2
    def transK[M[_]: Catchable: Suspendable]: Kleisli[M, PreparedStatement, A] =
      PreparedStatementOp.PreparedStatementKleisliTrans.transK[M].apply(ma)
#-fs2
  }

}

private[free] trait PreparedStatementInstances {
#+fs2
  /**
   * Suspendable instance for [[PreparedStatementIO]].
   * @group Typeclass Instances
   */
  implicit val SuspendablePreparedStatementIO: Suspendable[PreparedStatementIO] =
    new Suspendable[PreparedStatementIO] {
      def pure[A](a: A): PreparedStatementIO[A] = preparedstatement.delay(a)
      override def map[A, B](fa: PreparedStatementIO[A])(f: A => B): PreparedStatementIO[B] = fa.map(f)
      def flatMap[A, B](fa: PreparedStatementIO[A])(f: A => PreparedStatementIO[B]): PreparedStatementIO[B] = fa.flatMap(f)
      def suspend[A](fa: => PreparedStatementIO[A]): PreparedStatementIO[A] = F.suspend(fa)
      override def delay[A](a: => A): PreparedStatementIO[A] = preparedstatement.delay(a)
    }
#-fs2
}

