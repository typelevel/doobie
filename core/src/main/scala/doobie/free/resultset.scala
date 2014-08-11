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
 * `Free.runFC`. 
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
  sealed trait ResultSetOp[A]

  /** 
   * Module of constructors for `ResultSetOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `resultset` module.
   * @group Algebra 
   */
  object ResultSetOp {
    
    // Lifting
    case class LiftBlobIO[A](s: Blob, action: BlobIO[A]) extends ResultSetOp[A]
    case class LiftCallableStatementIO[A](s: CallableStatement, action: CallableStatementIO[A]) extends ResultSetOp[A]
    case class LiftClobIO[A](s: Clob, action: ClobIO[A]) extends ResultSetOp[A]
    case class LiftConnectionIO[A](s: Connection, action: ConnectionIO[A]) extends ResultSetOp[A]
    case class LiftDatabaseMetaDataIO[A](s: DatabaseMetaData, action: DatabaseMetaDataIO[A]) extends ResultSetOp[A]
    case class LiftDriverIO[A](s: Driver, action: DriverIO[A]) extends ResultSetOp[A]
    case class LiftNClobIO[A](s: NClob, action: NClobIO[A]) extends ResultSetOp[A]
    case class LiftPreparedStatementIO[A](s: PreparedStatement, action: PreparedStatementIO[A]) extends ResultSetOp[A]
    case class LiftRefIO[A](s: Ref, action: RefIO[A]) extends ResultSetOp[A]
    case class LiftSQLDataIO[A](s: SQLData, action: SQLDataIO[A]) extends ResultSetOp[A]
    case class LiftSQLInputIO[A](s: SQLInput, action: SQLInputIO[A]) extends ResultSetOp[A]
    case class LiftSQLOutputIO[A](s: SQLOutput, action: SQLOutputIO[A]) extends ResultSetOp[A]
    case class LiftStatementIO[A](s: Statement, action: StatementIO[A]) extends ResultSetOp[A]

    // Combinators
    case class Attempt[A](action: ResultSetIO[A]) extends ResultSetOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends ResultSetOp[A]

    // Primitive Operations
    case class  Absolute(a: Int) extends ResultSetOp[Boolean]
    case object AfterLast extends ResultSetOp[Unit]
    case object BeforeFirst extends ResultSetOp[Unit]
    case object CancelRowUpdates extends ResultSetOp[Unit]
    case object ClearWarnings extends ResultSetOp[Unit]
    case object Close extends ResultSetOp[Unit]
    case object DeleteRow extends ResultSetOp[Unit]
    case class  FindColumn(a: String) extends ResultSetOp[Int]
    case object First extends ResultSetOp[Boolean]
    case class  GetArray(a: String) extends ResultSetOp[SqlArray]
    case class  GetArray1(a: Int) extends ResultSetOp[SqlArray]
    case class  GetAsciiStream(a: Int) extends ResultSetOp[InputStream]
    case class  GetAsciiStream1(a: String) extends ResultSetOp[InputStream]
    case class  GetBigDecimal(a: String) extends ResultSetOp[BigDecimal]
    case class  GetBigDecimal1(a: Int) extends ResultSetOp[BigDecimal]
    case class  GetBigDecimal2(a: String, b: Int) extends ResultSetOp[BigDecimal]
    case class  GetBigDecimal3(a: Int, b: Int) extends ResultSetOp[BigDecimal]
    case class  GetBinaryStream(a: String) extends ResultSetOp[InputStream]
    case class  GetBinaryStream1(a: Int) extends ResultSetOp[InputStream]
    case class  GetBlob(a: String) extends ResultSetOp[Blob]
    case class  GetBlob1(a: Int) extends ResultSetOp[Blob]
    case class  GetBoolean(a: Int) extends ResultSetOp[Boolean]
    case class  GetBoolean1(a: String) extends ResultSetOp[Boolean]
    case class  GetByte(a: String) extends ResultSetOp[Byte]
    case class  GetByte1(a: Int) extends ResultSetOp[Byte]
    case class  GetBytes(a: String) extends ResultSetOp[Array[Byte]]
    case class  GetBytes1(a: Int) extends ResultSetOp[Array[Byte]]
    case class  GetCharacterStream(a: String) extends ResultSetOp[Reader]
    case class  GetCharacterStream1(a: Int) extends ResultSetOp[Reader]
    case class  GetClob(a: Int) extends ResultSetOp[Clob]
    case class  GetClob1(a: String) extends ResultSetOp[Clob]
    case object GetConcurrency extends ResultSetOp[Int]
    case object GetCursorName extends ResultSetOp[String]
    case class  GetDate(a: Int, b: Calendar) extends ResultSetOp[Date]
    case class  GetDate1(a: String) extends ResultSetOp[Date]
    case class  GetDate2(a: String, b: Calendar) extends ResultSetOp[Date]
    case class  GetDate3(a: Int) extends ResultSetOp[Date]
    case class  GetDouble(a: Int) extends ResultSetOp[Double]
    case class  GetDouble1(a: String) extends ResultSetOp[Double]
    case object GetFetchDirection extends ResultSetOp[Int]
    case object GetFetchSize extends ResultSetOp[Int]
    case class  GetFloat(a: String) extends ResultSetOp[Float]
    case class  GetFloat1(a: Int) extends ResultSetOp[Float]
    case object GetHoldability extends ResultSetOp[Int]
    case class  GetInt(a: String) extends ResultSetOp[Int]
    case class  GetInt1(a: Int) extends ResultSetOp[Int]
    case class  GetLong(a: String) extends ResultSetOp[Long]
    case class  GetLong1(a: Int) extends ResultSetOp[Long]
    case object GetMetaData extends ResultSetOp[ResultSetMetaData]
    case class  GetNCharacterStream(a: String) extends ResultSetOp[Reader]
    case class  GetNCharacterStream1(a: Int) extends ResultSetOp[Reader]
    case class  GetNClob(a: Int) extends ResultSetOp[NClob]
    case class  GetNClob1(a: String) extends ResultSetOp[NClob]
    case class  GetNString(a: String) extends ResultSetOp[String]
    case class  GetNString1(a: Int) extends ResultSetOp[String]
    case class  GetObject(a: Int, b: Map[String, Class[_]]) extends ResultSetOp[Object]
    case class  GetObject1(a: String, b: Map[String, Class[_]]) extends ResultSetOp[Object]
    case class  GetObject2(a: String) extends ResultSetOp[Object]
    case class  GetObject3(a: Int) extends ResultSetOp[Object]
    case class  GetRef(a: String) extends ResultSetOp[Ref]
    case class  GetRef1(a: Int) extends ResultSetOp[Ref]
    case object GetRow extends ResultSetOp[Int]
    case class  GetRowId(a: Int) extends ResultSetOp[RowId]
    case class  GetRowId1(a: String) extends ResultSetOp[RowId]
    case class  GetSQLXML(a: Int) extends ResultSetOp[SQLXML]
    case class  GetSQLXML1(a: String) extends ResultSetOp[SQLXML]
    case class  GetShort(a: String) extends ResultSetOp[Short]
    case class  GetShort1(a: Int) extends ResultSetOp[Short]
    case object GetStatement extends ResultSetOp[Statement]
    case class  GetString(a: String) extends ResultSetOp[String]
    case class  GetString1(a: Int) extends ResultSetOp[String]
    case class  GetTime(a: String, b: Calendar) extends ResultSetOp[Time]
    case class  GetTime1(a: String) extends ResultSetOp[Time]
    case class  GetTime2(a: Int) extends ResultSetOp[Time]
    case class  GetTime3(a: Int, b: Calendar) extends ResultSetOp[Time]
    case class  GetTimestamp(a: Int) extends ResultSetOp[Timestamp]
    case class  GetTimestamp1(a: Int, b: Calendar) extends ResultSetOp[Timestamp]
    case class  GetTimestamp2(a: String, b: Calendar) extends ResultSetOp[Timestamp]
    case class  GetTimestamp3(a: String) extends ResultSetOp[Timestamp]
    case object GetType extends ResultSetOp[Int]
    case class  GetURL(a: Int) extends ResultSetOp[URL]
    case class  GetURL1(a: String) extends ResultSetOp[URL]
    case class  GetUnicodeStream(a: String) extends ResultSetOp[InputStream]
    case class  GetUnicodeStream1(a: Int) extends ResultSetOp[InputStream]
    case object GetWarnings extends ResultSetOp[SQLWarning]
    case object InsertRow extends ResultSetOp[Unit]
    case object IsAfterLast extends ResultSetOp[Boolean]
    case object IsBeforeFirst extends ResultSetOp[Boolean]
    case object IsClosed extends ResultSetOp[Boolean]
    case object IsFirst extends ResultSetOp[Boolean]
    case object IsLast extends ResultSetOp[Boolean]
    case class  IsWrapperFor(a: Class[_]) extends ResultSetOp[Boolean]
    case object Last extends ResultSetOp[Boolean]
    case object MoveToCurrentRow extends ResultSetOp[Unit]
    case object MoveToInsertRow extends ResultSetOp[Unit]
    case object Next extends ResultSetOp[Boolean]
    case object Previous extends ResultSetOp[Boolean]
    case object RefreshRow extends ResultSetOp[Unit]
    case class  Relative(a: Int) extends ResultSetOp[Boolean]
    case object RowDeleted extends ResultSetOp[Boolean]
    case object RowInserted extends ResultSetOp[Boolean]
    case object RowUpdated extends ResultSetOp[Boolean]
    case class  SetFetchDirection(a: Int) extends ResultSetOp[Unit]
    case class  SetFetchSize(a: Int) extends ResultSetOp[Unit]
    case class  Unwrap[T](a: Class[T]) extends ResultSetOp[T]
    case class  UpdateArray(a: Int, b: SqlArray) extends ResultSetOp[Unit]
    case class  UpdateArray1(a: String, b: SqlArray) extends ResultSetOp[Unit]
    case class  UpdateAsciiStream(a: String, b: InputStream) extends ResultSetOp[Unit]
    case class  UpdateAsciiStream1(a: Int, b: InputStream, c: Long) extends ResultSetOp[Unit]
    case class  UpdateAsciiStream2(a: Int, b: InputStream, c: Int) extends ResultSetOp[Unit]
    case class  UpdateAsciiStream3(a: Int, b: InputStream) extends ResultSetOp[Unit]
    case class  UpdateAsciiStream4(a: String, b: InputStream, c: Long) extends ResultSetOp[Unit]
    case class  UpdateAsciiStream5(a: String, b: InputStream, c: Int) extends ResultSetOp[Unit]
    case class  UpdateBigDecimal(a: Int, b: BigDecimal) extends ResultSetOp[Unit]
    case class  UpdateBigDecimal1(a: String, b: BigDecimal) extends ResultSetOp[Unit]
    case class  UpdateBinaryStream(a: String, b: InputStream, c: Long) extends ResultSetOp[Unit]
    case class  UpdateBinaryStream1(a: String, b: InputStream, c: Int) extends ResultSetOp[Unit]
    case class  UpdateBinaryStream2(a: Int, b: InputStream, c: Long) extends ResultSetOp[Unit]
    case class  UpdateBinaryStream3(a: Int, b: InputStream) extends ResultSetOp[Unit]
    case class  UpdateBinaryStream4(a: String, b: InputStream) extends ResultSetOp[Unit]
    case class  UpdateBinaryStream5(a: Int, b: InputStream, c: Int) extends ResultSetOp[Unit]
    case class  UpdateBlob(a: String, b: InputStream) extends ResultSetOp[Unit]
    case class  UpdateBlob1(a: Int, b: InputStream) extends ResultSetOp[Unit]
    case class  UpdateBlob2(a: String, b: Blob) extends ResultSetOp[Unit]
    case class  UpdateBlob3(a: Int, b: Blob) extends ResultSetOp[Unit]
    case class  UpdateBlob4(a: Int, b: InputStream, c: Long) extends ResultSetOp[Unit]
    case class  UpdateBlob5(a: String, b: InputStream, c: Long) extends ResultSetOp[Unit]
    case class  UpdateBoolean(a: Int, b: Boolean) extends ResultSetOp[Unit]
    case class  UpdateBoolean1(a: String, b: Boolean) extends ResultSetOp[Unit]
    case class  UpdateByte(a: String, b: Byte) extends ResultSetOp[Unit]
    case class  UpdateByte1(a: Int, b: Byte) extends ResultSetOp[Unit]
    case class  UpdateBytes(a: String, b: Array[Byte]) extends ResultSetOp[Unit]
    case class  UpdateBytes1(a: Int, b: Array[Byte]) extends ResultSetOp[Unit]
    case class  UpdateCharacterStream(a: Int, b: Reader, c: Long) extends ResultSetOp[Unit]
    case class  UpdateCharacterStream1(a: Int, b: Reader, c: Int) extends ResultSetOp[Unit]
    case class  UpdateCharacterStream2(a: String, b: Reader, c: Long) extends ResultSetOp[Unit]
    case class  UpdateCharacterStream3(a: String, b: Reader) extends ResultSetOp[Unit]
    case class  UpdateCharacterStream4(a: Int, b: Reader) extends ResultSetOp[Unit]
    case class  UpdateCharacterStream5(a: String, b: Reader, c: Int) extends ResultSetOp[Unit]
    case class  UpdateClob(a: String, b: Reader) extends ResultSetOp[Unit]
    case class  UpdateClob1(a: String, b: Clob) extends ResultSetOp[Unit]
    case class  UpdateClob2(a: Int, b: Clob) extends ResultSetOp[Unit]
    case class  UpdateClob3(a: Int, b: Reader) extends ResultSetOp[Unit]
    case class  UpdateClob4(a: Int, b: Reader, c: Long) extends ResultSetOp[Unit]
    case class  UpdateClob5(a: String, b: Reader, c: Long) extends ResultSetOp[Unit]
    case class  UpdateDate(a: Int, b: Date) extends ResultSetOp[Unit]
    case class  UpdateDate1(a: String, b: Date) extends ResultSetOp[Unit]
    case class  UpdateDouble(a: Int, b: Double) extends ResultSetOp[Unit]
    case class  UpdateDouble1(a: String, b: Double) extends ResultSetOp[Unit]
    case class  UpdateFloat(a: String, b: Float) extends ResultSetOp[Unit]
    case class  UpdateFloat1(a: Int, b: Float) extends ResultSetOp[Unit]
    case class  UpdateInt(a: Int, b: Int) extends ResultSetOp[Unit]
    case class  UpdateInt1(a: String, b: Int) extends ResultSetOp[Unit]
    case class  UpdateLong(a: Int, b: Long) extends ResultSetOp[Unit]
    case class  UpdateLong1(a: String, b: Long) extends ResultSetOp[Unit]
    case class  UpdateNCharacterStream(a: String, b: Reader, c: Long) extends ResultSetOp[Unit]
    case class  UpdateNCharacterStream1(a: Int, b: Reader, c: Long) extends ResultSetOp[Unit]
    case class  UpdateNCharacterStream2(a: String, b: Reader) extends ResultSetOp[Unit]
    case class  UpdateNCharacterStream3(a: Int, b: Reader) extends ResultSetOp[Unit]
    case class  UpdateNClob(a: String, b: Reader) extends ResultSetOp[Unit]
    case class  UpdateNClob1(a: Int, b: NClob) extends ResultSetOp[Unit]
    case class  UpdateNClob2(a: Int, b: Reader, c: Long) extends ResultSetOp[Unit]
    case class  UpdateNClob3(a: String, b: Reader, c: Long) extends ResultSetOp[Unit]
    case class  UpdateNClob4(a: Int, b: Reader) extends ResultSetOp[Unit]
    case class  UpdateNClob5(a: String, b: NClob) extends ResultSetOp[Unit]
    case class  UpdateNString(a: String, b: String) extends ResultSetOp[Unit]
    case class  UpdateNString1(a: Int, b: String) extends ResultSetOp[Unit]
    case class  UpdateNull(a: Int) extends ResultSetOp[Unit]
    case class  UpdateNull1(a: String) extends ResultSetOp[Unit]
    case class  UpdateObject(a: String, b: Object, c: Int) extends ResultSetOp[Unit]
    case class  UpdateObject1(a: String, b: Object) extends ResultSetOp[Unit]
    case class  UpdateObject2(a: Int, b: Object) extends ResultSetOp[Unit]
    case class  UpdateObject3(a: Int, b: Object, c: Int) extends ResultSetOp[Unit]
    case class  UpdateRef(a: Int, b: Ref) extends ResultSetOp[Unit]
    case class  UpdateRef1(a: String, b: Ref) extends ResultSetOp[Unit]
    case object UpdateRow extends ResultSetOp[Unit]
    case class  UpdateRowId(a: Int, b: RowId) extends ResultSetOp[Unit]
    case class  UpdateRowId1(a: String, b: RowId) extends ResultSetOp[Unit]
    case class  UpdateSQLXML(a: String, b: SQLXML) extends ResultSetOp[Unit]
    case class  UpdateSQLXML1(a: Int, b: SQLXML) extends ResultSetOp[Unit]
    case class  UpdateShort(a: Int, b: Short) extends ResultSetOp[Unit]
    case class  UpdateShort1(a: String, b: Short) extends ResultSetOp[Unit]
    case class  UpdateString(a: Int, b: String) extends ResultSetOp[Unit]
    case class  UpdateString1(a: String, b: String) extends ResultSetOp[Unit]
    case class  UpdateTime(a: Int, b: Time) extends ResultSetOp[Unit]
    case class  UpdateTime1(a: String, b: Time) extends ResultSetOp[Unit]
    case class  UpdateTimestamp(a: String, b: Timestamp) extends ResultSetOp[Unit]
    case class  UpdateTimestamp1(a: Int, b: Timestamp) extends ResultSetOp[Unit]
    case object WasNull extends ResultSetOp[Boolean]

  }
  import ResultSetOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[ResultSetOp]]; abstractly, a computation that consumes 
   * a `java.sql.ResultSet` and produces a value of type `A`. 
   * @group Algebra 
   */
  type ResultSetIO[A] = F.FreeC[ResultSetOp, A]

  /**
   * Monad instance for [[ResultSetIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadResultSetIO: Monad[ResultSetIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[ResultSetOp, α]})#λ]

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
   * @group Constructors (Lifting)
   */
  def liftBlob[A](s: Blob, k: BlobIO[A]): ResultSetIO[A] =
    F.liftFC(LiftBlobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftCallableStatement[A](s: CallableStatement, k: CallableStatementIO[A]): ResultSetIO[A] =
    F.liftFC(LiftCallableStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftClob[A](s: Clob, k: ClobIO[A]): ResultSetIO[A] =
    F.liftFC(LiftClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftConnection[A](s: Connection, k: ConnectionIO[A]): ResultSetIO[A] =
    F.liftFC(LiftConnectionIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDatabaseMetaData[A](s: DatabaseMetaData, k: DatabaseMetaDataIO[A]): ResultSetIO[A] =
    F.liftFC(LiftDatabaseMetaDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDriver[A](s: Driver, k: DriverIO[A]): ResultSetIO[A] =
    F.liftFC(LiftDriverIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftNClob[A](s: NClob, k: NClobIO[A]): ResultSetIO[A] =
    F.liftFC(LiftNClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftPreparedStatement[A](s: PreparedStatement, k: PreparedStatementIO[A]): ResultSetIO[A] =
    F.liftFC(LiftPreparedStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftRef[A](s: Ref, k: RefIO[A]): ResultSetIO[A] =
    F.liftFC(LiftRefIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLData[A](s: SQLData, k: SQLDataIO[A]): ResultSetIO[A] =
    F.liftFC(LiftSQLDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLInput[A](s: SQLInput, k: SQLInputIO[A]): ResultSetIO[A] =
    F.liftFC(LiftSQLInputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLOutput[A](s: SQLOutput, k: SQLOutputIO[A]): ResultSetIO[A] =
    F.liftFC(LiftSQLOutputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftStatement[A](s: Statement, k: StatementIO[A]): ResultSetIO[A] =
    F.liftFC(LiftStatementIO(s, k))

  /** 
   * Lift a ResultSetIO[A] into an exception-capturing ResultSetIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: ResultSetIO[A]): ResultSetIO[Throwable \/ A] =
    F.liftFC[ResultSetOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): ResultSetIO[A] =
    F.liftFC(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  def absolute(a: Int): ResultSetIO[Boolean] =
    F.liftFC(Absolute(a))

  /** 
   * @group Constructors (Primitives)
   */
  val afterLast: ResultSetIO[Unit] =
    F.liftFC(AfterLast)

  /** 
   * @group Constructors (Primitives)
   */
  val beforeFirst: ResultSetIO[Unit] =
    F.liftFC(BeforeFirst)

  /** 
   * @group Constructors (Primitives)
   */
  val cancelRowUpdates: ResultSetIO[Unit] =
    F.liftFC(CancelRowUpdates)

  /** 
   * @group Constructors (Primitives)
   */
  val clearWarnings: ResultSetIO[Unit] =
    F.liftFC(ClearWarnings)

  /** 
   * @group Constructors (Primitives)
   */
  val close: ResultSetIO[Unit] =
    F.liftFC(Close)

  /** 
   * @group Constructors (Primitives)
   */
  val deleteRow: ResultSetIO[Unit] =
    F.liftFC(DeleteRow)

  /** 
   * @group Constructors (Primitives)
   */
  def findColumn(a: String): ResultSetIO[Int] =
    F.liftFC(FindColumn(a))

  /** 
   * @group Constructors (Primitives)
   */
  val first: ResultSetIO[Boolean] =
    F.liftFC(First)

  /** 
   * @group Constructors (Primitives)
   */
  def getArray(a: String): ResultSetIO[SqlArray] =
    F.liftFC(GetArray(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getArray(a: Int): ResultSetIO[SqlArray] =
    F.liftFC(GetArray1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getAsciiStream(a: Int): ResultSetIO[InputStream] =
    F.liftFC(GetAsciiStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getAsciiStream(a: String): ResultSetIO[InputStream] =
    F.liftFC(GetAsciiStream1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBigDecimal(a: String): ResultSetIO[BigDecimal] =
    F.liftFC(GetBigDecimal(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBigDecimal(a: Int): ResultSetIO[BigDecimal] =
    F.liftFC(GetBigDecimal1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBigDecimal(a: String, b: Int): ResultSetIO[BigDecimal] =
    F.liftFC(GetBigDecimal2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getBigDecimal(a: Int, b: Int): ResultSetIO[BigDecimal] =
    F.liftFC(GetBigDecimal3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getBinaryStream(a: String): ResultSetIO[InputStream] =
    F.liftFC(GetBinaryStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBinaryStream(a: Int): ResultSetIO[InputStream] =
    F.liftFC(GetBinaryStream1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBlob(a: String): ResultSetIO[Blob] =
    F.liftFC(GetBlob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBlob(a: Int): ResultSetIO[Blob] =
    F.liftFC(GetBlob1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBoolean(a: Int): ResultSetIO[Boolean] =
    F.liftFC(GetBoolean(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBoolean(a: String): ResultSetIO[Boolean] =
    F.liftFC(GetBoolean1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getByte(a: String): ResultSetIO[Byte] =
    F.liftFC(GetByte(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getByte(a: Int): ResultSetIO[Byte] =
    F.liftFC(GetByte1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBytes(a: String): ResultSetIO[Array[Byte]] =
    F.liftFC(GetBytes(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getBytes(a: Int): ResultSetIO[Array[Byte]] =
    F.liftFC(GetBytes1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getCharacterStream(a: String): ResultSetIO[Reader] =
    F.liftFC(GetCharacterStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getCharacterStream(a: Int): ResultSetIO[Reader] =
    F.liftFC(GetCharacterStream1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getClob(a: Int): ResultSetIO[Clob] =
    F.liftFC(GetClob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getClob(a: String): ResultSetIO[Clob] =
    F.liftFC(GetClob1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getConcurrency: ResultSetIO[Int] =
    F.liftFC(GetConcurrency)

  /** 
   * @group Constructors (Primitives)
   */
  val getCursorName: ResultSetIO[String] =
    F.liftFC(GetCursorName)

  /** 
   * @group Constructors (Primitives)
   */
  def getDate(a: Int, b: Calendar): ResultSetIO[Date] =
    F.liftFC(GetDate(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getDate(a: String): ResultSetIO[Date] =
    F.liftFC(GetDate1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getDate(a: String, b: Calendar): ResultSetIO[Date] =
    F.liftFC(GetDate2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getDate(a: Int): ResultSetIO[Date] =
    F.liftFC(GetDate3(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getDouble(a: Int): ResultSetIO[Double] =
    F.liftFC(GetDouble(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getDouble(a: String): ResultSetIO[Double] =
    F.liftFC(GetDouble1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getFetchDirection: ResultSetIO[Int] =
    F.liftFC(GetFetchDirection)

  /** 
   * @group Constructors (Primitives)
   */
  val getFetchSize: ResultSetIO[Int] =
    F.liftFC(GetFetchSize)

  /** 
   * @group Constructors (Primitives)
   */
  def getFloat(a: String): ResultSetIO[Float] =
    F.liftFC(GetFloat(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getFloat(a: Int): ResultSetIO[Float] =
    F.liftFC(GetFloat1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getHoldability: ResultSetIO[Int] =
    F.liftFC(GetHoldability)

  /** 
   * @group Constructors (Primitives)
   */
  def getInt(a: String): ResultSetIO[Int] =
    F.liftFC(GetInt(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getInt(a: Int): ResultSetIO[Int] =
    F.liftFC(GetInt1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getLong(a: String): ResultSetIO[Long] =
    F.liftFC(GetLong(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getLong(a: Int): ResultSetIO[Long] =
    F.liftFC(GetLong1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getMetaData: ResultSetIO[ResultSetMetaData] =
    F.liftFC(GetMetaData)

  /** 
   * @group Constructors (Primitives)
   */
  def getNCharacterStream(a: String): ResultSetIO[Reader] =
    F.liftFC(GetNCharacterStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNCharacterStream(a: Int): ResultSetIO[Reader] =
    F.liftFC(GetNCharacterStream1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNClob(a: Int): ResultSetIO[NClob] =
    F.liftFC(GetNClob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNClob(a: String): ResultSetIO[NClob] =
    F.liftFC(GetNClob1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNString(a: String): ResultSetIO[String] =
    F.liftFC(GetNString(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getNString(a: Int): ResultSetIO[String] =
    F.liftFC(GetNString1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject(a: Int, b: Map[String, Class[_]]): ResultSetIO[Object] =
    F.liftFC(GetObject(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject(a: String, b: Map[String, Class[_]]): ResultSetIO[Object] =
    F.liftFC(GetObject1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject(a: String): ResultSetIO[Object] =
    F.liftFC(GetObject2(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getObject(a: Int): ResultSetIO[Object] =
    F.liftFC(GetObject3(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getRef(a: String): ResultSetIO[Ref] =
    F.liftFC(GetRef(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getRef(a: Int): ResultSetIO[Ref] =
    F.liftFC(GetRef1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getRow: ResultSetIO[Int] =
    F.liftFC(GetRow)

  /** 
   * @group Constructors (Primitives)
   */
  def getRowId(a: Int): ResultSetIO[RowId] =
    F.liftFC(GetRowId(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getRowId(a: String): ResultSetIO[RowId] =
    F.liftFC(GetRowId1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getSQLXML(a: Int): ResultSetIO[SQLXML] =
    F.liftFC(GetSQLXML(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getSQLXML(a: String): ResultSetIO[SQLXML] =
    F.liftFC(GetSQLXML1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getShort(a: String): ResultSetIO[Short] =
    F.liftFC(GetShort(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getShort(a: Int): ResultSetIO[Short] =
    F.liftFC(GetShort1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getStatement: ResultSetIO[Statement] =
    F.liftFC(GetStatement)

  /** 
   * @group Constructors (Primitives)
   */
  def getString(a: String): ResultSetIO[String] =
    F.liftFC(GetString(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getString(a: Int): ResultSetIO[String] =
    F.liftFC(GetString1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getTime(a: String, b: Calendar): ResultSetIO[Time] =
    F.liftFC(GetTime(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getTime(a: String): ResultSetIO[Time] =
    F.liftFC(GetTime1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getTime(a: Int): ResultSetIO[Time] =
    F.liftFC(GetTime2(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getTime(a: Int, b: Calendar): ResultSetIO[Time] =
    F.liftFC(GetTime3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getTimestamp(a: Int): ResultSetIO[Timestamp] =
    F.liftFC(GetTimestamp(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getTimestamp(a: Int, b: Calendar): ResultSetIO[Timestamp] =
    F.liftFC(GetTimestamp1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getTimestamp(a: String, b: Calendar): ResultSetIO[Timestamp] =
    F.liftFC(GetTimestamp2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getTimestamp(a: String): ResultSetIO[Timestamp] =
    F.liftFC(GetTimestamp3(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getType: ResultSetIO[Int] =
    F.liftFC(GetType)

  /** 
   * @group Constructors (Primitives)
   */
  def getURL(a: Int): ResultSetIO[URL] =
    F.liftFC(GetURL(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getURL(a: String): ResultSetIO[URL] =
    F.liftFC(GetURL1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getUnicodeStream(a: String): ResultSetIO[InputStream] =
    F.liftFC(GetUnicodeStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getUnicodeStream(a: Int): ResultSetIO[InputStream] =
    F.liftFC(GetUnicodeStream1(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getWarnings: ResultSetIO[SQLWarning] =
    F.liftFC(GetWarnings)

  /** 
   * @group Constructors (Primitives)
   */
  val insertRow: ResultSetIO[Unit] =
    F.liftFC(InsertRow)

  /** 
   * @group Constructors (Primitives)
   */
  val isAfterLast: ResultSetIO[Boolean] =
    F.liftFC(IsAfterLast)

  /** 
   * @group Constructors (Primitives)
   */
  val isBeforeFirst: ResultSetIO[Boolean] =
    F.liftFC(IsBeforeFirst)

  /** 
   * @group Constructors (Primitives)
   */
  val isClosed: ResultSetIO[Boolean] =
    F.liftFC(IsClosed)

  /** 
   * @group Constructors (Primitives)
   */
  val isFirst: ResultSetIO[Boolean] =
    F.liftFC(IsFirst)

  /** 
   * @group Constructors (Primitives)
   */
  val isLast: ResultSetIO[Boolean] =
    F.liftFC(IsLast)

  /** 
   * @group Constructors (Primitives)
   */
  def isWrapperFor(a: Class[_]): ResultSetIO[Boolean] =
    F.liftFC(IsWrapperFor(a))

  /** 
   * @group Constructors (Primitives)
   */
  val last: ResultSetIO[Boolean] =
    F.liftFC(Last)

  /** 
   * @group Constructors (Primitives)
   */
  val moveToCurrentRow: ResultSetIO[Unit] =
    F.liftFC(MoveToCurrentRow)

  /** 
   * @group Constructors (Primitives)
   */
  val moveToInsertRow: ResultSetIO[Unit] =
    F.liftFC(MoveToInsertRow)

  /** 
   * @group Constructors (Primitives)
   */
  val next: ResultSetIO[Boolean] =
    F.liftFC(Next)

  /** 
   * @group Constructors (Primitives)
   */
  val previous: ResultSetIO[Boolean] =
    F.liftFC(Previous)

  /** 
   * @group Constructors (Primitives)
   */
  val refreshRow: ResultSetIO[Unit] =
    F.liftFC(RefreshRow)

  /** 
   * @group Constructors (Primitives)
   */
  def relative(a: Int): ResultSetIO[Boolean] =
    F.liftFC(Relative(a))

  /** 
   * @group Constructors (Primitives)
   */
  val rowDeleted: ResultSetIO[Boolean] =
    F.liftFC(RowDeleted)

  /** 
   * @group Constructors (Primitives)
   */
  val rowInserted: ResultSetIO[Boolean] =
    F.liftFC(RowInserted)

  /** 
   * @group Constructors (Primitives)
   */
  val rowUpdated: ResultSetIO[Boolean] =
    F.liftFC(RowUpdated)

  /** 
   * @group Constructors (Primitives)
   */
  def setFetchDirection(a: Int): ResultSetIO[Unit] =
    F.liftFC(SetFetchDirection(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setFetchSize(a: Int): ResultSetIO[Unit] =
    F.liftFC(SetFetchSize(a))

  /** 
   * @group Constructors (Primitives)
   */
  def unwrap[T](a: Class[T]): ResultSetIO[T] =
    F.liftFC(Unwrap(a))

  /** 
   * @group Constructors (Primitives)
   */
  def updateArray(a: Int, b: SqlArray): ResultSetIO[Unit] =
    F.liftFC(UpdateArray(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateArray(a: String, b: SqlArray): ResultSetIO[Unit] =
    F.liftFC(UpdateArray1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateAsciiStream(a: String, b: InputStream): ResultSetIO[Unit] =
    F.liftFC(UpdateAsciiStream(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateAsciiStream(a: Int, b: InputStream, c: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateAsciiStream1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateAsciiStream(a: Int, b: InputStream, c: Int): ResultSetIO[Unit] =
    F.liftFC(UpdateAsciiStream2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateAsciiStream(a: Int, b: InputStream): ResultSetIO[Unit] =
    F.liftFC(UpdateAsciiStream3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateAsciiStream(a: String, b: InputStream, c: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateAsciiStream4(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateAsciiStream(a: String, b: InputStream, c: Int): ResultSetIO[Unit] =
    F.liftFC(UpdateAsciiStream5(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBigDecimal(a: Int, b: BigDecimal): ResultSetIO[Unit] =
    F.liftFC(UpdateBigDecimal(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBigDecimal(a: String, b: BigDecimal): ResultSetIO[Unit] =
    F.liftFC(UpdateBigDecimal1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBinaryStream(a: String, b: InputStream, c: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateBinaryStream(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBinaryStream(a: String, b: InputStream, c: Int): ResultSetIO[Unit] =
    F.liftFC(UpdateBinaryStream1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBinaryStream(a: Int, b: InputStream, c: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateBinaryStream2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBinaryStream(a: Int, b: InputStream): ResultSetIO[Unit] =
    F.liftFC(UpdateBinaryStream3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBinaryStream(a: String, b: InputStream): ResultSetIO[Unit] =
    F.liftFC(UpdateBinaryStream4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBinaryStream(a: Int, b: InputStream, c: Int): ResultSetIO[Unit] =
    F.liftFC(UpdateBinaryStream5(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBlob(a: String, b: InputStream): ResultSetIO[Unit] =
    F.liftFC(UpdateBlob(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBlob(a: Int, b: InputStream): ResultSetIO[Unit] =
    F.liftFC(UpdateBlob1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBlob(a: String, b: Blob): ResultSetIO[Unit] =
    F.liftFC(UpdateBlob2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBlob(a: Int, b: Blob): ResultSetIO[Unit] =
    F.liftFC(UpdateBlob3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBlob(a: Int, b: InputStream, c: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateBlob4(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBlob(a: String, b: InputStream, c: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateBlob5(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBoolean(a: Int, b: Boolean): ResultSetIO[Unit] =
    F.liftFC(UpdateBoolean(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBoolean(a: String, b: Boolean): ResultSetIO[Unit] =
    F.liftFC(UpdateBoolean1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateByte(a: String, b: Byte): ResultSetIO[Unit] =
    F.liftFC(UpdateByte(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateByte(a: Int, b: Byte): ResultSetIO[Unit] =
    F.liftFC(UpdateByte1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBytes(a: String, b: Array[Byte]): ResultSetIO[Unit] =
    F.liftFC(UpdateBytes(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateBytes(a: Int, b: Array[Byte]): ResultSetIO[Unit] =
    F.liftFC(UpdateBytes1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateCharacterStream(a: Int, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateCharacterStream(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateCharacterStream(a: Int, b: Reader, c: Int): ResultSetIO[Unit] =
    F.liftFC(UpdateCharacterStream1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateCharacterStream(a: String, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateCharacterStream2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateCharacterStream(a: String, b: Reader): ResultSetIO[Unit] =
    F.liftFC(UpdateCharacterStream3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateCharacterStream(a: Int, b: Reader): ResultSetIO[Unit] =
    F.liftFC(UpdateCharacterStream4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateCharacterStream(a: String, b: Reader, c: Int): ResultSetIO[Unit] =
    F.liftFC(UpdateCharacterStream5(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateClob(a: String, b: Reader): ResultSetIO[Unit] =
    F.liftFC(UpdateClob(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateClob(a: String, b: Clob): ResultSetIO[Unit] =
    F.liftFC(UpdateClob1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateClob(a: Int, b: Clob): ResultSetIO[Unit] =
    F.liftFC(UpdateClob2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateClob(a: Int, b: Reader): ResultSetIO[Unit] =
    F.liftFC(UpdateClob3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateClob(a: Int, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateClob4(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateClob(a: String, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateClob5(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateDate(a: Int, b: Date): ResultSetIO[Unit] =
    F.liftFC(UpdateDate(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateDate(a: String, b: Date): ResultSetIO[Unit] =
    F.liftFC(UpdateDate1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateDouble(a: Int, b: Double): ResultSetIO[Unit] =
    F.liftFC(UpdateDouble(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateDouble(a: String, b: Double): ResultSetIO[Unit] =
    F.liftFC(UpdateDouble1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateFloat(a: String, b: Float): ResultSetIO[Unit] =
    F.liftFC(UpdateFloat(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateFloat(a: Int, b: Float): ResultSetIO[Unit] =
    F.liftFC(UpdateFloat1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateInt(a: Int, b: Int): ResultSetIO[Unit] =
    F.liftFC(UpdateInt(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateInt(a: String, b: Int): ResultSetIO[Unit] =
    F.liftFC(UpdateInt1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateLong(a: Int, b: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateLong(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateLong(a: String, b: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateLong1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNCharacterStream(a: String, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateNCharacterStream(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNCharacterStream(a: Int, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateNCharacterStream1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNCharacterStream(a: String, b: Reader): ResultSetIO[Unit] =
    F.liftFC(UpdateNCharacterStream2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNCharacterStream(a: Int, b: Reader): ResultSetIO[Unit] =
    F.liftFC(UpdateNCharacterStream3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNClob(a: String, b: Reader): ResultSetIO[Unit] =
    F.liftFC(UpdateNClob(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNClob(a: Int, b: NClob): ResultSetIO[Unit] =
    F.liftFC(UpdateNClob1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNClob(a: Int, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateNClob2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNClob(a: String, b: Reader, c: Long): ResultSetIO[Unit] =
    F.liftFC(UpdateNClob3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNClob(a: Int, b: Reader): ResultSetIO[Unit] =
    F.liftFC(UpdateNClob4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNClob(a: String, b: NClob): ResultSetIO[Unit] =
    F.liftFC(UpdateNClob5(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNString(a: String, b: String): ResultSetIO[Unit] =
    F.liftFC(UpdateNString(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNString(a: Int, b: String): ResultSetIO[Unit] =
    F.liftFC(UpdateNString1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNull(a: Int): ResultSetIO[Unit] =
    F.liftFC(UpdateNull(a))

  /** 
   * @group Constructors (Primitives)
   */
  def updateNull(a: String): ResultSetIO[Unit] =
    F.liftFC(UpdateNull1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def updateObject(a: String, b: Object, c: Int): ResultSetIO[Unit] =
    F.liftFC(UpdateObject(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateObject(a: String, b: Object): ResultSetIO[Unit] =
    F.liftFC(UpdateObject1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateObject(a: Int, b: Object): ResultSetIO[Unit] =
    F.liftFC(UpdateObject2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateObject(a: Int, b: Object, c: Int): ResultSetIO[Unit] =
    F.liftFC(UpdateObject3(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def updateRef(a: Int, b: Ref): ResultSetIO[Unit] =
    F.liftFC(UpdateRef(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateRef(a: String, b: Ref): ResultSetIO[Unit] =
    F.liftFC(UpdateRef1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val updateRow: ResultSetIO[Unit] =
    F.liftFC(UpdateRow)

  /** 
   * @group Constructors (Primitives)
   */
  def updateRowId(a: Int, b: RowId): ResultSetIO[Unit] =
    F.liftFC(UpdateRowId(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateRowId(a: String, b: RowId): ResultSetIO[Unit] =
    F.liftFC(UpdateRowId1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateSQLXML(a: String, b: SQLXML): ResultSetIO[Unit] =
    F.liftFC(UpdateSQLXML(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateSQLXML(a: Int, b: SQLXML): ResultSetIO[Unit] =
    F.liftFC(UpdateSQLXML1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateShort(a: Int, b: Short): ResultSetIO[Unit] =
    F.liftFC(UpdateShort(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateShort(a: String, b: Short): ResultSetIO[Unit] =
    F.liftFC(UpdateShort1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateString(a: Int, b: String): ResultSetIO[Unit] =
    F.liftFC(UpdateString(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateString(a: String, b: String): ResultSetIO[Unit] =
    F.liftFC(UpdateString1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateTime(a: Int, b: Time): ResultSetIO[Unit] =
    F.liftFC(UpdateTime(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateTime(a: String, b: Time): ResultSetIO[Unit] =
    F.liftFC(UpdateTime1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateTimestamp(a: String, b: Timestamp): ResultSetIO[Unit] =
    F.liftFC(UpdateTimestamp(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def updateTimestamp(a: Int, b: Timestamp): ResultSetIO[Unit] =
    F.liftFC(UpdateTimestamp1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val wasNull: ResultSetIO[Boolean] =
    F.liftFC(WasNull)

 /** 
  * Natural transformation from `ResultSetOp` to `Kleisli` for the given `M`, consuming a `java.sql.ResultSet`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: ResultSetOp ~> ({type l[a] = Kleisli[M, ResultSet, a]})#l =
   new (ResultSetOp ~> ({type l[a] = Kleisli[M, ResultSet, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: ResultSet => A): Kleisli[M, ResultSet, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: ResultSetOp[A]): Kleisli[M, ResultSet, A] = 
       op match {

        // Lifting
        case LiftBlobIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftCallableStatementIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftClobIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftConnectionIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftDatabaseMetaDataIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftDriverIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftNClobIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftPreparedStatementIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftRefIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftSQLDataIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftSQLInputIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftSQLOutputIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftStatementIO(s, k) => Kleisli(_ => k.transK[M].run(s))
  
        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.transK[M].attempt
  
        // Primitive Operations
        case Absolute(a) => primitive(_.absolute(a))
        case AfterLast => primitive(_.afterLast)
        case BeforeFirst => primitive(_.beforeFirst)
        case CancelRowUpdates => primitive(_.cancelRowUpdates)
        case ClearWarnings => primitive(_.clearWarnings)
        case Close => primitive(_.close)
        case DeleteRow => primitive(_.deleteRow)
        case FindColumn(a) => primitive(_.findColumn(a))
        case First => primitive(_.first)
        case GetArray(a) => primitive(_.getArray(a))
        case GetArray1(a) => primitive(_.getArray(a))
        case GetAsciiStream(a) => primitive(_.getAsciiStream(a))
        case GetAsciiStream1(a) => primitive(_.getAsciiStream(a))
        case GetBigDecimal(a) => primitive(_.getBigDecimal(a))
        case GetBigDecimal1(a) => primitive(_.getBigDecimal(a))
        case GetBigDecimal2(a, b) => primitive(_.getBigDecimal(a, b))
        case GetBigDecimal3(a, b) => primitive(_.getBigDecimal(a, b))
        case GetBinaryStream(a) => primitive(_.getBinaryStream(a))
        case GetBinaryStream1(a) => primitive(_.getBinaryStream(a))
        case GetBlob(a) => primitive(_.getBlob(a))
        case GetBlob1(a) => primitive(_.getBlob(a))
        case GetBoolean(a) => primitive(_.getBoolean(a))
        case GetBoolean1(a) => primitive(_.getBoolean(a))
        case GetByte(a) => primitive(_.getByte(a))
        case GetByte1(a) => primitive(_.getByte(a))
        case GetBytes(a) => primitive(_.getBytes(a))
        case GetBytes1(a) => primitive(_.getBytes(a))
        case GetCharacterStream(a) => primitive(_.getCharacterStream(a))
        case GetCharacterStream1(a) => primitive(_.getCharacterStream(a))
        case GetClob(a) => primitive(_.getClob(a))
        case GetClob1(a) => primitive(_.getClob(a))
        case GetConcurrency => primitive(_.getConcurrency)
        case GetCursorName => primitive(_.getCursorName)
        case GetDate(a, b) => primitive(_.getDate(a, b))
        case GetDate1(a) => primitive(_.getDate(a))
        case GetDate2(a, b) => primitive(_.getDate(a, b))
        case GetDate3(a) => primitive(_.getDate(a))
        case GetDouble(a) => primitive(_.getDouble(a))
        case GetDouble1(a) => primitive(_.getDouble(a))
        case GetFetchDirection => primitive(_.getFetchDirection)
        case GetFetchSize => primitive(_.getFetchSize)
        case GetFloat(a) => primitive(_.getFloat(a))
        case GetFloat1(a) => primitive(_.getFloat(a))
        case GetHoldability => primitive(_.getHoldability)
        case GetInt(a) => primitive(_.getInt(a))
        case GetInt1(a) => primitive(_.getInt(a))
        case GetLong(a) => primitive(_.getLong(a))
        case GetLong1(a) => primitive(_.getLong(a))
        case GetMetaData => primitive(_.getMetaData)
        case GetNCharacterStream(a) => primitive(_.getNCharacterStream(a))
        case GetNCharacterStream1(a) => primitive(_.getNCharacterStream(a))
        case GetNClob(a) => primitive(_.getNClob(a))
        case GetNClob1(a) => primitive(_.getNClob(a))
        case GetNString(a) => primitive(_.getNString(a))
        case GetNString1(a) => primitive(_.getNString(a))
        case GetObject(a, b) => primitive(_.getObject(a, b))
        case GetObject1(a, b) => primitive(_.getObject(a, b))
        case GetObject2(a) => primitive(_.getObject(a))
        case GetObject3(a) => primitive(_.getObject(a))
        case GetRef(a) => primitive(_.getRef(a))
        case GetRef1(a) => primitive(_.getRef(a))
        case GetRow => primitive(_.getRow)
        case GetRowId(a) => primitive(_.getRowId(a))
        case GetRowId1(a) => primitive(_.getRowId(a))
        case GetSQLXML(a) => primitive(_.getSQLXML(a))
        case GetSQLXML1(a) => primitive(_.getSQLXML(a))
        case GetShort(a) => primitive(_.getShort(a))
        case GetShort1(a) => primitive(_.getShort(a))
        case GetStatement => primitive(_.getStatement)
        case GetString(a) => primitive(_.getString(a))
        case GetString1(a) => primitive(_.getString(a))
        case GetTime(a, b) => primitive(_.getTime(a, b))
        case GetTime1(a) => primitive(_.getTime(a))
        case GetTime2(a) => primitive(_.getTime(a))
        case GetTime3(a, b) => primitive(_.getTime(a, b))
        case GetTimestamp(a) => primitive(_.getTimestamp(a))
        case GetTimestamp1(a, b) => primitive(_.getTimestamp(a, b))
        case GetTimestamp2(a, b) => primitive(_.getTimestamp(a, b))
        case GetTimestamp3(a) => primitive(_.getTimestamp(a))
        case GetType => primitive(_.getType)
        case GetURL(a) => primitive(_.getURL(a))
        case GetURL1(a) => primitive(_.getURL(a))
        case GetUnicodeStream(a) => primitive(_.getUnicodeStream(a))
        case GetUnicodeStream1(a) => primitive(_.getUnicodeStream(a))
        case GetWarnings => primitive(_.getWarnings)
        case InsertRow => primitive(_.insertRow)
        case IsAfterLast => primitive(_.isAfterLast)
        case IsBeforeFirst => primitive(_.isBeforeFirst)
        case IsClosed => primitive(_.isClosed)
        case IsFirst => primitive(_.isFirst)
        case IsLast => primitive(_.isLast)
        case IsWrapperFor(a) => primitive(_.isWrapperFor(a))
        case Last => primitive(_.last)
        case MoveToCurrentRow => primitive(_.moveToCurrentRow)
        case MoveToInsertRow => primitive(_.moveToInsertRow)
        case Next => primitive(_.next)
        case Previous => primitive(_.previous)
        case RefreshRow => primitive(_.refreshRow)
        case Relative(a) => primitive(_.relative(a))
        case RowDeleted => primitive(_.rowDeleted)
        case RowInserted => primitive(_.rowInserted)
        case RowUpdated => primitive(_.rowUpdated)
        case SetFetchDirection(a) => primitive(_.setFetchDirection(a))
        case SetFetchSize(a) => primitive(_.setFetchSize(a))
        case Unwrap(a) => primitive(_.unwrap(a))
        case UpdateArray(a, b) => primitive(_.updateArray(a, b))
        case UpdateArray1(a, b) => primitive(_.updateArray(a, b))
        case UpdateAsciiStream(a, b) => primitive(_.updateAsciiStream(a, b))
        case UpdateAsciiStream1(a, b, c) => primitive(_.updateAsciiStream(a, b, c))
        case UpdateAsciiStream2(a, b, c) => primitive(_.updateAsciiStream(a, b, c))
        case UpdateAsciiStream3(a, b) => primitive(_.updateAsciiStream(a, b))
        case UpdateAsciiStream4(a, b, c) => primitive(_.updateAsciiStream(a, b, c))
        case UpdateAsciiStream5(a, b, c) => primitive(_.updateAsciiStream(a, b, c))
        case UpdateBigDecimal(a, b) => primitive(_.updateBigDecimal(a, b))
        case UpdateBigDecimal1(a, b) => primitive(_.updateBigDecimal(a, b))
        case UpdateBinaryStream(a, b, c) => primitive(_.updateBinaryStream(a, b, c))
        case UpdateBinaryStream1(a, b, c) => primitive(_.updateBinaryStream(a, b, c))
        case UpdateBinaryStream2(a, b, c) => primitive(_.updateBinaryStream(a, b, c))
        case UpdateBinaryStream3(a, b) => primitive(_.updateBinaryStream(a, b))
        case UpdateBinaryStream4(a, b) => primitive(_.updateBinaryStream(a, b))
        case UpdateBinaryStream5(a, b, c) => primitive(_.updateBinaryStream(a, b, c))
        case UpdateBlob(a, b) => primitive(_.updateBlob(a, b))
        case UpdateBlob1(a, b) => primitive(_.updateBlob(a, b))
        case UpdateBlob2(a, b) => primitive(_.updateBlob(a, b))
        case UpdateBlob3(a, b) => primitive(_.updateBlob(a, b))
        case UpdateBlob4(a, b, c) => primitive(_.updateBlob(a, b, c))
        case UpdateBlob5(a, b, c) => primitive(_.updateBlob(a, b, c))
        case UpdateBoolean(a, b) => primitive(_.updateBoolean(a, b))
        case UpdateBoolean1(a, b) => primitive(_.updateBoolean(a, b))
        case UpdateByte(a, b) => primitive(_.updateByte(a, b))
        case UpdateByte1(a, b) => primitive(_.updateByte(a, b))
        case UpdateBytes(a, b) => primitive(_.updateBytes(a, b))
        case UpdateBytes1(a, b) => primitive(_.updateBytes(a, b))
        case UpdateCharacterStream(a, b, c) => primitive(_.updateCharacterStream(a, b, c))
        case UpdateCharacterStream1(a, b, c) => primitive(_.updateCharacterStream(a, b, c))
        case UpdateCharacterStream2(a, b, c) => primitive(_.updateCharacterStream(a, b, c))
        case UpdateCharacterStream3(a, b) => primitive(_.updateCharacterStream(a, b))
        case UpdateCharacterStream4(a, b) => primitive(_.updateCharacterStream(a, b))
        case UpdateCharacterStream5(a, b, c) => primitive(_.updateCharacterStream(a, b, c))
        case UpdateClob(a, b) => primitive(_.updateClob(a, b))
        case UpdateClob1(a, b) => primitive(_.updateClob(a, b))
        case UpdateClob2(a, b) => primitive(_.updateClob(a, b))
        case UpdateClob3(a, b) => primitive(_.updateClob(a, b))
        case UpdateClob4(a, b, c) => primitive(_.updateClob(a, b, c))
        case UpdateClob5(a, b, c) => primitive(_.updateClob(a, b, c))
        case UpdateDate(a, b) => primitive(_.updateDate(a, b))
        case UpdateDate1(a, b) => primitive(_.updateDate(a, b))
        case UpdateDouble(a, b) => primitive(_.updateDouble(a, b))
        case UpdateDouble1(a, b) => primitive(_.updateDouble(a, b))
        case UpdateFloat(a, b) => primitive(_.updateFloat(a, b))
        case UpdateFloat1(a, b) => primitive(_.updateFloat(a, b))
        case UpdateInt(a, b) => primitive(_.updateInt(a, b))
        case UpdateInt1(a, b) => primitive(_.updateInt(a, b))
        case UpdateLong(a, b) => primitive(_.updateLong(a, b))
        case UpdateLong1(a, b) => primitive(_.updateLong(a, b))
        case UpdateNCharacterStream(a, b, c) => primitive(_.updateNCharacterStream(a, b, c))
        case UpdateNCharacterStream1(a, b, c) => primitive(_.updateNCharacterStream(a, b, c))
        case UpdateNCharacterStream2(a, b) => primitive(_.updateNCharacterStream(a, b))
        case UpdateNCharacterStream3(a, b) => primitive(_.updateNCharacterStream(a, b))
        case UpdateNClob(a, b) => primitive(_.updateNClob(a, b))
        case UpdateNClob1(a, b) => primitive(_.updateNClob(a, b))
        case UpdateNClob2(a, b, c) => primitive(_.updateNClob(a, b, c))
        case UpdateNClob3(a, b, c) => primitive(_.updateNClob(a, b, c))
        case UpdateNClob4(a, b) => primitive(_.updateNClob(a, b))
        case UpdateNClob5(a, b) => primitive(_.updateNClob(a, b))
        case UpdateNString(a, b) => primitive(_.updateNString(a, b))
        case UpdateNString1(a, b) => primitive(_.updateNString(a, b))
        case UpdateNull(a) => primitive(_.updateNull(a))
        case UpdateNull1(a) => primitive(_.updateNull(a))
        case UpdateObject(a, b, c) => primitive(_.updateObject(a, b, c))
        case UpdateObject1(a, b) => primitive(_.updateObject(a, b))
        case UpdateObject2(a, b) => primitive(_.updateObject(a, b))
        case UpdateObject3(a, b, c) => primitive(_.updateObject(a, b, c))
        case UpdateRef(a, b) => primitive(_.updateRef(a, b))
        case UpdateRef1(a, b) => primitive(_.updateRef(a, b))
        case UpdateRow => primitive(_.updateRow)
        case UpdateRowId(a, b) => primitive(_.updateRowId(a, b))
        case UpdateRowId1(a, b) => primitive(_.updateRowId(a, b))
        case UpdateSQLXML(a, b) => primitive(_.updateSQLXML(a, b))
        case UpdateSQLXML1(a, b) => primitive(_.updateSQLXML(a, b))
        case UpdateShort(a, b) => primitive(_.updateShort(a, b))
        case UpdateShort1(a, b) => primitive(_.updateShort(a, b))
        case UpdateString(a, b) => primitive(_.updateString(a, b))
        case UpdateString1(a, b) => primitive(_.updateString(a, b))
        case UpdateTime(a, b) => primitive(_.updateTime(a, b))
        case UpdateTime1(a, b) => primitive(_.updateTime(a, b))
        case UpdateTimestamp(a, b) => primitive(_.updateTimestamp(a, b))
        case UpdateTimestamp1(a, b) => primitive(_.updateTimestamp(a, b))
        case WasNull => primitive(_.wasNull)
  
      }
  
    }

  /**
   * Syntax for `ResultSetIO`.
   * @group Algebra
   */
  implicit class ResultSetIOOps[A](ma: ResultSetIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, ResultSet, A] =
      F.runFC[ResultSetOp,({type l[a]=Kleisli[M,ResultSet,a]})#l,A](ma)(kleisliTrans[M])
  }

}

