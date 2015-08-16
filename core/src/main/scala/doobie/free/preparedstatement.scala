package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

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
 * `Free.runFC`. 
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
object preparedstatement {
  
  /** 
   * Sum type of primitive operations over a `java.sql.PreparedStatement`.
   * @group Algebra 
   */
  sealed trait PreparedStatementOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: PreparedStatement => A): Kleisli[M, PreparedStatement, A] = 
      Kleisli((s: PreparedStatement) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, PreparedStatement, A]
  }

  /** 
   * Module of constructors for `PreparedStatementOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `preparedstatement` module.
   * @group Algebra 
   */
  object PreparedStatementOp {
    
    // Lifting
    case class LiftBlobIO[A](s: Blob, action: BlobIO[A]) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftCallableStatementIO[A](s: CallableStatement, action: CallableStatementIO[A]) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftClobIO[A](s: Clob, action: ClobIO[A]) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftConnectionIO[A](s: Connection, action: ConnectionIO[A]) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDatabaseMetaDataIO[A](s: DatabaseMetaData, action: DatabaseMetaDataIO[A]) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDriverIO[A](s: Driver, action: DriverIO[A]) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftNClobIO[A](s: NClob, action: NClobIO[A]) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftRefIO[A](s: Ref, action: RefIO[A]) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftResultSetIO[A](s: ResultSet, action: ResultSetIO[A]) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLDataIO[A](s: SQLData, action: SQLDataIO[A]) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLInputIO[A](s: SQLInput, action: SQLInputIO[A]) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLOutputIO[A](s: SQLOutput, action: SQLOutputIO[A]) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftStatementIO[A](s: Statement, action: StatementIO[A]) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }

    // Combinators
    case class Attempt[A](action: PreparedStatementIO[A]) extends PreparedStatementOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, PreparedStatement, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: PreparedStatement => A) extends PreparedStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case object AddBatch extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.addBatch())
    }
    case class  AddBatch1(a: String) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.addBatch(a))
    }
    case object Cancel extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.cancel())
    }
    case object ClearBatch extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.clearBatch())
    }
    case object ClearParameters extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.clearParameters())
    }
    case object ClearWarnings extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.clearWarnings())
    }
    case object Close extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.close())
    }
    case object CloseOnCompletion extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.closeOnCompletion())
    }
    case object Execute extends PreparedStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute())
    }
    case class  Execute1(a: String, b: Int) extends PreparedStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a, b))
    }
    case class  Execute2(a: String, b: Array[Int]) extends PreparedStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a, b))
    }
    case class  Execute3(a: String) extends PreparedStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a))
    }
    case class  Execute4(a: String, b: Array[String]) extends PreparedStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a, b))
    }
    case object ExecuteBatch extends PreparedStatementOp[Array[Int]] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeBatch())
    }
    case object ExecuteQuery extends PreparedStatementOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeQuery())
    }
    case class  ExecuteQuery1(a: String) extends PreparedStatementOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeQuery(a))
    }
    case object ExecuteUpdate extends PreparedStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate())
    }
    case class  ExecuteUpdate1(a: String, b: Int) extends PreparedStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate2(a: String, b: Array[Int]) extends PreparedStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate3(a: String, b: Array[String]) extends PreparedStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate4(a: String) extends PreparedStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a))
    }
    case object GetConnection extends PreparedStatementOp[Connection] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getConnection())
    }
    case object GetFetchDirection extends PreparedStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFetchDirection())
    }
    case object GetFetchSize extends PreparedStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFetchSize())
    }
    case object GetGeneratedKeys extends PreparedStatementOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getGeneratedKeys())
    }
    case object GetMaxFieldSize extends PreparedStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxFieldSize())
    }
    case object GetMaxRows extends PreparedStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxRows())
    }
    case object GetMetaData extends PreparedStatementOp[ResultSetMetaData] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMetaData())
    }
    case class  GetMoreResults(a: Int) extends PreparedStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMoreResults(a))
    }
    case object GetMoreResults1 extends PreparedStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMoreResults())
    }
    case object GetParameterMetaData extends PreparedStatementOp[ParameterMetaData] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getParameterMetaData())
    }
    case object GetQueryTimeout extends PreparedStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getQueryTimeout())
    }
    case object GetResultSet extends PreparedStatementOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSet())
    }
    case object GetResultSetConcurrency extends PreparedStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSetConcurrency())
    }
    case object GetResultSetHoldability extends PreparedStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSetHoldability())
    }
    case object GetResultSetType extends PreparedStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSetType())
    }
    case object GetUpdateCount extends PreparedStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getUpdateCount())
    }
    case object GetWarnings extends PreparedStatementOp[SQLWarning] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getWarnings())
    }
    case object IsCloseOnCompletion extends PreparedStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isCloseOnCompletion())
    }
    case object IsClosed extends PreparedStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isClosed())
    }
    case object IsPoolable extends PreparedStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isPoolable())
    }
    case class  IsWrapperFor(a: Class[_]) extends PreparedStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isWrapperFor(a))
    }
    case class  SetArray(a: Int, b: SqlArray) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setArray(a, b))
    }
    case class  SetAsciiStream(a: Int, b: InputStream, c: Long) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a, b, c))
    }
    case class  SetAsciiStream1(a: Int, b: InputStream, c: Int) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a, b, c))
    }
    case class  SetAsciiStream2(a: Int, b: InputStream) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a, b))
    }
    case class  SetBigDecimal(a: Int, b: BigDecimal) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBigDecimal(a, b))
    }
    case class  SetBinaryStream(a: Int, b: InputStream) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBinaryStream(a, b))
    }
    case class  SetBinaryStream1(a: Int, b: InputStream, c: Int) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBinaryStream(a, b, c))
    }
    case class  SetBinaryStream2(a: Int, b: InputStream, c: Long) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBinaryStream(a, b, c))
    }
    case class  SetBlob(a: Int, b: InputStream, c: Long) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBlob(a, b, c))
    }
    case class  SetBlob1(a: Int, b: Blob) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBlob(a, b))
    }
    case class  SetBlob2(a: Int, b: InputStream) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBlob(a, b))
    }
    case class  SetBoolean(a: Int, b: Boolean) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBoolean(a, b))
    }
    case class  SetByte(a: Int, b: Byte) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setByte(a, b))
    }
    case class  SetBytes(a: Int, b: Array[Byte]) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBytes(a, b))
    }
    case class  SetCharacterStream(a: Int, b: Reader, c: Long) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a, b, c))
    }
    case class  SetCharacterStream1(a: Int, b: Reader, c: Int) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a, b, c))
    }
    case class  SetCharacterStream2(a: Int, b: Reader) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a, b))
    }
    case class  SetClob(a: Int, b: Reader, c: Long) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setClob(a, b, c))
    }
    case class  SetClob1(a: Int, b: Reader) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setClob(a, b))
    }
    case class  SetClob2(a: Int, b: Clob) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setClob(a, b))
    }
    case class  SetCursorName(a: String) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCursorName(a))
    }
    case class  SetDate(a: Int, b: Date, c: Calendar) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setDate(a, b, c))
    }
    case class  SetDate1(a: Int, b: Date) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setDate(a, b))
    }
    case class  SetDouble(a: Int, b: Double) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setDouble(a, b))
    }
    case class  SetEscapeProcessing(a: Boolean) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setEscapeProcessing(a))
    }
    case class  SetFetchDirection(a: Int) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setFetchDirection(a))
    }
    case class  SetFetchSize(a: Int) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setFetchSize(a))
    }
    case class  SetFloat(a: Int, b: Float) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setFloat(a, b))
    }
    case class  SetInt(a: Int, b: Int) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setInt(a, b))
    }
    case class  SetLong(a: Int, b: Long) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setLong(a, b))
    }
    case class  SetMaxFieldSize(a: Int) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setMaxFieldSize(a))
    }
    case class  SetMaxRows(a: Int) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setMaxRows(a))
    }
    case class  SetNCharacterStream(a: Int, b: Reader) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNCharacterStream(a, b))
    }
    case class  SetNCharacterStream1(a: Int, b: Reader, c: Long) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNCharacterStream(a, b, c))
    }
    case class  SetNClob(a: Int, b: Reader) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNClob(a, b))
    }
    case class  SetNClob1(a: Int, b: Reader, c: Long) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNClob(a, b, c))
    }
    case class  SetNClob2(a: Int, b: NClob) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNClob(a, b))
    }
    case class  SetNString(a: Int, b: String) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNString(a, b))
    }
    case class  SetNull(a: Int, b: Int, c: String) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNull(a, b, c))
    }
    case class  SetNull1(a: Int, b: Int) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNull(a, b))
    }
    case class  SetObject(a: Int, b: Object, c: Int) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a, b, c))
    }
    case class  SetObject1(a: Int, b: Object) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a, b))
    }
    case class  SetObject2(a: Int, b: Object, c: Int, d: Int) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a, b, c, d))
    }
    case class  SetPoolable(a: Boolean) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setPoolable(a))
    }
    case class  SetQueryTimeout(a: Int) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setQueryTimeout(a))
    }
    case class  SetRef(a: Int, b: Ref) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setRef(a, b))
    }
    case class  SetRowId(a: Int, b: RowId) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setRowId(a, b))
    }
    case class  SetSQLXML(a: Int, b: SQLXML) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setSQLXML(a, b))
    }
    case class  SetShort(a: Int, b: Short) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setShort(a, b))
    }
    case class  SetString(a: Int, b: String) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setString(a, b))
    }
    case class  SetTime(a: Int, b: Time) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTime(a, b))
    }
    case class  SetTime1(a: Int, b: Time, c: Calendar) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTime(a, b, c))
    }
    case class  SetTimestamp(a: Int, b: Timestamp) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTimestamp(a, b))
    }
    case class  SetTimestamp1(a: Int, b: Timestamp, c: Calendar) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTimestamp(a, b, c))
    }
    case class  SetURL(a: Int, b: URL) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setURL(a, b))
    }
    case class  SetUnicodeStream(a: Int, b: InputStream, c: Int) extends PreparedStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setUnicodeStream(a, b, c))
    }
    case class  Unwrap[T](a: Class[T]) extends PreparedStatementOp[T] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.unwrap(a))
    }

  }
  import PreparedStatementOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[PreparedStatementOp]]; abstractly, a computation that consumes 
   * a `java.sql.PreparedStatement` and produces a value of type `A`. 
   * @group Algebra 
   */
  type PreparedStatementIO[A] = F.FreeC[PreparedStatementOp, A]

  /**
   * Monad instance for [[PreparedStatementIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadPreparedStatementIO: Monad[PreparedStatementIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[PreparedStatementOp, α]})#λ]

  /**
   * Catchable instance for [[PreparedStatementIO]].
   * @group Typeclass Instances
   */
  implicit val CatchablePreparedStatementIO: Catchable[PreparedStatementIO] =
    new Catchable[PreparedStatementIO] {
      def attempt[A](f: PreparedStatementIO[A]): PreparedStatementIO[Throwable \/ A] = preparedstatement.attempt(f)
      def fail[A](err: Throwable): PreparedStatementIO[A] = preparedstatement.delay(throw err)
    }

  /**
   * Capture instance for [[PreparedStatementIO]].
   * @group Typeclass Instances
   */
  implicit val CapturePreparedStatementIO: Capture[PreparedStatementIO] =
    new Capture[PreparedStatementIO] {
      def apply[A](a: => A): PreparedStatementIO[A] = preparedstatement.delay(a)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftBlob[A](s: Blob, k: BlobIO[A]): PreparedStatementIO[A] =
    F.liftFC(LiftBlobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftCallableStatement[A](s: CallableStatement, k: CallableStatementIO[A]): PreparedStatementIO[A] =
    F.liftFC(LiftCallableStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftClob[A](s: Clob, k: ClobIO[A]): PreparedStatementIO[A] =
    F.liftFC(LiftClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftConnection[A](s: Connection, k: ConnectionIO[A]): PreparedStatementIO[A] =
    F.liftFC(LiftConnectionIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDatabaseMetaData[A](s: DatabaseMetaData, k: DatabaseMetaDataIO[A]): PreparedStatementIO[A] =
    F.liftFC(LiftDatabaseMetaDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDriver[A](s: Driver, k: DriverIO[A]): PreparedStatementIO[A] =
    F.liftFC(LiftDriverIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftNClob[A](s: NClob, k: NClobIO[A]): PreparedStatementIO[A] =
    F.liftFC(LiftNClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftRef[A](s: Ref, k: RefIO[A]): PreparedStatementIO[A] =
    F.liftFC(LiftRefIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftResultSet[A](s: ResultSet, k: ResultSetIO[A]): PreparedStatementIO[A] =
    F.liftFC(LiftResultSetIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLData[A](s: SQLData, k: SQLDataIO[A]): PreparedStatementIO[A] =
    F.liftFC(LiftSQLDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLInput[A](s: SQLInput, k: SQLInputIO[A]): PreparedStatementIO[A] =
    F.liftFC(LiftSQLInputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLOutput[A](s: SQLOutput, k: SQLOutputIO[A]): PreparedStatementIO[A] =
    F.liftFC(LiftSQLOutputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftStatement[A](s: Statement, k: StatementIO[A]): PreparedStatementIO[A] =
    F.liftFC(LiftStatementIO(s, k))

  /** 
   * Lift a PreparedStatementIO[A] into an exception-capturing PreparedStatementIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: PreparedStatementIO[A]): PreparedStatementIO[Throwable \/ A] =
    F.liftFC[PreparedStatementOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): PreparedStatementIO[A] =
    F.liftFC(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying PreparedStatement.
   * @group Constructors (Lifting)
   */
  def raw[A](f: PreparedStatement => A): PreparedStatementIO[A] =
    F.liftFC(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  val addBatch: PreparedStatementIO[Unit] =
    F.liftFC(AddBatch)

  /** 
   * @group Constructors (Primitives)
   */
  def addBatch(a: String): PreparedStatementIO[Unit] =
    F.liftFC(AddBatch1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val cancel: PreparedStatementIO[Unit] =
    F.liftFC(Cancel)

  /** 
   * @group Constructors (Primitives)
   */
  val clearBatch: PreparedStatementIO[Unit] =
    F.liftFC(ClearBatch)

  /** 
   * @group Constructors (Primitives)
   */
  val clearParameters: PreparedStatementIO[Unit] =
    F.liftFC(ClearParameters)

  /** 
   * @group Constructors (Primitives)
   */
  val clearWarnings: PreparedStatementIO[Unit] =
    F.liftFC(ClearWarnings)

  /** 
   * @group Constructors (Primitives)
   */
  val close: PreparedStatementIO[Unit] =
    F.liftFC(Close)

  /** 
   * @group Constructors (Primitives)
   */
  val closeOnCompletion: PreparedStatementIO[Unit] =
    F.liftFC(CloseOnCompletion)

  /** 
   * @group Constructors (Primitives)
   */
  val execute: PreparedStatementIO[Boolean] =
    F.liftFC(Execute)

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Int): PreparedStatementIO[Boolean] =
    F.liftFC(Execute1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Array[Int]): PreparedStatementIO[Boolean] =
    F.liftFC(Execute2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String): PreparedStatementIO[Boolean] =
    F.liftFC(Execute3(a))

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Array[String]): PreparedStatementIO[Boolean] =
    F.liftFC(Execute4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val executeBatch: PreparedStatementIO[Array[Int]] =
    F.liftFC(ExecuteBatch)

  /** 
   * @group Constructors (Primitives)
   */
  val executeQuery: PreparedStatementIO[ResultSet] =
    F.liftFC(ExecuteQuery)

  /** 
   * @group Constructors (Primitives)
   */
  def executeQuery(a: String): PreparedStatementIO[ResultSet] =
    F.liftFC(ExecuteQuery1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val executeUpdate: PreparedStatementIO[Int] =
    F.liftFC(ExecuteUpdate)

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Int): PreparedStatementIO[Int] =
    F.liftFC(ExecuteUpdate1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Array[Int]): PreparedStatementIO[Int] =
    F.liftFC(ExecuteUpdate2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Array[String]): PreparedStatementIO[Int] =
    F.liftFC(ExecuteUpdate3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String): PreparedStatementIO[Int] =
    F.liftFC(ExecuteUpdate4(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getConnection: PreparedStatementIO[Connection] =
    F.liftFC(GetConnection)

  /** 
   * @group Constructors (Primitives)
   */
  val getFetchDirection: PreparedStatementIO[Int] =
    F.liftFC(GetFetchDirection)

  /** 
   * @group Constructors (Primitives)
   */
  val getFetchSize: PreparedStatementIO[Int] =
    F.liftFC(GetFetchSize)

  /** 
   * @group Constructors (Primitives)
   */
  val getGeneratedKeys: PreparedStatementIO[ResultSet] =
    F.liftFC(GetGeneratedKeys)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxFieldSize: PreparedStatementIO[Int] =
    F.liftFC(GetMaxFieldSize)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxRows: PreparedStatementIO[Int] =
    F.liftFC(GetMaxRows)

  /** 
   * @group Constructors (Primitives)
   */
  val getMetaData: PreparedStatementIO[ResultSetMetaData] =
    F.liftFC(GetMetaData)

  /** 
   * @group Constructors (Primitives)
   */
  def getMoreResults(a: Int): PreparedStatementIO[Boolean] =
    F.liftFC(GetMoreResults(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getMoreResults: PreparedStatementIO[Boolean] =
    F.liftFC(GetMoreResults1)

  /** 
   * @group Constructors (Primitives)
   */
  val getParameterMetaData: PreparedStatementIO[ParameterMetaData] =
    F.liftFC(GetParameterMetaData)

  /** 
   * @group Constructors (Primitives)
   */
  val getQueryTimeout: PreparedStatementIO[Int] =
    F.liftFC(GetQueryTimeout)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSet: PreparedStatementIO[ResultSet] =
    F.liftFC(GetResultSet)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSetConcurrency: PreparedStatementIO[Int] =
    F.liftFC(GetResultSetConcurrency)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSetHoldability: PreparedStatementIO[Int] =
    F.liftFC(GetResultSetHoldability)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSetType: PreparedStatementIO[Int] =
    F.liftFC(GetResultSetType)

  /** 
   * @group Constructors (Primitives)
   */
  val getUpdateCount: PreparedStatementIO[Int] =
    F.liftFC(GetUpdateCount)

  /** 
   * @group Constructors (Primitives)
   */
  val getWarnings: PreparedStatementIO[SQLWarning] =
    F.liftFC(GetWarnings)

  /** 
   * @group Constructors (Primitives)
   */
  val isCloseOnCompletion: PreparedStatementIO[Boolean] =
    F.liftFC(IsCloseOnCompletion)

  /** 
   * @group Constructors (Primitives)
   */
  val isClosed: PreparedStatementIO[Boolean] =
    F.liftFC(IsClosed)

  /** 
   * @group Constructors (Primitives)
   */
  val isPoolable: PreparedStatementIO[Boolean] =
    F.liftFC(IsPoolable)

  /** 
   * @group Constructors (Primitives)
   */
  def isWrapperFor(a: Class[_]): PreparedStatementIO[Boolean] =
    F.liftFC(IsWrapperFor(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setArray(a: Int, b: SqlArray): PreparedStatementIO[Unit] =
    F.liftFC(SetArray(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: Int, b: InputStream, c: Long): PreparedStatementIO[Unit] =
    F.liftFC(SetAsciiStream(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: Int, b: InputStream, c: Int): PreparedStatementIO[Unit] =
    F.liftFC(SetAsciiStream1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: Int, b: InputStream): PreparedStatementIO[Unit] =
    F.liftFC(SetAsciiStream2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBigDecimal(a: Int, b: BigDecimal): PreparedStatementIO[Unit] =
    F.liftFC(SetBigDecimal(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: Int, b: InputStream): PreparedStatementIO[Unit] =
    F.liftFC(SetBinaryStream(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: Int, b: InputStream, c: Int): PreparedStatementIO[Unit] =
    F.liftFC(SetBinaryStream1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: Int, b: InputStream, c: Long): PreparedStatementIO[Unit] =
    F.liftFC(SetBinaryStream2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setBlob(a: Int, b: InputStream, c: Long): PreparedStatementIO[Unit] =
    F.liftFC(SetBlob(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setBlob(a: Int, b: Blob): PreparedStatementIO[Unit] =
    F.liftFC(SetBlob1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBlob(a: Int, b: InputStream): PreparedStatementIO[Unit] =
    F.liftFC(SetBlob2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBoolean(a: Int, b: Boolean): PreparedStatementIO[Unit] =
    F.liftFC(SetBoolean(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setByte(a: Int, b: Byte): PreparedStatementIO[Unit] =
    F.liftFC(SetByte(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBytes(a: Int, b: Array[Byte]): PreparedStatementIO[Unit] =
    F.liftFC(SetBytes(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: Int, b: Reader, c: Long): PreparedStatementIO[Unit] =
    F.liftFC(SetCharacterStream(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: Int, b: Reader, c: Int): PreparedStatementIO[Unit] =
    F.liftFC(SetCharacterStream1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: Int, b: Reader): PreparedStatementIO[Unit] =
    F.liftFC(SetCharacterStream2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setClob(a: Int, b: Reader, c: Long): PreparedStatementIO[Unit] =
    F.liftFC(SetClob(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setClob(a: Int, b: Reader): PreparedStatementIO[Unit] =
    F.liftFC(SetClob1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setClob(a: Int, b: Clob): PreparedStatementIO[Unit] =
    F.liftFC(SetClob2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setCursorName(a: String): PreparedStatementIO[Unit] =
    F.liftFC(SetCursorName(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setDate(a: Int, b: Date, c: Calendar): PreparedStatementIO[Unit] =
    F.liftFC(SetDate(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setDate(a: Int, b: Date): PreparedStatementIO[Unit] =
    F.liftFC(SetDate1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setDouble(a: Int, b: Double): PreparedStatementIO[Unit] =
    F.liftFC(SetDouble(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setEscapeProcessing(a: Boolean): PreparedStatementIO[Unit] =
    F.liftFC(SetEscapeProcessing(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setFetchDirection(a: Int): PreparedStatementIO[Unit] =
    F.liftFC(SetFetchDirection(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setFetchSize(a: Int): PreparedStatementIO[Unit] =
    F.liftFC(SetFetchSize(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setFloat(a: Int, b: Float): PreparedStatementIO[Unit] =
    F.liftFC(SetFloat(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setInt(a: Int, b: Int): PreparedStatementIO[Unit] =
    F.liftFC(SetInt(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setLong(a: Int, b: Long): PreparedStatementIO[Unit] =
    F.liftFC(SetLong(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setMaxFieldSize(a: Int): PreparedStatementIO[Unit] =
    F.liftFC(SetMaxFieldSize(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setMaxRows(a: Int): PreparedStatementIO[Unit] =
    F.liftFC(SetMaxRows(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setNCharacterStream(a: Int, b: Reader): PreparedStatementIO[Unit] =
    F.liftFC(SetNCharacterStream(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setNCharacterStream(a: Int, b: Reader, c: Long): PreparedStatementIO[Unit] =
    F.liftFC(SetNCharacterStream1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setNClob(a: Int, b: Reader): PreparedStatementIO[Unit] =
    F.liftFC(SetNClob(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setNClob(a: Int, b: Reader, c: Long): PreparedStatementIO[Unit] =
    F.liftFC(SetNClob1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setNClob(a: Int, b: NClob): PreparedStatementIO[Unit] =
    F.liftFC(SetNClob2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setNString(a: Int, b: String): PreparedStatementIO[Unit] =
    F.liftFC(SetNString(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setNull(a: Int, b: Int, c: String): PreparedStatementIO[Unit] =
    F.liftFC(SetNull(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setNull(a: Int, b: Int): PreparedStatementIO[Unit] =
    F.liftFC(SetNull1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setObject(a: Int, b: Object, c: Int): PreparedStatementIO[Unit] =
    F.liftFC(SetObject(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setObject(a: Int, b: Object): PreparedStatementIO[Unit] =
    F.liftFC(SetObject1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setObject(a: Int, b: Object, c: Int, d: Int): PreparedStatementIO[Unit] =
    F.liftFC(SetObject2(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def setPoolable(a: Boolean): PreparedStatementIO[Unit] =
    F.liftFC(SetPoolable(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setQueryTimeout(a: Int): PreparedStatementIO[Unit] =
    F.liftFC(SetQueryTimeout(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setRef(a: Int, b: Ref): PreparedStatementIO[Unit] =
    F.liftFC(SetRef(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setRowId(a: Int, b: RowId): PreparedStatementIO[Unit] =
    F.liftFC(SetRowId(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setSQLXML(a: Int, b: SQLXML): PreparedStatementIO[Unit] =
    F.liftFC(SetSQLXML(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setShort(a: Int, b: Short): PreparedStatementIO[Unit] =
    F.liftFC(SetShort(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setString(a: Int, b: String): PreparedStatementIO[Unit] =
    F.liftFC(SetString(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setTime(a: Int, b: Time): PreparedStatementIO[Unit] =
    F.liftFC(SetTime(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setTime(a: Int, b: Time, c: Calendar): PreparedStatementIO[Unit] =
    F.liftFC(SetTime1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setTimestamp(a: Int, b: Timestamp): PreparedStatementIO[Unit] =
    F.liftFC(SetTimestamp(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setTimestamp(a: Int, b: Timestamp, c: Calendar): PreparedStatementIO[Unit] =
    F.liftFC(SetTimestamp1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setURL(a: Int, b: URL): PreparedStatementIO[Unit] =
    F.liftFC(SetURL(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setUnicodeStream(a: Int, b: InputStream, c: Int): PreparedStatementIO[Unit] =
    F.liftFC(SetUnicodeStream(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def unwrap[T](a: Class[T]): PreparedStatementIO[T] =
    F.liftFC(Unwrap(a))

 /** 
  * Natural transformation from `PreparedStatementOp` to `Kleisli` for the given `M`, consuming a `java.sql.PreparedStatement`. 
  * @group Algebra
  */
  def kleisliTrans[M[_]: Monad: Catchable: Capture]: PreparedStatementOp ~> Kleisli[M, PreparedStatement, ?] =
    new (PreparedStatementOp ~> Kleisli[M, PreparedStatement, ?]) {
      def apply[A](op: PreparedStatementOp[A]): Kleisli[M, PreparedStatement, A] =
        op.defaultTransK[M]
    }

  /**
   * Syntax for `PreparedStatementIO`.
   * @group Algebra
   */
  implicit class PreparedStatementIOOps[A](ma: PreparedStatementIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, PreparedStatement, A] =
      F.runFC[PreparedStatementOp, Kleisli[M, PreparedStatement, ?], A](ma)(kleisliTrans[M])
  }

}

