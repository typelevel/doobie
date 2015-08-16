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
import java.util.Map

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
 * Algebra and free monad for primitive operations over a `java.sql.CallableStatement`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `CallableStatementIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `CallableStatementOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, CallableStatement, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: CallableStatementIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: CallableStatement = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object callablestatement {
  
  /** 
   * Sum type of primitive operations over a `java.sql.CallableStatement`.
   * @group Algebra 
   */
  sealed trait CallableStatementOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: CallableStatement => A): Kleisli[M, CallableStatement, A] = 
      Kleisli((s: CallableStatement) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, CallableStatement, A]
  }

  /** 
   * Module of constructors for `CallableStatementOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `callablestatement` module.
   * @group Algebra 
   */
  object CallableStatementOp {
    
    // Lifting
    case class LiftBlobIO[A](s: Blob, action: BlobIO[A]) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftClobIO[A](s: Clob, action: ClobIO[A]) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftConnectionIO[A](s: Connection, action: ConnectionIO[A]) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDatabaseMetaDataIO[A](s: DatabaseMetaData, action: DatabaseMetaDataIO[A]) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDriverIO[A](s: Driver, action: DriverIO[A]) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftNClobIO[A](s: NClob, action: NClobIO[A]) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftPreparedStatementIO[A](s: PreparedStatement, action: PreparedStatementIO[A]) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftRefIO[A](s: Ref, action: RefIO[A]) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftResultSetIO[A](s: ResultSet, action: ResultSetIO[A]) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLDataIO[A](s: SQLData, action: SQLDataIO[A]) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLInputIO[A](s: SQLInput, action: SQLInputIO[A]) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLOutputIO[A](s: SQLOutput, action: SQLOutputIO[A]) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftStatementIO[A](s: Statement, action: StatementIO[A]) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }

    // Combinators
    case class Attempt[A](action: CallableStatementIO[A]) extends CallableStatementOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, CallableStatement, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: CallableStatement => A) extends CallableStatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case object AddBatch extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.addBatch())
    }
    case class  AddBatch1(a: String) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.addBatch(a))
    }
    case object Cancel extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.cancel())
    }
    case object ClearBatch extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.clearBatch())
    }
    case object ClearParameters extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.clearParameters())
    }
    case object ClearWarnings extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.clearWarnings())
    }
    case object Close extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.close())
    }
    case object CloseOnCompletion extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.closeOnCompletion())
    }
    case object Execute extends CallableStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute())
    }
    case class  Execute1(a: String, b: Int) extends CallableStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a, b))
    }
    case class  Execute2(a: String, b: Array[Int]) extends CallableStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a, b))
    }
    case class  Execute3(a: String) extends CallableStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a))
    }
    case class  Execute4(a: String, b: Array[String]) extends CallableStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a, b))
    }
    case object ExecuteBatch extends CallableStatementOp[Array[Int]] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeBatch())
    }
    case object ExecuteQuery extends CallableStatementOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeQuery())
    }
    case class  ExecuteQuery1(a: String) extends CallableStatementOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeQuery(a))
    }
    case object ExecuteUpdate extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate())
    }
    case class  ExecuteUpdate1(a: String, b: Int) extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate2(a: String, b: Array[Int]) extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate3(a: String, b: Array[String]) extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate4(a: String) extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a))
    }
    case class  GetArray(a: String) extends CallableStatementOp[SqlArray] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getArray(a))
    }
    case class  GetArray1(a: Int) extends CallableStatementOp[SqlArray] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getArray(a))
    }
    case class  GetBigDecimal(a: Int) extends CallableStatementOp[BigDecimal] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBigDecimal(a))
    }
    case class  GetBigDecimal1(a: String) extends CallableStatementOp[BigDecimal] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBigDecimal(a))
    }
    case class  GetBigDecimal2(a: Int, b: Int) extends CallableStatementOp[BigDecimal] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBigDecimal(a, b))
    }
    case class  GetBlob(a: String) extends CallableStatementOp[Blob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBlob(a))
    }
    case class  GetBlob1(a: Int) extends CallableStatementOp[Blob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBlob(a))
    }
    case class  GetBoolean(a: String) extends CallableStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBoolean(a))
    }
    case class  GetBoolean1(a: Int) extends CallableStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBoolean(a))
    }
    case class  GetByte(a: Int) extends CallableStatementOp[Byte] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getByte(a))
    }
    case class  GetByte1(a: String) extends CallableStatementOp[Byte] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getByte(a))
    }
    case class  GetBytes(a: Int) extends CallableStatementOp[Array[Byte]] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBytes(a))
    }
    case class  GetBytes1(a: String) extends CallableStatementOp[Array[Byte]] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBytes(a))
    }
    case class  GetCharacterStream(a: Int) extends CallableStatementOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCharacterStream(a))
    }
    case class  GetCharacterStream1(a: String) extends CallableStatementOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCharacterStream(a))
    }
    case class  GetClob(a: Int) extends CallableStatementOp[Clob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getClob(a))
    }
    case class  GetClob1(a: String) extends CallableStatementOp[Clob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getClob(a))
    }
    case object GetConnection extends CallableStatementOp[Connection] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getConnection())
    }
    case class  GetDate(a: Int) extends CallableStatementOp[Date] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDate(a))
    }
    case class  GetDate1(a: Int, b: Calendar) extends CallableStatementOp[Date] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDate(a, b))
    }
    case class  GetDate2(a: String) extends CallableStatementOp[Date] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDate(a))
    }
    case class  GetDate3(a: String, b: Calendar) extends CallableStatementOp[Date] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDate(a, b))
    }
    case class  GetDouble(a: String) extends CallableStatementOp[Double] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDouble(a))
    }
    case class  GetDouble1(a: Int) extends CallableStatementOp[Double] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDouble(a))
    }
    case object GetFetchDirection extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFetchDirection())
    }
    case object GetFetchSize extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFetchSize())
    }
    case class  GetFloat(a: Int) extends CallableStatementOp[Float] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFloat(a))
    }
    case class  GetFloat1(a: String) extends CallableStatementOp[Float] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFloat(a))
    }
    case object GetGeneratedKeys extends CallableStatementOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getGeneratedKeys())
    }
    case class  GetInt(a: Int) extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getInt(a))
    }
    case class  GetInt1(a: String) extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getInt(a))
    }
    case class  GetLong(a: String) extends CallableStatementOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getLong(a))
    }
    case class  GetLong1(a: Int) extends CallableStatementOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getLong(a))
    }
    case object GetMaxFieldSize extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxFieldSize())
    }
    case object GetMaxRows extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxRows())
    }
    case object GetMetaData extends CallableStatementOp[ResultSetMetaData] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMetaData())
    }
    case class  GetMoreResults(a: Int) extends CallableStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMoreResults(a))
    }
    case object GetMoreResults1 extends CallableStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMoreResults())
    }
    case class  GetNCharacterStream(a: String) extends CallableStatementOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getNCharacterStream(a))
    }
    case class  GetNCharacterStream1(a: Int) extends CallableStatementOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getNCharacterStream(a))
    }
    case class  GetNClob(a: String) extends CallableStatementOp[NClob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getNClob(a))
    }
    case class  GetNClob1(a: Int) extends CallableStatementOp[NClob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getNClob(a))
    }
    case class  GetNString(a: Int) extends CallableStatementOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getNString(a))
    }
    case class  GetNString1(a: String) extends CallableStatementOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getNString(a))
    }
    case class  GetObject(a: String) extends CallableStatementOp[Object] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a))
    }
    case class  GetObject1(a: Int, b: Map[String, Class[_]]) extends CallableStatementOp[Object] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a, b))
    }
    case class  GetObject2(a: Int) extends CallableStatementOp[Object] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a))
    }
    case class  GetObject3(a: String, b: Map[String, Class[_]]) extends CallableStatementOp[Object] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a, b))
    }
    case class  GetObject4[T](a: String, b: Class[T]) extends CallableStatementOp[T] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a, b))
    }
    case class  GetObject5[T](a: Int, b: Class[T]) extends CallableStatementOp[T] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a, b))
    }
    case object GetParameterMetaData extends CallableStatementOp[ParameterMetaData] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getParameterMetaData())
    }
    case object GetQueryTimeout extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getQueryTimeout())
    }
    case class  GetRef(a: String) extends CallableStatementOp[Ref] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getRef(a))
    }
    case class  GetRef1(a: Int) extends CallableStatementOp[Ref] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getRef(a))
    }
    case object GetResultSet extends CallableStatementOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSet())
    }
    case object GetResultSetConcurrency extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSetConcurrency())
    }
    case object GetResultSetHoldability extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSetHoldability())
    }
    case object GetResultSetType extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSetType())
    }
    case class  GetRowId(a: Int) extends CallableStatementOp[RowId] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getRowId(a))
    }
    case class  GetRowId1(a: String) extends CallableStatementOp[RowId] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getRowId(a))
    }
    case class  GetSQLXML(a: Int) extends CallableStatementOp[SQLXML] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSQLXML(a))
    }
    case class  GetSQLXML1(a: String) extends CallableStatementOp[SQLXML] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSQLXML(a))
    }
    case class  GetShort(a: String) extends CallableStatementOp[Short] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getShort(a))
    }
    case class  GetShort1(a: Int) extends CallableStatementOp[Short] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getShort(a))
    }
    case class  GetString(a: String) extends CallableStatementOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getString(a))
    }
    case class  GetString1(a: Int) extends CallableStatementOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getString(a))
    }
    case class  GetTime(a: Int) extends CallableStatementOp[Time] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTime(a))
    }
    case class  GetTime1(a: String, b: Calendar) extends CallableStatementOp[Time] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTime(a, b))
    }
    case class  GetTime2(a: Int, b: Calendar) extends CallableStatementOp[Time] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTime(a, b))
    }
    case class  GetTime3(a: String) extends CallableStatementOp[Time] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTime(a))
    }
    case class  GetTimestamp(a: Int, b: Calendar) extends CallableStatementOp[Timestamp] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTimestamp(a, b))
    }
    case class  GetTimestamp1(a: String) extends CallableStatementOp[Timestamp] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTimestamp(a))
    }
    case class  GetTimestamp2(a: String, b: Calendar) extends CallableStatementOp[Timestamp] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTimestamp(a, b))
    }
    case class  GetTimestamp3(a: Int) extends CallableStatementOp[Timestamp] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTimestamp(a))
    }
    case class  GetURL(a: String) extends CallableStatementOp[URL] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getURL(a))
    }
    case class  GetURL1(a: Int) extends CallableStatementOp[URL] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getURL(a))
    }
    case object GetUpdateCount extends CallableStatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getUpdateCount())
    }
    case object GetWarnings extends CallableStatementOp[SQLWarning] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getWarnings())
    }
    case object IsCloseOnCompletion extends CallableStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isCloseOnCompletion())
    }
    case object IsClosed extends CallableStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isClosed())
    }
    case object IsPoolable extends CallableStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isPoolable())
    }
    case class  IsWrapperFor(a: Class[_]) extends CallableStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isWrapperFor(a))
    }
    case class  RegisterOutParameter(a: String, b: Int, c: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.registerOutParameter(a, b, c))
    }
    case class  RegisterOutParameter1(a: String, b: Int, c: String) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.registerOutParameter(a, b, c))
    }
    case class  RegisterOutParameter2(a: Int, b: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.registerOutParameter(a, b))
    }
    case class  RegisterOutParameter3(a: Int, b: Int, c: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.registerOutParameter(a, b, c))
    }
    case class  RegisterOutParameter4(a: String, b: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.registerOutParameter(a, b))
    }
    case class  RegisterOutParameter5(a: Int, b: Int, c: String) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.registerOutParameter(a, b, c))
    }
    case class  SetArray(a: Int, b: SqlArray) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setArray(a, b))
    }
    case class  SetAsciiStream(a: String, b: InputStream, c: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a, b, c))
    }
    case class  SetAsciiStream1(a: String, b: InputStream, c: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a, b, c))
    }
    case class  SetAsciiStream2(a: String, b: InputStream) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a, b))
    }
    case class  SetAsciiStream3(a: Int, b: InputStream, c: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a, b, c))
    }
    case class  SetAsciiStream4(a: Int, b: InputStream, c: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a, b, c))
    }
    case class  SetAsciiStream5(a: Int, b: InputStream) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a, b))
    }
    case class  SetBigDecimal(a: String, b: BigDecimal) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBigDecimal(a, b))
    }
    case class  SetBigDecimal1(a: Int, b: BigDecimal) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBigDecimal(a, b))
    }
    case class  SetBinaryStream(a: String, b: InputStream) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBinaryStream(a, b))
    }
    case class  SetBinaryStream1(a: String, b: InputStream, c: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBinaryStream(a, b, c))
    }
    case class  SetBinaryStream2(a: String, b: InputStream, c: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBinaryStream(a, b, c))
    }
    case class  SetBinaryStream3(a: Int, b: InputStream) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBinaryStream(a, b))
    }
    case class  SetBinaryStream4(a: Int, b: InputStream, c: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBinaryStream(a, b, c))
    }
    case class  SetBinaryStream5(a: Int, b: InputStream, c: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBinaryStream(a, b, c))
    }
    case class  SetBlob(a: String, b: InputStream, c: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBlob(a, b, c))
    }
    case class  SetBlob1(a: String, b: InputStream) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBlob(a, b))
    }
    case class  SetBlob2(a: String, b: Blob) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBlob(a, b))
    }
    case class  SetBlob3(a: Int, b: InputStream, c: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBlob(a, b, c))
    }
    case class  SetBlob4(a: Int, b: Blob) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBlob(a, b))
    }
    case class  SetBlob5(a: Int, b: InputStream) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBlob(a, b))
    }
    case class  SetBoolean(a: String, b: Boolean) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBoolean(a, b))
    }
    case class  SetBoolean1(a: Int, b: Boolean) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBoolean(a, b))
    }
    case class  SetByte(a: String, b: Byte) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setByte(a, b))
    }
    case class  SetByte1(a: Int, b: Byte) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setByte(a, b))
    }
    case class  SetBytes(a: String, b: Array[Byte]) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBytes(a, b))
    }
    case class  SetBytes1(a: Int, b: Array[Byte]) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBytes(a, b))
    }
    case class  SetCharacterStream(a: String, b: Reader, c: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a, b, c))
    }
    case class  SetCharacterStream1(a: String, b: Reader, c: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a, b, c))
    }
    case class  SetCharacterStream2(a: String, b: Reader) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a, b))
    }
    case class  SetCharacterStream3(a: Int, b: Reader, c: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a, b, c))
    }
    case class  SetCharacterStream4(a: Int, b: Reader, c: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a, b, c))
    }
    case class  SetCharacterStream5(a: Int, b: Reader) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a, b))
    }
    case class  SetClob(a: String, b: Clob) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setClob(a, b))
    }
    case class  SetClob1(a: String, b: Reader, c: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setClob(a, b, c))
    }
    case class  SetClob2(a: String, b: Reader) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setClob(a, b))
    }
    case class  SetClob3(a: Int, b: Reader, c: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setClob(a, b, c))
    }
    case class  SetClob4(a: Int, b: Reader) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setClob(a, b))
    }
    case class  SetClob5(a: Int, b: Clob) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setClob(a, b))
    }
    case class  SetCursorName(a: String) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCursorName(a))
    }
    case class  SetDate(a: String, b: Date) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setDate(a, b))
    }
    case class  SetDate1(a: String, b: Date, c: Calendar) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setDate(a, b, c))
    }
    case class  SetDate2(a: Int, b: Date, c: Calendar) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setDate(a, b, c))
    }
    case class  SetDate3(a: Int, b: Date) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setDate(a, b))
    }
    case class  SetDouble(a: String, b: Double) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setDouble(a, b))
    }
    case class  SetDouble1(a: Int, b: Double) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setDouble(a, b))
    }
    case class  SetEscapeProcessing(a: Boolean) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setEscapeProcessing(a))
    }
    case class  SetFetchDirection(a: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setFetchDirection(a))
    }
    case class  SetFetchSize(a: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setFetchSize(a))
    }
    case class  SetFloat(a: String, b: Float) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setFloat(a, b))
    }
    case class  SetFloat1(a: Int, b: Float) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setFloat(a, b))
    }
    case class  SetInt(a: String, b: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setInt(a, b))
    }
    case class  SetInt1(a: Int, b: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setInt(a, b))
    }
    case class  SetLong(a: String, b: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setLong(a, b))
    }
    case class  SetLong1(a: Int, b: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setLong(a, b))
    }
    case class  SetMaxFieldSize(a: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setMaxFieldSize(a))
    }
    case class  SetMaxRows(a: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setMaxRows(a))
    }
    case class  SetNCharacterStream(a: String, b: Reader, c: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNCharacterStream(a, b, c))
    }
    case class  SetNCharacterStream1(a: String, b: Reader) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNCharacterStream(a, b))
    }
    case class  SetNCharacterStream2(a: Int, b: Reader) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNCharacterStream(a, b))
    }
    case class  SetNCharacterStream3(a: Int, b: Reader, c: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNCharacterStream(a, b, c))
    }
    case class  SetNClob(a: String, b: NClob) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNClob(a, b))
    }
    case class  SetNClob1(a: String, b: Reader, c: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNClob(a, b, c))
    }
    case class  SetNClob2(a: String, b: Reader) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNClob(a, b))
    }
    case class  SetNClob3(a: Int, b: Reader) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNClob(a, b))
    }
    case class  SetNClob4(a: Int, b: Reader, c: Long) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNClob(a, b, c))
    }
    case class  SetNClob5(a: Int, b: NClob) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNClob(a, b))
    }
    case class  SetNString(a: String, b: String) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNString(a, b))
    }
    case class  SetNString1(a: Int, b: String) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNString(a, b))
    }
    case class  SetNull(a: String, b: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNull(a, b))
    }
    case class  SetNull1(a: String, b: Int, c: String) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNull(a, b, c))
    }
    case class  SetNull2(a: Int, b: Int, c: String) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNull(a, b, c))
    }
    case class  SetNull3(a: Int, b: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNull(a, b))
    }
    case class  SetObject(a: String, b: Object) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a, b))
    }
    case class  SetObject1(a: String, b: Object, c: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a, b, c))
    }
    case class  SetObject2(a: String, b: Object, c: Int, d: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a, b, c, d))
    }
    case class  SetObject3(a: Int, b: Object, c: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a, b, c))
    }
    case class  SetObject4(a: Int, b: Object) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a, b))
    }
    case class  SetObject5(a: Int, b: Object, c: Int, d: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a, b, c, d))
    }
    case class  SetPoolable(a: Boolean) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setPoolable(a))
    }
    case class  SetQueryTimeout(a: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setQueryTimeout(a))
    }
    case class  SetRef(a: Int, b: Ref) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setRef(a, b))
    }
    case class  SetRowId(a: String, b: RowId) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setRowId(a, b))
    }
    case class  SetRowId1(a: Int, b: RowId) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setRowId(a, b))
    }
    case class  SetSQLXML(a: String, b: SQLXML) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setSQLXML(a, b))
    }
    case class  SetSQLXML1(a: Int, b: SQLXML) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setSQLXML(a, b))
    }
    case class  SetShort(a: String, b: Short) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setShort(a, b))
    }
    case class  SetShort1(a: Int, b: Short) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setShort(a, b))
    }
    case class  SetString(a: String, b: String) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setString(a, b))
    }
    case class  SetString1(a: Int, b: String) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setString(a, b))
    }
    case class  SetTime(a: String, b: Time, c: Calendar) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTime(a, b, c))
    }
    case class  SetTime1(a: String, b: Time) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTime(a, b))
    }
    case class  SetTime2(a: Int, b: Time) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTime(a, b))
    }
    case class  SetTime3(a: Int, b: Time, c: Calendar) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTime(a, b, c))
    }
    case class  SetTimestamp(a: String, b: Timestamp) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTimestamp(a, b))
    }
    case class  SetTimestamp1(a: String, b: Timestamp, c: Calendar) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTimestamp(a, b, c))
    }
    case class  SetTimestamp2(a: Int, b: Timestamp) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTimestamp(a, b))
    }
    case class  SetTimestamp3(a: Int, b: Timestamp, c: Calendar) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTimestamp(a, b, c))
    }
    case class  SetURL(a: String, b: URL) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setURL(a, b))
    }
    case class  SetURL1(a: Int, b: URL) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setURL(a, b))
    }
    case class  SetUnicodeStream(a: Int, b: InputStream, c: Int) extends CallableStatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setUnicodeStream(a, b, c))
    }
    case class  Unwrap[T](a: Class[T]) extends CallableStatementOp[T] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.unwrap(a))
    }
    case object WasNull extends CallableStatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.wasNull())
    }

  }
  import CallableStatementOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[CallableStatementOp]]; abstractly, a computation that consumes 
   * a `java.sql.CallableStatement` and produces a value of type `A`. 
   * @group Algebra 
   */
  type CallableStatementIO[A] = F.FreeC[CallableStatementOp, A]

  /**
   * Monad instance for [[CallableStatementIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadCallableStatementIO: Monad[CallableStatementIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[CallableStatementOp, α]})#λ]

  /**
   * Catchable instance for [[CallableStatementIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableCallableStatementIO: Catchable[CallableStatementIO] =
    new Catchable[CallableStatementIO] {
      def attempt[A](f: CallableStatementIO[A]): CallableStatementIO[Throwable \/ A] = callablestatement.attempt(f)
      def fail[A](err: Throwable): CallableStatementIO[A] = callablestatement.delay(throw err)
    }

  /**
   * Capture instance for [[CallableStatementIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureCallableStatementIO: Capture[CallableStatementIO] =
    new Capture[CallableStatementIO] {
      def apply[A](a: => A): CallableStatementIO[A] = callablestatement.delay(a)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftBlob[A](s: Blob, k: BlobIO[A]): CallableStatementIO[A] =
    F.liftFC(LiftBlobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftClob[A](s: Clob, k: ClobIO[A]): CallableStatementIO[A] =
    F.liftFC(LiftClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftConnection[A](s: Connection, k: ConnectionIO[A]): CallableStatementIO[A] =
    F.liftFC(LiftConnectionIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDatabaseMetaData[A](s: DatabaseMetaData, k: DatabaseMetaDataIO[A]): CallableStatementIO[A] =
    F.liftFC(LiftDatabaseMetaDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDriver[A](s: Driver, k: DriverIO[A]): CallableStatementIO[A] =
    F.liftFC(LiftDriverIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftNClob[A](s: NClob, k: NClobIO[A]): CallableStatementIO[A] =
    F.liftFC(LiftNClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftPreparedStatement[A](s: PreparedStatement, k: PreparedStatementIO[A]): CallableStatementIO[A] =
    F.liftFC(LiftPreparedStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftRef[A](s: Ref, k: RefIO[A]): CallableStatementIO[A] =
    F.liftFC(LiftRefIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftResultSet[A](s: ResultSet, k: ResultSetIO[A]): CallableStatementIO[A] =
    F.liftFC(LiftResultSetIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLData[A](s: SQLData, k: SQLDataIO[A]): CallableStatementIO[A] =
    F.liftFC(LiftSQLDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLInput[A](s: SQLInput, k: SQLInputIO[A]): CallableStatementIO[A] =
    F.liftFC(LiftSQLInputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLOutput[A](s: SQLOutput, k: SQLOutputIO[A]): CallableStatementIO[A] =
    F.liftFC(LiftSQLOutputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftStatement[A](s: Statement, k: StatementIO[A]): CallableStatementIO[A] =
    F.liftFC(LiftStatementIO(s, k))

  /** 
   * Lift a CallableStatementIO[A] into an exception-capturing CallableStatementIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: CallableStatementIO[A]): CallableStatementIO[Throwable \/ A] =
    F.liftFC[CallableStatementOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): CallableStatementIO[A] =
    F.liftFC(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying CallableStatement.
   * @group Constructors (Lifting)
   */
  def raw[A](f: CallableStatement => A): CallableStatementIO[A] =
    F.liftFC(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  val addBatch: CallableStatementIO[Unit] =
    F.liftFC(AddBatch)

  /** 
   * @group Constructors (Primitives)
   */
  def addBatch(a: String): CallableStatementIO[Unit] =
    F.liftFC(AddBatch1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val cancel: CallableStatementIO[Unit] =
    F.liftFC(Cancel)

  /** 
   * @group Constructors (Primitives)
   */
  val clearBatch: CallableStatementIO[Unit] =
    F.liftFC(ClearBatch)

  /** 
   * @group Constructors (Primitives)
   */
  val clearParameters: CallableStatementIO[Unit] =
    F.liftFC(ClearParameters)

  /** 
   * @group Constructors (Primitives)
   */
  val clearWarnings: CallableStatementIO[Unit] =
    F.liftFC(ClearWarnings)

  /** 
   * @group Constructors (Primitives)
   */
  val close: CallableStatementIO[Unit] =
    F.liftFC(Close)

  /** 
   * @group Constructors (Primitives)
   */
  val closeOnCompletion: CallableStatementIO[Unit] =
    F.liftFC(CloseOnCompletion)

  /** 
   * @group Constructors (Primitives)
   */
  val execute: CallableStatementIO[Boolean] =
    F.liftFC(Execute)

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Int): CallableStatementIO[Boolean] =
    F.liftFC(Execute1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Array[Int]): CallableStatementIO[Boolean] =
    F.liftFC(Execute2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String): CallableStatementIO[Boolean] =
    F.liftFC(Execute3(a))

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Array[String]): CallableStatementIO[Boolean] =
    F.liftFC(Execute4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val executeBatch: CallableStatementIO[Array[Int]] =
    F.liftFC(ExecuteBatch)

  /** 
   * @group Constructors (Primitives)
   */
  val executeQuery: CallableStatementIO[ResultSet] =
    F.liftFC(ExecuteQuery)

  /** 
   * @group Constructors (Primitives)
   */
  def executeQuery(a: String): CallableStatementIO[ResultSet] =
    F.liftFC(ExecuteQuery1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val executeUpdate: CallableStatementIO[Int] =
    F.liftFC(ExecuteUpdate)

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Int): CallableStatementIO[Int] =
    F.liftFC(ExecuteUpdate1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Array[Int]): CallableStatementIO[Int] =
    F.liftFC(ExecuteUpdate2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Array[String]): CallableStatementIO[Int] =
    F.liftFC(ExecuteUpdate3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String): CallableStatementIO[Int] =
    F.liftFC(ExecuteUpdate4(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getArray(a: String): CallableStatementIO[SqlArray] =
    F.liftFC(GetArray(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getArray(a: Int): CallableStatementIO[SqlArray] =
    F.liftFC(GetArray1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBigDecimal(a: Int): CallableStatementIO[BigDecimal] =
    F.liftFC(GetBigDecimal(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBigDecimal(a: String): CallableStatementIO[BigDecimal] =
    F.liftFC(GetBigDecimal1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBigDecimal(a: Int, b: Int): CallableStatementIO[BigDecimal] =
    F.liftFC(GetBigDecimal2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getBlob(a: String): CallableStatementIO[Blob] =
    F.liftFC(GetBlob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBlob(a: Int): CallableStatementIO[Blob] =
    F.liftFC(GetBlob1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBoolean(a: String): CallableStatementIO[Boolean] =
    F.liftFC(GetBoolean(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBoolean(a: Int): CallableStatementIO[Boolean] =
    F.liftFC(GetBoolean1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getByte(a: Int): CallableStatementIO[Byte] =
    F.liftFC(GetByte(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getByte(a: String): CallableStatementIO[Byte] =
    F.liftFC(GetByte1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBytes(a: Int): CallableStatementIO[Array[Byte]] =
    F.liftFC(GetBytes(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBytes(a: String): CallableStatementIO[Array[Byte]] =
    F.liftFC(GetBytes1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getCharacterStream(a: Int): CallableStatementIO[Reader] =
    F.liftFC(GetCharacterStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getCharacterStream(a: String): CallableStatementIO[Reader] =
    F.liftFC(GetCharacterStream1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getClob(a: Int): CallableStatementIO[Clob] =
    F.liftFC(GetClob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getClob(a: String): CallableStatementIO[Clob] =
    F.liftFC(GetClob1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getConnection: CallableStatementIO[Connection] =
    F.liftFC(GetConnection)

  /** 
   * @group Constructors (Primitives)
   */
  def getDate(a: Int): CallableStatementIO[Date] =
    F.liftFC(GetDate(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getDate(a: Int, b: Calendar): CallableStatementIO[Date] =
    F.liftFC(GetDate1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getDate(a: String): CallableStatementIO[Date] =
    F.liftFC(GetDate2(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getDate(a: String, b: Calendar): CallableStatementIO[Date] =
    F.liftFC(GetDate3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getDouble(a: String): CallableStatementIO[Double] =
    F.liftFC(GetDouble(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getDouble(a: Int): CallableStatementIO[Double] =
    F.liftFC(GetDouble1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getFetchDirection: CallableStatementIO[Int] =
    F.liftFC(GetFetchDirection)

  /** 
   * @group Constructors (Primitives)
   */
  val getFetchSize: CallableStatementIO[Int] =
    F.liftFC(GetFetchSize)

  /** 
   * @group Constructors (Primitives)
   */
  def getFloat(a: Int): CallableStatementIO[Float] =
    F.liftFC(GetFloat(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getFloat(a: String): CallableStatementIO[Float] =
    F.liftFC(GetFloat1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getGeneratedKeys: CallableStatementIO[ResultSet] =
    F.liftFC(GetGeneratedKeys)

  /** 
   * @group Constructors (Primitives)
   */
  def getInt(a: Int): CallableStatementIO[Int] =
    F.liftFC(GetInt(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getInt(a: String): CallableStatementIO[Int] =
    F.liftFC(GetInt1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getLong(a: String): CallableStatementIO[Long] =
    F.liftFC(GetLong(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getLong(a: Int): CallableStatementIO[Long] =
    F.liftFC(GetLong1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxFieldSize: CallableStatementIO[Int] =
    F.liftFC(GetMaxFieldSize)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxRows: CallableStatementIO[Int] =
    F.liftFC(GetMaxRows)

  /** 
   * @group Constructors (Primitives)
   */
  val getMetaData: CallableStatementIO[ResultSetMetaData] =
    F.liftFC(GetMetaData)

  /** 
   * @group Constructors (Primitives)
   */
  def getMoreResults(a: Int): CallableStatementIO[Boolean] =
    F.liftFC(GetMoreResults(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getMoreResults: CallableStatementIO[Boolean] =
    F.liftFC(GetMoreResults1)

  /** 
   * @group Constructors (Primitives)
   */
  def getNCharacterStream(a: String): CallableStatementIO[Reader] =
    F.liftFC(GetNCharacterStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNCharacterStream(a: Int): CallableStatementIO[Reader] =
    F.liftFC(GetNCharacterStream1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNClob(a: String): CallableStatementIO[NClob] =
    F.liftFC(GetNClob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNClob(a: Int): CallableStatementIO[NClob] =
    F.liftFC(GetNClob1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNString(a: Int): CallableStatementIO[String] =
    F.liftFC(GetNString(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNString(a: String): CallableStatementIO[String] =
    F.liftFC(GetNString1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject(a: String): CallableStatementIO[Object] =
    F.liftFC(GetObject(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject(a: Int, b: Map[String, Class[_]]): CallableStatementIO[Object] =
    F.liftFC(GetObject1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject(a: Int): CallableStatementIO[Object] =
    F.liftFC(GetObject2(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject(a: String, b: Map[String, Class[_]]): CallableStatementIO[Object] =
    F.liftFC(GetObject3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject[T](a: String, b: Class[T]): CallableStatementIO[T] =
    F.liftFC(GetObject4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject[T](a: Int, b: Class[T]): CallableStatementIO[T] =
    F.liftFC(GetObject5(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val getParameterMetaData: CallableStatementIO[ParameterMetaData] =
    F.liftFC(GetParameterMetaData)

  /** 
   * @group Constructors (Primitives)
   */
  val getQueryTimeout: CallableStatementIO[Int] =
    F.liftFC(GetQueryTimeout)

  /** 
   * @group Constructors (Primitives)
   */
  def getRef(a: String): CallableStatementIO[Ref] =
    F.liftFC(GetRef(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getRef(a: Int): CallableStatementIO[Ref] =
    F.liftFC(GetRef1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSet: CallableStatementIO[ResultSet] =
    F.liftFC(GetResultSet)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSetConcurrency: CallableStatementIO[Int] =
    F.liftFC(GetResultSetConcurrency)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSetHoldability: CallableStatementIO[Int] =
    F.liftFC(GetResultSetHoldability)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSetType: CallableStatementIO[Int] =
    F.liftFC(GetResultSetType)

  /** 
   * @group Constructors (Primitives)
   */
  def getRowId(a: Int): CallableStatementIO[RowId] =
    F.liftFC(GetRowId(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getRowId(a: String): CallableStatementIO[RowId] =
    F.liftFC(GetRowId1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getSQLXML(a: Int): CallableStatementIO[SQLXML] =
    F.liftFC(GetSQLXML(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getSQLXML(a: String): CallableStatementIO[SQLXML] =
    F.liftFC(GetSQLXML1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getShort(a: String): CallableStatementIO[Short] =
    F.liftFC(GetShort(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getShort(a: Int): CallableStatementIO[Short] =
    F.liftFC(GetShort1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getString(a: String): CallableStatementIO[String] =
    F.liftFC(GetString(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getString(a: Int): CallableStatementIO[String] =
    F.liftFC(GetString1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getTime(a: Int): CallableStatementIO[Time] =
    F.liftFC(GetTime(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getTime(a: String, b: Calendar): CallableStatementIO[Time] =
    F.liftFC(GetTime1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getTime(a: Int, b: Calendar): CallableStatementIO[Time] =
    F.liftFC(GetTime2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getTime(a: String): CallableStatementIO[Time] =
    F.liftFC(GetTime3(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getTimestamp(a: Int, b: Calendar): CallableStatementIO[Timestamp] =
    F.liftFC(GetTimestamp(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getTimestamp(a: String): CallableStatementIO[Timestamp] =
    F.liftFC(GetTimestamp1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getTimestamp(a: String, b: Calendar): CallableStatementIO[Timestamp] =
    F.liftFC(GetTimestamp2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getTimestamp(a: Int): CallableStatementIO[Timestamp] =
    F.liftFC(GetTimestamp3(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getURL(a: String): CallableStatementIO[URL] =
    F.liftFC(GetURL(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getURL(a: Int): CallableStatementIO[URL] =
    F.liftFC(GetURL1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getUpdateCount: CallableStatementIO[Int] =
    F.liftFC(GetUpdateCount)

  /** 
   * @group Constructors (Primitives)
   */
  val getWarnings: CallableStatementIO[SQLWarning] =
    F.liftFC(GetWarnings)

  /** 
   * @group Constructors (Primitives)
   */
  val isCloseOnCompletion: CallableStatementIO[Boolean] =
    F.liftFC(IsCloseOnCompletion)

  /** 
   * @group Constructors (Primitives)
   */
  val isClosed: CallableStatementIO[Boolean] =
    F.liftFC(IsClosed)

  /** 
   * @group Constructors (Primitives)
   */
  val isPoolable: CallableStatementIO[Boolean] =
    F.liftFC(IsPoolable)

  /** 
   * @group Constructors (Primitives)
   */
  def isWrapperFor(a: Class[_]): CallableStatementIO[Boolean] =
    F.liftFC(IsWrapperFor(a))

  /** 
   * @group Constructors (Primitives)
   */
  def registerOutParameter(a: String, b: Int, c: Int): CallableStatementIO[Unit] =
    F.liftFC(RegisterOutParameter(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def registerOutParameter(a: String, b: Int, c: String): CallableStatementIO[Unit] =
    F.liftFC(RegisterOutParameter1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def registerOutParameter(a: Int, b: Int): CallableStatementIO[Unit] =
    F.liftFC(RegisterOutParameter2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def registerOutParameter(a: Int, b: Int, c: Int): CallableStatementIO[Unit] =
    F.liftFC(RegisterOutParameter3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def registerOutParameter(a: String, b: Int): CallableStatementIO[Unit] =
    F.liftFC(RegisterOutParameter4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def registerOutParameter(a: Int, b: Int, c: String): CallableStatementIO[Unit] =
    F.liftFC(RegisterOutParameter5(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setArray(a: Int, b: SqlArray): CallableStatementIO[Unit] =
    F.liftFC(SetArray(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: String, b: InputStream, c: Int): CallableStatementIO[Unit] =
    F.liftFC(SetAsciiStream(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: String, b: InputStream, c: Long): CallableStatementIO[Unit] =
    F.liftFC(SetAsciiStream1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: String, b: InputStream): CallableStatementIO[Unit] =
    F.liftFC(SetAsciiStream2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: Int, b: InputStream, c: Long): CallableStatementIO[Unit] =
    F.liftFC(SetAsciiStream3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: Int, b: InputStream, c: Int): CallableStatementIO[Unit] =
    F.liftFC(SetAsciiStream4(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: Int, b: InputStream): CallableStatementIO[Unit] =
    F.liftFC(SetAsciiStream5(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBigDecimal(a: String, b: BigDecimal): CallableStatementIO[Unit] =
    F.liftFC(SetBigDecimal(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBigDecimal(a: Int, b: BigDecimal): CallableStatementIO[Unit] =
    F.liftFC(SetBigDecimal1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: String, b: InputStream): CallableStatementIO[Unit] =
    F.liftFC(SetBinaryStream(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: String, b: InputStream, c: Int): CallableStatementIO[Unit] =
    F.liftFC(SetBinaryStream1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: String, b: InputStream, c: Long): CallableStatementIO[Unit] =
    F.liftFC(SetBinaryStream2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: Int, b: InputStream): CallableStatementIO[Unit] =
    F.liftFC(SetBinaryStream3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: Int, b: InputStream, c: Int): CallableStatementIO[Unit] =
    F.liftFC(SetBinaryStream4(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: Int, b: InputStream, c: Long): CallableStatementIO[Unit] =
    F.liftFC(SetBinaryStream5(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setBlob(a: String, b: InputStream, c: Long): CallableStatementIO[Unit] =
    F.liftFC(SetBlob(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setBlob(a: String, b: InputStream): CallableStatementIO[Unit] =
    F.liftFC(SetBlob1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBlob(a: String, b: Blob): CallableStatementIO[Unit] =
    F.liftFC(SetBlob2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBlob(a: Int, b: InputStream, c: Long): CallableStatementIO[Unit] =
    F.liftFC(SetBlob3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setBlob(a: Int, b: Blob): CallableStatementIO[Unit] =
    F.liftFC(SetBlob4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBlob(a: Int, b: InputStream): CallableStatementIO[Unit] =
    F.liftFC(SetBlob5(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBoolean(a: String, b: Boolean): CallableStatementIO[Unit] =
    F.liftFC(SetBoolean(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBoolean(a: Int, b: Boolean): CallableStatementIO[Unit] =
    F.liftFC(SetBoolean1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setByte(a: String, b: Byte): CallableStatementIO[Unit] =
    F.liftFC(SetByte(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setByte(a: Int, b: Byte): CallableStatementIO[Unit] =
    F.liftFC(SetByte1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBytes(a: String, b: Array[Byte]): CallableStatementIO[Unit] =
    F.liftFC(SetBytes(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBytes(a: Int, b: Array[Byte]): CallableStatementIO[Unit] =
    F.liftFC(SetBytes1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: String, b: Reader, c: Int): CallableStatementIO[Unit] =
    F.liftFC(SetCharacterStream(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: String, b: Reader, c: Long): CallableStatementIO[Unit] =
    F.liftFC(SetCharacterStream1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: String, b: Reader): CallableStatementIO[Unit] =
    F.liftFC(SetCharacterStream2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: Int, b: Reader, c: Long): CallableStatementIO[Unit] =
    F.liftFC(SetCharacterStream3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: Int, b: Reader, c: Int): CallableStatementIO[Unit] =
    F.liftFC(SetCharacterStream4(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: Int, b: Reader): CallableStatementIO[Unit] =
    F.liftFC(SetCharacterStream5(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setClob(a: String, b: Clob): CallableStatementIO[Unit] =
    F.liftFC(SetClob(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setClob(a: String, b: Reader, c: Long): CallableStatementIO[Unit] =
    F.liftFC(SetClob1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setClob(a: String, b: Reader): CallableStatementIO[Unit] =
    F.liftFC(SetClob2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setClob(a: Int, b: Reader, c: Long): CallableStatementIO[Unit] =
    F.liftFC(SetClob3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setClob(a: Int, b: Reader): CallableStatementIO[Unit] =
    F.liftFC(SetClob4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setClob(a: Int, b: Clob): CallableStatementIO[Unit] =
    F.liftFC(SetClob5(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setCursorName(a: String): CallableStatementIO[Unit] =
    F.liftFC(SetCursorName(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setDate(a: String, b: Date): CallableStatementIO[Unit] =
    F.liftFC(SetDate(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setDate(a: String, b: Date, c: Calendar): CallableStatementIO[Unit] =
    F.liftFC(SetDate1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setDate(a: Int, b: Date, c: Calendar): CallableStatementIO[Unit] =
    F.liftFC(SetDate2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setDate(a: Int, b: Date): CallableStatementIO[Unit] =
    F.liftFC(SetDate3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setDouble(a: String, b: Double): CallableStatementIO[Unit] =
    F.liftFC(SetDouble(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setDouble(a: Int, b: Double): CallableStatementIO[Unit] =
    F.liftFC(SetDouble1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setEscapeProcessing(a: Boolean): CallableStatementIO[Unit] =
    F.liftFC(SetEscapeProcessing(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setFetchDirection(a: Int): CallableStatementIO[Unit] =
    F.liftFC(SetFetchDirection(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setFetchSize(a: Int): CallableStatementIO[Unit] =
    F.liftFC(SetFetchSize(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setFloat(a: String, b: Float): CallableStatementIO[Unit] =
    F.liftFC(SetFloat(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setFloat(a: Int, b: Float): CallableStatementIO[Unit] =
    F.liftFC(SetFloat1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setInt(a: String, b: Int): CallableStatementIO[Unit] =
    F.liftFC(SetInt(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setInt(a: Int, b: Int): CallableStatementIO[Unit] =
    F.liftFC(SetInt1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setLong(a: String, b: Long): CallableStatementIO[Unit] =
    F.liftFC(SetLong(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setLong(a: Int, b: Long): CallableStatementIO[Unit] =
    F.liftFC(SetLong1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setMaxFieldSize(a: Int): CallableStatementIO[Unit] =
    F.liftFC(SetMaxFieldSize(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setMaxRows(a: Int): CallableStatementIO[Unit] =
    F.liftFC(SetMaxRows(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setNCharacterStream(a: String, b: Reader, c: Long): CallableStatementIO[Unit] =
    F.liftFC(SetNCharacterStream(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setNCharacterStream(a: String, b: Reader): CallableStatementIO[Unit] =
    F.liftFC(SetNCharacterStream1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setNCharacterStream(a: Int, b: Reader): CallableStatementIO[Unit] =
    F.liftFC(SetNCharacterStream2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setNCharacterStream(a: Int, b: Reader, c: Long): CallableStatementIO[Unit] =
    F.liftFC(SetNCharacterStream3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setNClob(a: String, b: NClob): CallableStatementIO[Unit] =
    F.liftFC(SetNClob(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setNClob(a: String, b: Reader, c: Long): CallableStatementIO[Unit] =
    F.liftFC(SetNClob1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setNClob(a: String, b: Reader): CallableStatementIO[Unit] =
    F.liftFC(SetNClob2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setNClob(a: Int, b: Reader): CallableStatementIO[Unit] =
    F.liftFC(SetNClob3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setNClob(a: Int, b: Reader, c: Long): CallableStatementIO[Unit] =
    F.liftFC(SetNClob4(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setNClob(a: Int, b: NClob): CallableStatementIO[Unit] =
    F.liftFC(SetNClob5(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setNString(a: String, b: String): CallableStatementIO[Unit] =
    F.liftFC(SetNString(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setNString(a: Int, b: String): CallableStatementIO[Unit] =
    F.liftFC(SetNString1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setNull(a: String, b: Int): CallableStatementIO[Unit] =
    F.liftFC(SetNull(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setNull(a: String, b: Int, c: String): CallableStatementIO[Unit] =
    F.liftFC(SetNull1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setNull(a: Int, b: Int, c: String): CallableStatementIO[Unit] =
    F.liftFC(SetNull2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setNull(a: Int, b: Int): CallableStatementIO[Unit] =
    F.liftFC(SetNull3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setObject(a: String, b: Object): CallableStatementIO[Unit] =
    F.liftFC(SetObject(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setObject(a: String, b: Object, c: Int): CallableStatementIO[Unit] =
    F.liftFC(SetObject1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setObject(a: String, b: Object, c: Int, d: Int): CallableStatementIO[Unit] =
    F.liftFC(SetObject2(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def setObject(a: Int, b: Object, c: Int): CallableStatementIO[Unit] =
    F.liftFC(SetObject3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setObject(a: Int, b: Object): CallableStatementIO[Unit] =
    F.liftFC(SetObject4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setObject(a: Int, b: Object, c: Int, d: Int): CallableStatementIO[Unit] =
    F.liftFC(SetObject5(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def setPoolable(a: Boolean): CallableStatementIO[Unit] =
    F.liftFC(SetPoolable(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setQueryTimeout(a: Int): CallableStatementIO[Unit] =
    F.liftFC(SetQueryTimeout(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setRef(a: Int, b: Ref): CallableStatementIO[Unit] =
    F.liftFC(SetRef(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setRowId(a: String, b: RowId): CallableStatementIO[Unit] =
    F.liftFC(SetRowId(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setRowId(a: Int, b: RowId): CallableStatementIO[Unit] =
    F.liftFC(SetRowId1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setSQLXML(a: String, b: SQLXML): CallableStatementIO[Unit] =
    F.liftFC(SetSQLXML(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setSQLXML(a: Int, b: SQLXML): CallableStatementIO[Unit] =
    F.liftFC(SetSQLXML1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setShort(a: String, b: Short): CallableStatementIO[Unit] =
    F.liftFC(SetShort(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setShort(a: Int, b: Short): CallableStatementIO[Unit] =
    F.liftFC(SetShort1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setString(a: String, b: String): CallableStatementIO[Unit] =
    F.liftFC(SetString(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setString(a: Int, b: String): CallableStatementIO[Unit] =
    F.liftFC(SetString1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setTime(a: String, b: Time, c: Calendar): CallableStatementIO[Unit] =
    F.liftFC(SetTime(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setTime(a: String, b: Time): CallableStatementIO[Unit] =
    F.liftFC(SetTime1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setTime(a: Int, b: Time): CallableStatementIO[Unit] =
    F.liftFC(SetTime2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setTime(a: Int, b: Time, c: Calendar): CallableStatementIO[Unit] =
    F.liftFC(SetTime3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setTimestamp(a: String, b: Timestamp): CallableStatementIO[Unit] =
    F.liftFC(SetTimestamp(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setTimestamp(a: String, b: Timestamp, c: Calendar): CallableStatementIO[Unit] =
    F.liftFC(SetTimestamp1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setTimestamp(a: Int, b: Timestamp): CallableStatementIO[Unit] =
    F.liftFC(SetTimestamp2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setTimestamp(a: Int, b: Timestamp, c: Calendar): CallableStatementIO[Unit] =
    F.liftFC(SetTimestamp3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def setURL(a: String, b: URL): CallableStatementIO[Unit] =
    F.liftFC(SetURL(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setURL(a: Int, b: URL): CallableStatementIO[Unit] =
    F.liftFC(SetURL1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setUnicodeStream(a: Int, b: InputStream, c: Int): CallableStatementIO[Unit] =
    F.liftFC(SetUnicodeStream(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def unwrap[T](a: Class[T]): CallableStatementIO[T] =
    F.liftFC(Unwrap(a))

  /** 
   * @group Constructors (Primitives)
   */
  val wasNull: CallableStatementIO[Boolean] =
    F.liftFC(WasNull)

 /** 
  * Natural transformation from `CallableStatementOp` to `Kleisli` for the given `M`, consuming a `java.sql.CallableStatement`. 
  * @group Algebra
  */
  def kleisliTrans[M[_]: Monad: Catchable: Capture]: CallableStatementOp ~> Kleisli[M, CallableStatement, ?] =
    new (CallableStatementOp ~> Kleisli[M, CallableStatement, ?]) {
      def apply[A](op: CallableStatementOp[A]): Kleisli[M, CallableStatement, A] =
        op.defaultTransK[M]
    }

  /**
   * Syntax for `CallableStatementIO`.
   * @group Algebra
   */
  implicit class CallableStatementIOOps[A](ma: CallableStatementIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, CallableStatement, A] =
      F.runFC[CallableStatementOp, Kleisli[M, CallableStatement, ?], A](ma)(kleisliTrans[M])
  }

}

