package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import java.io.InputStream
import java.io.Reader
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
import java.sql.RowId
import java.sql.SQLData
import java.sql.SQLInput
import java.sql.SQLOutput
import java.sql.SQLXML
import java.sql.Statement
import java.sql.Struct
import java.sql.Time
import java.sql.Timestamp
import java.sql.{ Array => SqlArray }

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
 * Algebra and free monad for primitive operations over a `java.sql.SQLOutput`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `SQLOutputIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `SQLOutputOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, SQLOutput, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: SQLOutputIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: SQLOutput = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object sqloutput {
  
  /** 
   * Sum type of primitive operations over a `java.sql.SQLOutput`.
   * @group Algebra 
   */
  sealed trait SQLOutputOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: SQLOutput => A): Kleisli[M, SQLOutput, A] = 
      Kleisli((s: SQLOutput) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, SQLOutput, A]
  }

  /** 
   * Module of constructors for `SQLOutputOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `sqloutput` module.
   * @group Algebra 
   */
  object SQLOutputOp {
    
    // Lifting
    case class LiftBlobIO[A](s: Blob, action: BlobIO[A]) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftCallableStatementIO[A](s: CallableStatement, action: CallableStatementIO[A]) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftClobIO[A](s: Clob, action: ClobIO[A]) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftConnectionIO[A](s: Connection, action: ConnectionIO[A]) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDatabaseMetaDataIO[A](s: DatabaseMetaData, action: DatabaseMetaDataIO[A]) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDriverIO[A](s: Driver, action: DriverIO[A]) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftNClobIO[A](s: NClob, action: NClobIO[A]) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftPreparedStatementIO[A](s: PreparedStatement, action: PreparedStatementIO[A]) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftRefIO[A](s: Ref, action: RefIO[A]) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftResultSetIO[A](s: ResultSet, action: ResultSetIO[A]) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLDataIO[A](s: SQLData, action: SQLDataIO[A]) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLInputIO[A](s: SQLInput, action: SQLInputIO[A]) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftStatementIO[A](s: Statement, action: StatementIO[A]) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }

    // Combinators
    case class Attempt[A](action: SQLOutputIO[A]) extends SQLOutputOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, SQLOutput, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: SQLOutput => A) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case class  WriteArray(a: SqlArray) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeArray(a))
    }
    case class  WriteAsciiStream(a: InputStream) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeAsciiStream(a))
    }
    case class  WriteBigDecimal(a: BigDecimal) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeBigDecimal(a))
    }
    case class  WriteBinaryStream(a: InputStream) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeBinaryStream(a))
    }
    case class  WriteBlob(a: Blob) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeBlob(a))
    }
    case class  WriteBoolean(a: Boolean) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeBoolean(a))
    }
    case class  WriteByte(a: Byte) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeByte(a))
    }
    case class  WriteBytes(a: Array[Byte]) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeBytes(a))
    }
    case class  WriteCharacterStream(a: Reader) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeCharacterStream(a))
    }
    case class  WriteClob(a: Clob) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeClob(a))
    }
    case class  WriteDate(a: Date) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeDate(a))
    }
    case class  WriteDouble(a: Double) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeDouble(a))
    }
    case class  WriteFloat(a: Float) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeFloat(a))
    }
    case class  WriteInt(a: Int) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeInt(a))
    }
    case class  WriteLong(a: Long) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeLong(a))
    }
    case class  WriteNClob(a: NClob) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeNClob(a))
    }
    case class  WriteNString(a: String) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeNString(a))
    }
    case class  WriteObject(a: SQLData) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeObject(a))
    }
    case class  WriteRef(a: Ref) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeRef(a))
    }
    case class  WriteRowId(a: RowId) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeRowId(a))
    }
    case class  WriteSQLXML(a: SQLXML) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeSQLXML(a))
    }
    case class  WriteShort(a: Short) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeShort(a))
    }
    case class  WriteString(a: String) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeString(a))
    }
    case class  WriteStruct(a: Struct) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeStruct(a))
    }
    case class  WriteTime(a: Time) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeTime(a))
    }
    case class  WriteTimestamp(a: Timestamp) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeTimestamp(a))
    }
    case class  WriteURL(a: URL) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeURL(a))
    }

  }
  import SQLOutputOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[SQLOutputOp]]; abstractly, a computation that consumes 
   * a `java.sql.SQLOutput` and produces a value of type `A`. 
   * @group Algebra 
   */
  type SQLOutputIO[A] = F.FreeC[SQLOutputOp, A]

  /**
   * Monad instance for [[SQLOutputIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadSQLOutputIO: Monad[SQLOutputIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[SQLOutputOp, α]})#λ]

  /**
   * Catchable instance for [[SQLOutputIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableSQLOutputIO: Catchable[SQLOutputIO] =
    new Catchable[SQLOutputIO] {
      def attempt[A](f: SQLOutputIO[A]): SQLOutputIO[Throwable \/ A] = sqloutput.attempt(f)
      def fail[A](err: Throwable): SQLOutputIO[A] = sqloutput.delay(throw err)
    }

  /**
   * Capture instance for [[SQLOutputIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureSQLOutputIO: Capture[SQLOutputIO] =
    new Capture[SQLOutputIO] {
      def apply[A](a: => A): SQLOutputIO[A] = sqloutput.delay(a)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftBlob[A](s: Blob, k: BlobIO[A]): SQLOutputIO[A] =
    F.liftFC(LiftBlobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftCallableStatement[A](s: CallableStatement, k: CallableStatementIO[A]): SQLOutputIO[A] =
    F.liftFC(LiftCallableStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftClob[A](s: Clob, k: ClobIO[A]): SQLOutputIO[A] =
    F.liftFC(LiftClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftConnection[A](s: Connection, k: ConnectionIO[A]): SQLOutputIO[A] =
    F.liftFC(LiftConnectionIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDatabaseMetaData[A](s: DatabaseMetaData, k: DatabaseMetaDataIO[A]): SQLOutputIO[A] =
    F.liftFC(LiftDatabaseMetaDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDriver[A](s: Driver, k: DriverIO[A]): SQLOutputIO[A] =
    F.liftFC(LiftDriverIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftNClob[A](s: NClob, k: NClobIO[A]): SQLOutputIO[A] =
    F.liftFC(LiftNClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftPreparedStatement[A](s: PreparedStatement, k: PreparedStatementIO[A]): SQLOutputIO[A] =
    F.liftFC(LiftPreparedStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftRef[A](s: Ref, k: RefIO[A]): SQLOutputIO[A] =
    F.liftFC(LiftRefIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftResultSet[A](s: ResultSet, k: ResultSetIO[A]): SQLOutputIO[A] =
    F.liftFC(LiftResultSetIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLData[A](s: SQLData, k: SQLDataIO[A]): SQLOutputIO[A] =
    F.liftFC(LiftSQLDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLInput[A](s: SQLInput, k: SQLInputIO[A]): SQLOutputIO[A] =
    F.liftFC(LiftSQLInputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftStatement[A](s: Statement, k: StatementIO[A]): SQLOutputIO[A] =
    F.liftFC(LiftStatementIO(s, k))

  /** 
   * Lift a SQLOutputIO[A] into an exception-capturing SQLOutputIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: SQLOutputIO[A]): SQLOutputIO[Throwable \/ A] =
    F.liftFC[SQLOutputOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): SQLOutputIO[A] =
    F.liftFC(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying SQLOutput.
   * @group Constructors (Lifting)
   */
  def raw[A](f: SQLOutput => A): SQLOutputIO[A] =
    F.liftFC(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  def writeArray(a: SqlArray): SQLOutputIO[Unit] =
    F.liftFC(WriteArray(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeAsciiStream(a: InputStream): SQLOutputIO[Unit] =
    F.liftFC(WriteAsciiStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeBigDecimal(a: BigDecimal): SQLOutputIO[Unit] =
    F.liftFC(WriteBigDecimal(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeBinaryStream(a: InputStream): SQLOutputIO[Unit] =
    F.liftFC(WriteBinaryStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeBlob(a: Blob): SQLOutputIO[Unit] =
    F.liftFC(WriteBlob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeBoolean(a: Boolean): SQLOutputIO[Unit] =
    F.liftFC(WriteBoolean(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeByte(a: Byte): SQLOutputIO[Unit] =
    F.liftFC(WriteByte(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeBytes(a: Array[Byte]): SQLOutputIO[Unit] =
    F.liftFC(WriteBytes(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeCharacterStream(a: Reader): SQLOutputIO[Unit] =
    F.liftFC(WriteCharacterStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeClob(a: Clob): SQLOutputIO[Unit] =
    F.liftFC(WriteClob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeDate(a: Date): SQLOutputIO[Unit] =
    F.liftFC(WriteDate(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeDouble(a: Double): SQLOutputIO[Unit] =
    F.liftFC(WriteDouble(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeFloat(a: Float): SQLOutputIO[Unit] =
    F.liftFC(WriteFloat(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeInt(a: Int): SQLOutputIO[Unit] =
    F.liftFC(WriteInt(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeLong(a: Long): SQLOutputIO[Unit] =
    F.liftFC(WriteLong(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeNClob(a: NClob): SQLOutputIO[Unit] =
    F.liftFC(WriteNClob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeNString(a: String): SQLOutputIO[Unit] =
    F.liftFC(WriteNString(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeObject(a: SQLData): SQLOutputIO[Unit] =
    F.liftFC(WriteObject(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeRef(a: Ref): SQLOutputIO[Unit] =
    F.liftFC(WriteRef(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeRowId(a: RowId): SQLOutputIO[Unit] =
    F.liftFC(WriteRowId(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeSQLXML(a: SQLXML): SQLOutputIO[Unit] =
    F.liftFC(WriteSQLXML(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeShort(a: Short): SQLOutputIO[Unit] =
    F.liftFC(WriteShort(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeString(a: String): SQLOutputIO[Unit] =
    F.liftFC(WriteString(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeStruct(a: Struct): SQLOutputIO[Unit] =
    F.liftFC(WriteStruct(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeTime(a: Time): SQLOutputIO[Unit] =
    F.liftFC(WriteTime(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeTimestamp(a: Timestamp): SQLOutputIO[Unit] =
    F.liftFC(WriteTimestamp(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeURL(a: URL): SQLOutputIO[Unit] =
    F.liftFC(WriteURL(a))

 /** 
  * Natural transformation from `SQLOutputOp` to `Kleisli` for the given `M`, consuming a `java.sql.SQLOutput`. 
  * @group Algebra
  */
  def kleisliTrans[M[_]: Monad: Catchable: Capture]: SQLOutputOp ~> Kleisli[M, SQLOutput, ?] =
    new (SQLOutputOp ~> Kleisli[M, SQLOutput, ?]) {
      def apply[A](op: SQLOutputOp[A]): Kleisli[M, SQLOutput, A] =
        op.defaultTransK[M]
    }

  /**
   * Syntax for `SQLOutputIO`.
   * @group Algebra
   */
  implicit class SQLOutputIOOps[A](ma: SQLOutputIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, SQLOutput, A] =
      F.runFC[SQLOutputOp, Kleisli[M, SQLOutput, ?], A](ma)(kleisliTrans[M])
  }

}

