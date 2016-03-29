package doobie.free

import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

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
 * Algebra and free monad for primitive operations over a `java.sql.ResultSet`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `ResultSetIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `ResultSetOp` to another monad via
 * `Free#foldMap`.
 *
 * The library provides a natural transformation to `Kleisli[M, ResultSet, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: ResultSetIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: ResultSet = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object resultset {
  
  /** 
   * Sum type of primitive operations over a `java.sql.ResultSet`.
   * @group Algebra 
   */
  sealed trait ResultSetOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: ResultSet => A): Kleisli[M, ResultSet, A] = 
      Kleisli((s: ResultSet) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, ResultSet, A]
  }

  /** 
   * Module of constructors for `ResultSetOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `resultset` module.
   * @group Algebra 
   */
  object ResultSetOp {
    
    // This algebra has a default interpreter
    implicit val ResultSetKleisliTrans: KleisliTrans.Aux[ResultSetOp, ResultSet] =
      new KleisliTrans[ResultSetOp] {
        type J = ResultSet
        def interpK[M[_]: Monad: Catchable: Capture]: ResultSetOp ~> Kleisli[M, ResultSet, ?] =
          new (ResultSetOp ~> Kleisli[M, ResultSet, ?]) {
            def apply[A](op: ResultSetOp[A]): Kleisli[M, ResultSet, A] =
              op.defaultTransK[M]
          }
      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F[Op, A], mod: KleisliTrans.Aux[Op, J]) extends ResultSetOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
    }

    // Combinators
    case class Attempt[A](action: ResultSetIO[A]) extends ResultSetOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, ResultSet, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends ResultSetOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: ResultSet => A) extends ResultSetOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case class  Absolute(a: Int) extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.absolute(a))
    }
    case object AfterLast extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.afterLast())
    }
    case object BeforeFirst extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.beforeFirst())
    }
    case object CancelRowUpdates extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.cancelRowUpdates())
    }
    case object ClearWarnings extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.clearWarnings())
    }
    case object Close extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.close())
    }
    case object DeleteRow extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.deleteRow())
    }
    case class  FindColumn(a: String) extends ResultSetOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.findColumn(a))
    }
    case object First extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.first())
    }
    case class  GetArray(a: String) extends ResultSetOp[SqlArray] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getArray(a))
    }
    case class  GetArray1(a: Int) extends ResultSetOp[SqlArray] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getArray(a))
    }
    case class  GetAsciiStream(a: Int) extends ResultSetOp[InputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getAsciiStream(a))
    }
    case class  GetAsciiStream1(a: String) extends ResultSetOp[InputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getAsciiStream(a))
    }
    case class  GetBigDecimal(a: Int, b: Int) extends ResultSetOp[BigDecimal] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBigDecimal(a, b))
    }
    case class  GetBigDecimal1(a: String, b: Int) extends ResultSetOp[BigDecimal] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBigDecimal(a, b))
    }
    case class  GetBigDecimal2(a: Int) extends ResultSetOp[BigDecimal] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBigDecimal(a))
    }
    case class  GetBigDecimal3(a: String) extends ResultSetOp[BigDecimal] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBigDecimal(a))
    }
    case class  GetBinaryStream(a: Int) extends ResultSetOp[InputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBinaryStream(a))
    }
    case class  GetBinaryStream1(a: String) extends ResultSetOp[InputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBinaryStream(a))
    }
    case class  GetBlob(a: Int) extends ResultSetOp[Blob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBlob(a))
    }
    case class  GetBlob1(a: String) extends ResultSetOp[Blob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBlob(a))
    }
    case class  GetBoolean(a: Int) extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBoolean(a))
    }
    case class  GetBoolean1(a: String) extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBoolean(a))
    }
    case class  GetByte(a: String) extends ResultSetOp[Byte] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getByte(a))
    }
    case class  GetByte1(a: Int) extends ResultSetOp[Byte] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getByte(a))
    }
    case class  GetBytes(a: Int) extends ResultSetOp[Array[Byte]] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBytes(a))
    }
    case class  GetBytes1(a: String) extends ResultSetOp[Array[Byte]] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBytes(a))
    }
    case class  GetCharacterStream(a: String) extends ResultSetOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCharacterStream(a))
    }
    case class  GetCharacterStream1(a: Int) extends ResultSetOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCharacterStream(a))
    }
    case class  GetClob(a: String) extends ResultSetOp[Clob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getClob(a))
    }
    case class  GetClob1(a: Int) extends ResultSetOp[Clob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getClob(a))
    }
    case object GetConcurrency extends ResultSetOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getConcurrency())
    }
    case object GetCursorName extends ResultSetOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCursorName())
    }
    case class  GetDate(a: Int) extends ResultSetOp[Date] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDate(a))
    }
    case class  GetDate1(a: Int, b: Calendar) extends ResultSetOp[Date] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDate(a, b))
    }
    case class  GetDate2(a: String) extends ResultSetOp[Date] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDate(a))
    }
    case class  GetDate3(a: String, b: Calendar) extends ResultSetOp[Date] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDate(a, b))
    }
    case class  GetDouble(a: String) extends ResultSetOp[Double] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDouble(a))
    }
    case class  GetDouble1(a: Int) extends ResultSetOp[Double] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDouble(a))
    }
    case object GetFetchDirection extends ResultSetOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFetchDirection())
    }
    case object GetFetchSize extends ResultSetOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFetchSize())
    }
    case class  GetFloat(a: String) extends ResultSetOp[Float] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFloat(a))
    }
    case class  GetFloat1(a: Int) extends ResultSetOp[Float] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFloat(a))
    }
    case object GetHoldability extends ResultSetOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getHoldability())
    }
    case class  GetInt(a: Int) extends ResultSetOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getInt(a))
    }
    case class  GetInt1(a: String) extends ResultSetOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getInt(a))
    }
    case class  GetLong(a: Int) extends ResultSetOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getLong(a))
    }
    case class  GetLong1(a: String) extends ResultSetOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getLong(a))
    }
    case object GetMetaData extends ResultSetOp[ResultSetMetaData] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMetaData())
    }
    case class  GetNCharacterStream(a: String) extends ResultSetOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getNCharacterStream(a))
    }
    case class  GetNCharacterStream1(a: Int) extends ResultSetOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getNCharacterStream(a))
    }
    case class  GetNClob(a: String) extends ResultSetOp[NClob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getNClob(a))
    }
    case class  GetNClob1(a: Int) extends ResultSetOp[NClob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getNClob(a))
    }
    case class  GetNString(a: String) extends ResultSetOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getNString(a))
    }
    case class  GetNString1(a: Int) extends ResultSetOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getNString(a))
    }
    case class  GetObject[T](a: Int, b: Class[T]) extends ResultSetOp[T] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a, b))
    }
    case class  GetObject1(a: String, b: Map[String, Class[_]]) extends ResultSetOp[Object] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a, b))
    }
    case class  GetObject2(a: String) extends ResultSetOp[Object] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a))
    }
    case class  GetObject3(a: Int, b: Map[String, Class[_]]) extends ResultSetOp[Object] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a, b))
    }
    case class  GetObject4(a: Int) extends ResultSetOp[Object] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a))
    }
    case class  GetObject5[T](a: String, b: Class[T]) extends ResultSetOp[T] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a, b))
    }
    case class  GetRef(a: Int) extends ResultSetOp[Ref] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getRef(a))
    }
    case class  GetRef1(a: String) extends ResultSetOp[Ref] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getRef(a))
    }
    case object GetRow extends ResultSetOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getRow())
    }
    case class  GetRowId(a: Int) extends ResultSetOp[RowId] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getRowId(a))
    }
    case class  GetRowId1(a: String) extends ResultSetOp[RowId] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getRowId(a))
    }
    case class  GetSQLXML(a: String) extends ResultSetOp[SQLXML] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSQLXML(a))
    }
    case class  GetSQLXML1(a: Int) extends ResultSetOp[SQLXML] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSQLXML(a))
    }
    case class  GetShort(a: String) extends ResultSetOp[Short] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getShort(a))
    }
    case class  GetShort1(a: Int) extends ResultSetOp[Short] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getShort(a))
    }
    case object GetStatement extends ResultSetOp[Statement] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getStatement())
    }
    case class  GetString(a: Int) extends ResultSetOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getString(a))
    }
    case class  GetString1(a: String) extends ResultSetOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getString(a))
    }
    case class  GetTime(a: String) extends ResultSetOp[Time] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTime(a))
    }
    case class  GetTime1(a: Int) extends ResultSetOp[Time] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTime(a))
    }
    case class  GetTime2(a: String, b: Calendar) extends ResultSetOp[Time] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTime(a, b))
    }
    case class  GetTime3(a: Int, b: Calendar) extends ResultSetOp[Time] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTime(a, b))
    }
    case class  GetTimestamp(a: Int) extends ResultSetOp[Timestamp] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTimestamp(a))
    }
    case class  GetTimestamp1(a: String, b: Calendar) extends ResultSetOp[Timestamp] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTimestamp(a, b))
    }
    case class  GetTimestamp2(a: String) extends ResultSetOp[Timestamp] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTimestamp(a))
    }
    case class  GetTimestamp3(a: Int, b: Calendar) extends ResultSetOp[Timestamp] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTimestamp(a, b))
    }
    case object GetType extends ResultSetOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getType())
    }
    case class  GetURL(a: Int) extends ResultSetOp[URL] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getURL(a))
    }
    case class  GetURL1(a: String) extends ResultSetOp[URL] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getURL(a))
    }
    case class  GetUnicodeStream(a: Int) extends ResultSetOp[InputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getUnicodeStream(a))
    }
    case class  GetUnicodeStream1(a: String) extends ResultSetOp[InputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getUnicodeStream(a))
    }
    case object GetWarnings extends ResultSetOp[SQLWarning] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getWarnings())
    }
    case object InsertRow extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.insertRow())
    }
    case object IsAfterLast extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isAfterLast())
    }
    case object IsBeforeFirst extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isBeforeFirst())
    }
    case object IsClosed extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isClosed())
    }
    case object IsFirst extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isFirst())
    }
    case object IsLast extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isLast())
    }
    case class  IsWrapperFor(a: Class[_]) extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isWrapperFor(a))
    }
    case object Last extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.last())
    }
    case object MoveToCurrentRow extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.moveToCurrentRow())
    }
    case object MoveToInsertRow extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.moveToInsertRow())
    }
    case object Next extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.next())
    }
    case object Previous extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.previous())
    }
    case object RefreshRow extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.refreshRow())
    }
    case class  Relative(a: Int) extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.relative(a))
    }
    case object RowDeleted extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.rowDeleted())
    }
    case object RowInserted extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.rowInserted())
    }
    case object RowUpdated extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.rowUpdated())
    }
    case class  SetFetchDirection(a: Int) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setFetchDirection(a))
    }
    case class  SetFetchSize(a: Int) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setFetchSize(a))
    }
    case class  Unwrap[T](a: Class[T]) extends ResultSetOp[T] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.unwrap(a))
    }
    case class  UpdateArray(a: String, b: SqlArray) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateArray(a, b))
    }
    case class  UpdateArray1(a: Int, b: SqlArray) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateArray(a, b))
    }
    case class  UpdateAsciiStream(a: String, b: InputStream, c: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateAsciiStream(a, b, c))
    }
    case class  UpdateAsciiStream1(a: String, b: InputStream) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateAsciiStream(a, b))
    }
    case class  UpdateAsciiStream2(a: String, b: InputStream, c: Int) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateAsciiStream(a, b, c))
    }
    case class  UpdateAsciiStream3(a: Int, b: InputStream, c: Int) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateAsciiStream(a, b, c))
    }
    case class  UpdateAsciiStream4(a: Int, b: InputStream) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateAsciiStream(a, b))
    }
    case class  UpdateAsciiStream5(a: Int, b: InputStream, c: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateAsciiStream(a, b, c))
    }
    case class  UpdateBigDecimal(a: Int, b: BigDecimal) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBigDecimal(a, b))
    }
    case class  UpdateBigDecimal1(a: String, b: BigDecimal) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBigDecimal(a, b))
    }
    case class  UpdateBinaryStream(a: Int, b: InputStream, c: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBinaryStream(a, b, c))
    }
    case class  UpdateBinaryStream1(a: String, b: InputStream, c: Int) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBinaryStream(a, b, c))
    }
    case class  UpdateBinaryStream2(a: String, b: InputStream) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBinaryStream(a, b))
    }
    case class  UpdateBinaryStream3(a: String, b: InputStream, c: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBinaryStream(a, b, c))
    }
    case class  UpdateBinaryStream4(a: Int, b: InputStream) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBinaryStream(a, b))
    }
    case class  UpdateBinaryStream5(a: Int, b: InputStream, c: Int) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBinaryStream(a, b, c))
    }
    case class  UpdateBlob(a: Int, b: Blob) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBlob(a, b))
    }
    case class  UpdateBlob1(a: Int, b: InputStream, c: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBlob(a, b, c))
    }
    case class  UpdateBlob2(a: Int, b: InputStream) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBlob(a, b))
    }
    case class  UpdateBlob3(a: String, b: InputStream, c: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBlob(a, b, c))
    }
    case class  UpdateBlob4(a: String, b: InputStream) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBlob(a, b))
    }
    case class  UpdateBlob5(a: String, b: Blob) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBlob(a, b))
    }
    case class  UpdateBoolean(a: Int, b: Boolean) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBoolean(a, b))
    }
    case class  UpdateBoolean1(a: String, b: Boolean) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBoolean(a, b))
    }
    case class  UpdateByte(a: String, b: Byte) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateByte(a, b))
    }
    case class  UpdateByte1(a: Int, b: Byte) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateByte(a, b))
    }
    case class  UpdateBytes(a: String, b: Array[Byte]) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBytes(a, b))
    }
    case class  UpdateBytes1(a: Int, b: Array[Byte]) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateBytes(a, b))
    }
    case class  UpdateCharacterStream(a: String, b: Reader, c: Int) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateCharacterStream(a, b, c))
    }
    case class  UpdateCharacterStream1(a: Int, b: Reader, c: Int) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateCharacterStream(a, b, c))
    }
    case class  UpdateCharacterStream2(a: Int, b: Reader, c: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateCharacterStream(a, b, c))
    }
    case class  UpdateCharacterStream3(a: String, b: Reader, c: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateCharacterStream(a, b, c))
    }
    case class  UpdateCharacterStream4(a: Int, b: Reader) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateCharacterStream(a, b))
    }
    case class  UpdateCharacterStream5(a: String, b: Reader) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateCharacterStream(a, b))
    }
    case class  UpdateClob(a: Int, b: Reader, c: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateClob(a, b, c))
    }
    case class  UpdateClob1(a: Int, b: Clob) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateClob(a, b))
    }
    case class  UpdateClob2(a: String, b: Clob) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateClob(a, b))
    }
    case class  UpdateClob3(a: Int, b: Reader) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateClob(a, b))
    }
    case class  UpdateClob4(a: String, b: Reader) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateClob(a, b))
    }
    case class  UpdateClob5(a: String, b: Reader, c: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateClob(a, b, c))
    }
    case class  UpdateDate(a: String, b: Date) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateDate(a, b))
    }
    case class  UpdateDate1(a: Int, b: Date) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateDate(a, b))
    }
    case class  UpdateDouble(a: String, b: Double) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateDouble(a, b))
    }
    case class  UpdateDouble1(a: Int, b: Double) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateDouble(a, b))
    }
    case class  UpdateFloat(a: String, b: Float) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateFloat(a, b))
    }
    case class  UpdateFloat1(a: Int, b: Float) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateFloat(a, b))
    }
    case class  UpdateInt(a: Int, b: Int) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateInt(a, b))
    }
    case class  UpdateInt1(a: String, b: Int) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateInt(a, b))
    }
    case class  UpdateLong(a: Int, b: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateLong(a, b))
    }
    case class  UpdateLong1(a: String, b: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateLong(a, b))
    }
    case class  UpdateNCharacterStream(a: Int, b: Reader, c: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateNCharacterStream(a, b, c))
    }
    case class  UpdateNCharacterStream1(a: String, b: Reader, c: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateNCharacterStream(a, b, c))
    }
    case class  UpdateNCharacterStream2(a: Int, b: Reader) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateNCharacterStream(a, b))
    }
    case class  UpdateNCharacterStream3(a: String, b: Reader) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateNCharacterStream(a, b))
    }
    case class  UpdateNClob(a: Int, b: Reader, c: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateNClob(a, b, c))
    }
    case class  UpdateNClob1(a: String, b: Reader) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateNClob(a, b))
    }
    case class  UpdateNClob2(a: String, b: Reader, c: Long) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateNClob(a, b, c))
    }
    case class  UpdateNClob3(a: Int, b: Reader) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateNClob(a, b))
    }
    case class  UpdateNClob4(a: String, b: NClob) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateNClob(a, b))
    }
    case class  UpdateNClob5(a: Int, b: NClob) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateNClob(a, b))
    }
    case class  UpdateNString(a: Int, b: String) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateNString(a, b))
    }
    case class  UpdateNString1(a: String, b: String) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateNString(a, b))
    }
    case class  UpdateNull(a: String) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateNull(a))
    }
    case class  UpdateNull1(a: Int) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateNull(a))
    }
    case class  UpdateObject(a: String, b: Object) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateObject(a, b))
    }
    case class  UpdateObject1(a: String, b: Object, c: Int) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateObject(a, b, c))
    }
    case class  UpdateObject2(a: Int, b: Object, c: Int) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateObject(a, b, c))
    }
    case class  UpdateObject3(a: Int, b: Object) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateObject(a, b))
    }
    case class  UpdateRef(a: String, b: Ref) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateRef(a, b))
    }
    case class  UpdateRef1(a: Int, b: Ref) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateRef(a, b))
    }
    case object UpdateRow extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateRow())
    }
    case class  UpdateRowId(a: Int, b: RowId) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateRowId(a, b))
    }
    case class  UpdateRowId1(a: String, b: RowId) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateRowId(a, b))
    }
    case class  UpdateSQLXML(a: Int, b: SQLXML) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateSQLXML(a, b))
    }
    case class  UpdateSQLXML1(a: String, b: SQLXML) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateSQLXML(a, b))
    }
    case class  UpdateShort(a: String, b: Short) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateShort(a, b))
    }
    case class  UpdateShort1(a: Int, b: Short) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateShort(a, b))
    }
    case class  UpdateString(a: String, b: String) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateString(a, b))
    }
    case class  UpdateString1(a: Int, b: String) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateString(a, b))
    }
    case class  UpdateTime(a: String, b: Time) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateTime(a, b))
    }
    case class  UpdateTime1(a: Int, b: Time) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateTime(a, b))
    }
    case class  UpdateTimestamp(a: Int, b: Timestamp) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateTimestamp(a, b))
    }
    case class  UpdateTimestamp1(a: String, b: Timestamp) extends ResultSetOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updateTimestamp(a, b))
    }
    case object WasNull extends ResultSetOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.wasNull())
    }

  }
  import ResultSetOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[ResultSetOp]]; abstractly, a computation that consumes 
   * a `java.sql.ResultSet` and produces a value of type `A`. 
   * @group Algebra 
   */
  type ResultSetIO[A] = F[ResultSetOp, A]

  /**
   * Catchable instance for [[ResultSetIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableResultSetIO: Catchable[ResultSetIO] =
    new Catchable[ResultSetIO] {
      def attempt[A](f: ResultSetIO[A]): ResultSetIO[Throwable \/ A] = resultset.attempt(f)
      def fail[A](err: Throwable): ResultSetIO[A] = resultset.delay(throw err)
    }

  /**
   * Capture instance for [[ResultSetIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureResultSetIO: Capture[ResultSetIO] =
    new Capture[ResultSetIO] {
      def apply[A](a: => A): ResultSetIO[A] = resultset.delay(a)
    }

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): ResultSetIO[A] =
    F.liftF(Lift(j, action, mod))

  /** 
   * Lift a ResultSetIO[A] into an exception-capturing ResultSetIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: ResultSetIO[A]): ResultSetIO[Throwable \/ A] =
    F.liftF[ResultSetOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): ResultSetIO[A] =
    F.liftF(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying ResultSet.
   * @group Constructors (Lifting)
   */
  def raw[A](f: ResultSet => A): ResultSetIO[A] =
    F.liftF(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  def absolute(a: Int): ResultSetIO[Boolean] =
    F.liftF(Absolute(a))

  /** 
   * @group Constructors (Primitives)
   */
  val afterLast: ResultSetIO[Unit] =
    F.liftF(AfterLast)

  /** 
   * @group Constructors (Primitives)
   */
  val beforeFirst: ResultSetIO[Unit] =
    F.liftF(BeforeFirst)

  /** 
   * @group Constructors (Primitives)
   */
  val cancelRowUpdates: ResultSetIO[Unit] =
    F.liftF(CancelRowUpdates)

  /** 
   * @group Constructors (Primitives)
   */
  val clearWarnings: ResultSetIO[Unit] =
    F.liftF(ClearWarnings)

  /** 
   * @group Constructors (Primitives)
   */
  val close: ResultSetIO[Unit] =
    F.liftF(Close)

  /** 
   * @group Constructors (Primitives)
   */
  val deleteRow: ResultSetIO[Unit] =
    F.liftF(DeleteRow)

  /** 
   * @group Constructors (Primitives)
   */
  def findColumn(a: String): ResultSetIO[Int] =
    F.liftF(FindColumn(a))

  /** 
   * @group Constructors (Primitives)
   */
  val first: ResultSetIO[Boolean] =
    F.liftF(First)

  /** 
   * @group Constructors (Primitives)
   */
  def getArray(a: String): ResultSetIO[SqlArray] =
    F.liftF(GetArray(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getArray(a: Int): ResultSetIO[SqlArray] =
    F.liftF(GetArray1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getAsciiStream(a: Int): ResultSetIO[InputStream] =
    F.liftF(GetAsciiStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getAsciiStream(a: String): ResultSetIO[InputStream] =
    F.liftF(GetAsciiStream1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBigDecimal(a: Int, b: Int): ResultSetIO[BigDecimal] =
    F.liftF(GetBigDecimal(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getBigDecimal(a: String, b: Int): ResultSetIO[BigDecimal] =
    F.liftF(GetBigDecimal1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getBigDecimal(a: Int): ResultSetIO[BigDecimal] =
    F.liftF(GetBigDecimal2(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBigDecimal(a: String): ResultSetIO[BigDecimal] =
    F.liftF(GetBigDecimal3(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBinaryStream(a: Int): ResultSetIO[InputStream] =
    F.liftF(GetBinaryStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBinaryStream(a: String): ResultSetIO[InputStream] =
    F.liftF(GetBinaryStream1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBlob(a: Int): ResultSetIO[Blob] =
    F.liftF(GetBlob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBlob(a: String): ResultSetIO[Blob] =
    F.liftF(GetBlob1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBoolean(a: Int): ResultSetIO[Boolean] =
    F.liftF(GetBoolean(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBoolean(a: String): ResultSetIO[Boolean] =
    F.liftF(GetBoolean1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getByte(a: String): ResultSetIO[Byte] =
    F.liftF(GetByte(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getByte(a: Int): ResultSetIO[Byte] =
    F.liftF(GetByte1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBytes(a: Int): ResultSetIO[Array[Byte]] =
    F.liftF(GetBytes(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBytes(a: String): ResultSetIO[Array[Byte]] =
    F.liftF(GetBytes1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getCharacterStream(a: String): ResultSetIO[Reader] =
    F.liftF(GetCharacterStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getCharacterStream(a: Int): ResultSetIO[Reader] =
    F.liftF(GetCharacterStream1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getClob(a: String): ResultSetIO[Clob] =
    F.liftF(GetClob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getClob(a: Int): ResultSetIO[Clob] =
    F.liftF(GetClob1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getConcurrency: ResultSetIO[Int] =
    F.liftF(GetConcurrency)

  /** 
   * @group Constructors (Primitives)
   */
  val getCursorName: ResultSetIO[String] =
    F.liftF(GetCursorName)

  /** 
   * @group Constructors (Primitives)
   */
  def getDate(a: Int): ResultSetIO[Date] =
    F.liftF(GetDate(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getDate(a: Int, b: Calendar): ResultSetIO[Date] =
    F.liftF(GetDate1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getDate(a: String): ResultSetIO[Date] =
    F.liftF(GetDate2(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getDate(a: String, b: Calendar): ResultSetIO[Date] =
    F.liftF(GetDate3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getDouble(a: String): ResultSetIO[Double] =
    F.liftF(GetDouble(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getDouble(a: Int): ResultSetIO[Double] =
    F.liftF(GetDouble1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getFetchDirection: ResultSetIO[Int] =
    F.liftF(GetFetchDirection)

  /** 
   * @group Constructors (Primitives)
   */
  val getFetchSize: ResultSetIO[Int] =
    F.liftF(GetFetchSize)

  /** 
   * @group Constructors (Primitives)
   */
  def getFloat(a: String): ResultSetIO[Float] =
    F.liftF(GetFloat(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getFloat(a: Int): ResultSetIO[Float] =
    F.liftF(GetFloat1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getHoldability: ResultSetIO[Int] =
    F.liftF(GetHoldability)

  /** 
   * @group Constructors (Primitives)
   */
  def getInt(a: Int): ResultSetIO[Int] =
    F.liftF(GetInt(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getInt(a: String): ResultSetIO[Int] =
    F.liftF(GetInt1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getLong(a: Int): ResultSetIO[Long] =
    F.liftF(GetLong(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getLong(a: String): ResultSetIO[Long] =
    F.liftF(GetLong1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getMetaData: ResultSetIO[ResultSetMetaData] =
    F.liftF(GetMetaData)

  /** 
   * @group Constructors (Primitives)
   */
  def getNCharacterStream(a: String): ResultSetIO[Reader] =
    F.liftF(GetNCharacterStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNCharacterStream(a: Int): ResultSetIO[Reader] =
    F.liftF(GetNCharacterStream1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNClob(a: String): ResultSetIO[NClob] =
    F.liftF(GetNClob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNClob(a: Int): ResultSetIO[NClob] =
    F.liftF(GetNClob1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNString(a: String): ResultSetIO[String] =
    F.liftF(GetNString(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNString(a: Int): ResultSetIO[String] =
    F.liftF(GetNString1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject[T](a: Int, b: Class[T]): ResultSetIO[T] =
    F.liftF(GetObject(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject(a: String, b: Map[String, Class[_]]): ResultSetIO[Object] =
    F.liftF(GetObject1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject(a: String): ResultSetIO[Object] =
    F.liftF(GetObject2(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject(a: Int, b: Map[String, Class[_]]): ResultSetIO[Object] =
    F.liftF(GetObject3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject(a: Int): ResultSetIO[Object] =
    F.liftF(GetObject4(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject[T](a: String, b: Class[T]): ResultSetIO[T] =
    F.liftF(GetObject5(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getRef(a: Int): ResultSetIO[Ref] =
    F.liftF(GetRef(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getRef(a: String): ResultSetIO[Ref] =
    F.liftF(GetRef1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getRow: ResultSetIO[Int] =
    F.liftF(GetRow)

  /** 
   * @group Constructors (Primitives)
   */
  def getRowId(a: Int): ResultSetIO[RowId] =
    F.liftF(GetRowId(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getRowId(a: String): ResultSetIO[RowId] =
    F.liftF(GetRowId1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getSQLXML(a: String): ResultSetIO[SQLXML] =
    F.liftF(GetSQLXML(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getSQLXML(a: Int): ResultSetIO[SQLXML] =
    F.liftF(GetSQLXML1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getShort(a: String): ResultSetIO[Short] =
    F.liftF(GetShort(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getShort(a: Int): ResultSetIO[Short] =
    F.liftF(GetShort1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getStatement: ResultSetIO[Statement] =
    F.liftF(GetStatement)

  /** 
   * @group Constructors (Primitives)
   */
  def getString(a: Int): ResultSetIO[String] =
    F.liftF(GetString(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getString(a: String): ResultSetIO[String] =
    F.liftF(GetString1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getTime(a: String): ResultSetIO[Time] =
    F.liftF(GetTime(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getTime(a: Int): ResultSetIO[Time] =
    F.liftF(GetTime1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getTime(a: String, b: Calendar): ResultSetIO[Time] =
    F.liftF(GetTime2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getTime(a: Int, b: Calendar): ResultSetIO[Time] =
    F.liftF(GetTime3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getTimestamp(a: Int): ResultSetIO[Timestamp] =
    F.liftF(GetTimestamp(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getTimestamp(a: String, b: Calendar): ResultSetIO[Timestamp] =
    F.liftF(GetTimestamp1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getTimestamp(a: String): ResultSetIO[Timestamp] =
    F.liftF(GetTimestamp2(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getTimestamp(a: Int, b: Calendar): ResultSetIO[Timestamp] =
    F.liftF(GetTimestamp3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val getType: ResultSetIO[Int] =
    F.liftF(GetType)

  /** 
   * @group Constructors (Primitives)
   */
  def getURL(a: Int): ResultSetIO[URL] =
    F.liftF(GetURL(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getURL(a: String): ResultSetIO[URL] =
    F.liftF(GetURL1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getUnicodeStream(a: Int): ResultSetIO[InputStream] =
    F.liftF(GetUnicodeStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getUnicodeStream(a: String): ResultSetIO[InputStream] =
    F.liftF(GetUnicodeStream1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getWarnings: ResultSetIO[SQLWarning] =
    F.liftF(GetWarnings)

  /** 
   * @group Constructors (Primitives)
   */
  val insertRow: ResultSetIO[Unit] =
    F.liftF(InsertRow)

  /** 
   * @group Constructors (Primitives)
   */
  val isAfterLast: ResultSetIO[Boolean] =
    F.liftF(IsAfterLast)

  /** 
   * @group Constructors (Primitives)
   */
  val isBeforeFirst: ResultSetIO[Boolean] =
    F.liftF(IsBeforeFirst)

  /** 
   * @group Constructors (Primitives)
   */
  val isClosed: ResultSetIO[Boolean] =
    F.liftF(IsClosed)

  /** 
   * @group Constructors (Primitives)
   */
  val isFirst: ResultSetIO[Boolean] =
    F.liftF(IsFirst)

  /** 
   * @group Constructors (Primitives)
   */
  val isLast: ResultSetIO[Boolean] =
    F.liftF(IsLast)

  /** 
   * @group Constructors (Primitives)
   */
  def isWrapperFor(a: Class[_]): ResultSetIO[Boolean] =
    F.liftF(IsWrapperFor(a))

  /** 
   * @group Constructors (Primitives)
   */
  val last: ResultSetIO[Boolean] =
    F.liftF(Last)

  /** 
   * @group Constructors (Primitives)
   */
  val moveToCurrentRow: ResultSetIO[Unit] =
    F.liftF(MoveToCurrentRow)

  /** 
   * @group Constructors (Primitives)
   */
  val moveToInsertRow: ResultSetIO[Unit] =
    F.liftF(MoveToInsertRow)

  /** 
   * @group Constructors (Primitives)
   */
  val next: ResultSetIO[Boolean] =
    F.liftF(Next)

  /** 
   * @group Constructors (Primitives)
   */
  val previous: ResultSetIO[Boolean] =
    F.liftF(Previous)

  /** 
   * @group Constructors (Primitives)
   */
  val refreshRow: ResultSetIO[Unit] =
    F.liftF(RefreshRow)

  /** 
   * @group Constructors (Primitives)
   */
  def relative(a: Int): ResultSetIO[Boolean] =
    F.liftF(Relative(a))

  /** 
   * @group Constructors (Primitives)
   */
  val rowDeleted: ResultSetIO[Boolean] =
    F.liftF(RowDeleted)

  /** 
   * @group Constructors (Primitives)
   */
  val rowInserted: ResultSetIO[Boolean] =
    F.liftF(RowInserted)

  /** 
   * @group Constructors (Primitives)
   */
  val rowUpdated: ResultSetIO[Boolean] =
    F.liftF(RowUpdated)

  /** 
   * @group Constructors (Primitives)
   */
  def setFetchDirection(a: Int): ResultSetIO[Unit] =
    F.liftF(SetFetchDirection(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setFetchSize(a: Int): ResultSetIO[Unit] =
    F.liftF(SetFetchSize(a))

  /** 
   * @group Constructors (Primitives)
   */
  def unwrap[T](a: Class[T]): ResultSetIO[T] =
    F.liftF(Unwrap(a))

  /** 
   * @group Constructors (Primitives)
   */
  def updateArray(a: String, b: SqlArray): ResultSetIO[Unit] =
    F.liftF(UpdateArray(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateArray(a: Int, b: SqlArray): ResultSetIO[Unit] =
    F.liftF(UpdateArray1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateAsciiStream(a: String, b: InputStream, c: Long): ResultSetIO[Unit] =
    F.liftF(UpdateAsciiStream(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateAsciiStream(a: String, b: InputStream): ResultSetIO[Unit] =
    F.liftF(UpdateAsciiStream1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateAsciiStream(a: String, b: InputStream, c: Int): ResultSetIO[Unit] =
    F.liftF(UpdateAsciiStream2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateAsciiStream(a: Int, b: InputStream, c: Int): ResultSetIO[Unit] =
    F.liftF(UpdateAsciiStream3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateAsciiStream(a: Int, b: InputStream): ResultSetIO[Unit] =
    F.liftF(UpdateAsciiStream4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateAsciiStream(a: Int, b: InputStream, c: Long): ResultSetIO[Unit] =
    F.liftF(UpdateAsciiStream5(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBigDecimal(a: Int, b: BigDecimal): ResultSetIO[Unit] =
    F.liftF(UpdateBigDecimal(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBigDecimal(a: String, b: BigDecimal): ResultSetIO[Unit] =
    F.liftF(UpdateBigDecimal1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBinaryStream(a: Int, b: InputStream, c: Long): ResultSetIO[Unit] =
    F.liftF(UpdateBinaryStream(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBinaryStream(a: String, b: InputStream, c: Int): ResultSetIO[Unit] =
    F.liftF(UpdateBinaryStream1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBinaryStream(a: String, b: InputStream): ResultSetIO[Unit] =
    F.liftF(UpdateBinaryStream2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBinaryStream(a: String, b: InputStream, c: Long): ResultSetIO[Unit] =
    F.liftF(UpdateBinaryStream3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBinaryStream(a: Int, b: InputStream): ResultSetIO[Unit] =
    F.liftF(UpdateBinaryStream4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBinaryStream(a: Int, b: InputStream, c: Int): ResultSetIO[Unit] =
    F.liftF(UpdateBinaryStream5(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBlob(a: Int, b: Blob): ResultSetIO[Unit] =
    F.liftF(UpdateBlob(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBlob(a: Int, b: InputStream, c: Long): ResultSetIO[Unit] =
    F.liftF(UpdateBlob1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBlob(a: Int, b: InputStream): ResultSetIO[Unit] =
    F.liftF(UpdateBlob2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBlob(a: String, b: InputStream, c: Long): ResultSetIO[Unit] =
    F.liftF(UpdateBlob3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBlob(a: String, b: InputStream): ResultSetIO[Unit] =
    F.liftF(UpdateBlob4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBlob(a: String, b: Blob): ResultSetIO[Unit] =
    F.liftF(UpdateBlob5(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBoolean(a: Int, b: Boolean): ResultSetIO[Unit] =
    F.liftF(UpdateBoolean(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBoolean(a: String, b: Boolean): ResultSetIO[Unit] =
    F.liftF(UpdateBoolean1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateByte(a: String, b: Byte): ResultSetIO[Unit] =
    F.liftF(UpdateByte(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateByte(a: Int, b: Byte): ResultSetIO[Unit] =
    F.liftF(UpdateByte1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBytes(a: String, b: Array[Byte]): ResultSetIO[Unit] =
    F.liftF(UpdateBytes(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBytes(a: Int, b: Array[Byte]): ResultSetIO[Unit] =
    F.liftF(UpdateBytes1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateCharacterStream(a: String, b: Reader, c: Int): ResultSetIO[Unit] =
    F.liftF(UpdateCharacterStream(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateCharacterStream(a: Int, b: Reader, c: Int): ResultSetIO[Unit] =
    F.liftF(UpdateCharacterStream1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateCharacterStream(a: Int, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftF(UpdateCharacterStream2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateCharacterStream(a: String, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftF(UpdateCharacterStream3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateCharacterStream(a: Int, b: Reader): ResultSetIO[Unit] =
    F.liftF(UpdateCharacterStream4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateCharacterStream(a: String, b: Reader): ResultSetIO[Unit] =
    F.liftF(UpdateCharacterStream5(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateClob(a: Int, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftF(UpdateClob(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateClob(a: Int, b: Clob): ResultSetIO[Unit] =
    F.liftF(UpdateClob1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateClob(a: String, b: Clob): ResultSetIO[Unit] =
    F.liftF(UpdateClob2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateClob(a: Int, b: Reader): ResultSetIO[Unit] =
    F.liftF(UpdateClob3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateClob(a: String, b: Reader): ResultSetIO[Unit] =
    F.liftF(UpdateClob4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateClob(a: String, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftF(UpdateClob5(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateDate(a: String, b: Date): ResultSetIO[Unit] =
    F.liftF(UpdateDate(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateDate(a: Int, b: Date): ResultSetIO[Unit] =
    F.liftF(UpdateDate1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateDouble(a: String, b: Double): ResultSetIO[Unit] =
    F.liftF(UpdateDouble(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateDouble(a: Int, b: Double): ResultSetIO[Unit] =
    F.liftF(UpdateDouble1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateFloat(a: String, b: Float): ResultSetIO[Unit] =
    F.liftF(UpdateFloat(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateFloat(a: Int, b: Float): ResultSetIO[Unit] =
    F.liftF(UpdateFloat1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateInt(a: Int, b: Int): ResultSetIO[Unit] =
    F.liftF(UpdateInt(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateInt(a: String, b: Int): ResultSetIO[Unit] =
    F.liftF(UpdateInt1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateLong(a: Int, b: Long): ResultSetIO[Unit] =
    F.liftF(UpdateLong(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateLong(a: String, b: Long): ResultSetIO[Unit] =
    F.liftF(UpdateLong1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNCharacterStream(a: Int, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftF(UpdateNCharacterStream(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNCharacterStream(a: String, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftF(UpdateNCharacterStream1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNCharacterStream(a: Int, b: Reader): ResultSetIO[Unit] =
    F.liftF(UpdateNCharacterStream2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNCharacterStream(a: String, b: Reader): ResultSetIO[Unit] =
    F.liftF(UpdateNCharacterStream3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNClob(a: Int, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftF(UpdateNClob(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNClob(a: String, b: Reader): ResultSetIO[Unit] =
    F.liftF(UpdateNClob1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNClob(a: String, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftF(UpdateNClob2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNClob(a: Int, b: Reader): ResultSetIO[Unit] =
    F.liftF(UpdateNClob3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNClob(a: String, b: NClob): ResultSetIO[Unit] =
    F.liftF(UpdateNClob4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNClob(a: Int, b: NClob): ResultSetIO[Unit] =
    F.liftF(UpdateNClob5(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNString(a: Int, b: String): ResultSetIO[Unit] =
    F.liftF(UpdateNString(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNString(a: String, b: String): ResultSetIO[Unit] =
    F.liftF(UpdateNString1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNull(a: String): ResultSetIO[Unit] =
    F.liftF(UpdateNull(a))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNull(a: Int): ResultSetIO[Unit] =
    F.liftF(UpdateNull1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def updateObject(a: String, b: Object): ResultSetIO[Unit] =
    F.liftF(UpdateObject(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateObject(a: String, b: Object, c: Int): ResultSetIO[Unit] =
    F.liftF(UpdateObject1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateObject(a: Int, b: Object, c: Int): ResultSetIO[Unit] =
    F.liftF(UpdateObject2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateObject(a: Int, b: Object): ResultSetIO[Unit] =
    F.liftF(UpdateObject3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateRef(a: String, b: Ref): ResultSetIO[Unit] =
    F.liftF(UpdateRef(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateRef(a: Int, b: Ref): ResultSetIO[Unit] =
    F.liftF(UpdateRef1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val updateRow: ResultSetIO[Unit] =
    F.liftF(UpdateRow)

  /** 
   * @group Constructors (Primitives)
   */
  def updateRowId(a: Int, b: RowId): ResultSetIO[Unit] =
    F.liftF(UpdateRowId(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateRowId(a: String, b: RowId): ResultSetIO[Unit] =
    F.liftF(UpdateRowId1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateSQLXML(a: Int, b: SQLXML): ResultSetIO[Unit] =
    F.liftF(UpdateSQLXML(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateSQLXML(a: String, b: SQLXML): ResultSetIO[Unit] =
    F.liftF(UpdateSQLXML1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateShort(a: String, b: Short): ResultSetIO[Unit] =
    F.liftF(UpdateShort(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateShort(a: Int, b: Short): ResultSetIO[Unit] =
    F.liftF(UpdateShort1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateString(a: String, b: String): ResultSetIO[Unit] =
    F.liftF(UpdateString(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateString(a: Int, b: String): ResultSetIO[Unit] =
    F.liftF(UpdateString1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateTime(a: String, b: Time): ResultSetIO[Unit] =
    F.liftF(UpdateTime(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateTime(a: Int, b: Time): ResultSetIO[Unit] =
    F.liftF(UpdateTime1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateTimestamp(a: Int, b: Timestamp): ResultSetIO[Unit] =
    F.liftF(UpdateTimestamp(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateTimestamp(a: String, b: Timestamp): ResultSetIO[Unit] =
    F.liftF(UpdateTimestamp1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val wasNull: ResultSetIO[Boolean] =
    F.liftF(WasNull)

 /** 
  * Natural transformation from `ResultSetOp` to `Kleisli` for the given `M`, consuming a `java.sql.ResultSet`. 
  * @group Algebra
  */
  def interpK[M[_]: Monad: Catchable: Capture]: ResultSetOp ~> Kleisli[M, ResultSet, ?] =
   ResultSetOp.ResultSetKleisliTrans.interpK

 /** 
  * Natural transformation from `ResultSetIO` to `Kleisli` for the given `M`, consuming a `java.sql.ResultSet`. 
  * @group Algebra
  */
  def transK[M[_]: Monad: Catchable: Capture]: ResultSetIO ~> Kleisli[M, ResultSet, ?] =
   ResultSetOp.ResultSetKleisliTrans.transK

 /** 
  * Natural transformation from `ResultSetIO` to `M`, given a `java.sql.ResultSet`. 
  * @group Algebra
  */
 def trans[M[_]: Monad: Catchable: Capture](c: ResultSet): ResultSetIO ~> M =
   ResultSetOp.ResultSetKleisliTrans.trans[M](c)

  /**
   * Syntax for `ResultSetIO`.
   * @group Algebra
   */
  implicit class ResultSetIOOps[A](ma: ResultSetIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, ResultSet, A] =
      ResultSetOp.ResultSetKleisliTrans.transK[M].apply(ma)
  }

}

