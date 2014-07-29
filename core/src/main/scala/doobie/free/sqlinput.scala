package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import java.io.InputStream
import java.io.Reader
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
import java.sql.RowId
import java.sql.SQLData
import java.sql.SQLInput
import java.sql.SQLOutput
import java.sql.SQLXML
import java.sql.Statement
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
 * Algebra and free monad for primitive operations over a `java.sql.SQLInput`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `SQLInputIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `SQLInputOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, SQLInput, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `liftK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: SQLInputIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: SQLInput = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.liftK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object sqlinput {
  
  /** 
   * Sum type of primitive operations over a `java.sql.SQLInput`.
   * @group Algebra 
   */
  sealed trait SQLInputOp[A]

  /** 
   * Module of constructors for `SQLInputOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `sqlinput` module.
   * @group Algebra 
   */
  object SQLInputOp {
    
    // Lifting
    case class LiftBlobIO[A](s: Blob, action: BlobIO[A]) extends SQLInputOp[A]
    case class LiftCallableStatementIO[A](s: CallableStatement, action: CallableStatementIO[A]) extends SQLInputOp[A]
    case class LiftClobIO[A](s: Clob, action: ClobIO[A]) extends SQLInputOp[A]
    case class LiftConnectionIO[A](s: Connection, action: ConnectionIO[A]) extends SQLInputOp[A]
    case class LiftDatabaseMetaDataIO[A](s: DatabaseMetaData, action: DatabaseMetaDataIO[A]) extends SQLInputOp[A]
    case class LiftDriverIO[A](s: Driver, action: DriverIO[A]) extends SQLInputOp[A]
    case class LiftNClobIO[A](s: NClob, action: NClobIO[A]) extends SQLInputOp[A]
    case class LiftPreparedStatementIO[A](s: PreparedStatement, action: PreparedStatementIO[A]) extends SQLInputOp[A]
    case class LiftRefIO[A](s: Ref, action: RefIO[A]) extends SQLInputOp[A]
    case class LiftResultSetIO[A](s: ResultSet, action: ResultSetIO[A]) extends SQLInputOp[A]
    case class LiftSQLDataIO[A](s: SQLData, action: SQLDataIO[A]) extends SQLInputOp[A]
    case class LiftSQLOutputIO[A](s: SQLOutput, action: SQLOutputIO[A]) extends SQLInputOp[A]
    case class LiftStatementIO[A](s: Statement, action: StatementIO[A]) extends SQLInputOp[A]

    // Combinators
    case class Attempt[A](action: SQLInputIO[A]) extends SQLInputOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends SQLInputOp[A]

    // Primitive Operations
    case object ReadArray extends SQLInputOp[SqlArray]
    case object ReadAsciiStream extends SQLInputOp[InputStream]
    case object ReadBigDecimal extends SQLInputOp[BigDecimal]
    case object ReadBinaryStream extends SQLInputOp[InputStream]
    case object ReadBlob extends SQLInputOp[Blob]
    case object ReadBoolean extends SQLInputOp[Boolean]
    case object ReadByte extends SQLInputOp[Byte]
    case object ReadBytes extends SQLInputOp[Array[Byte]]
    case object ReadCharacterStream extends SQLInputOp[Reader]
    case object ReadClob extends SQLInputOp[Clob]
    case object ReadDate extends SQLInputOp[Date]
    case object ReadDouble extends SQLInputOp[Double]
    case object ReadFloat extends SQLInputOp[Float]
    case object ReadInt extends SQLInputOp[Int]
    case object ReadLong extends SQLInputOp[Long]
    case object ReadNClob extends SQLInputOp[NClob]
    case object ReadNString extends SQLInputOp[String]
    case object ReadObject extends SQLInputOp[Object]
    case object ReadRef extends SQLInputOp[Ref]
    case object ReadRowId extends SQLInputOp[RowId]
    case object ReadSQLXML extends SQLInputOp[SQLXML]
    case object ReadShort extends SQLInputOp[Short]
    case object ReadString extends SQLInputOp[String]
    case object ReadTime extends SQLInputOp[Time]
    case object ReadTimestamp extends SQLInputOp[Timestamp]
    case object ReadURL extends SQLInputOp[URL]
    case object WasNull extends SQLInputOp[Boolean]

  }
  import SQLInputOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[SQLInputOp]]; abstractly, a computation that consumes 
   * a `java.sql.SQLInput` and produces a value of type `A`. 
   * @group Algebra 
   */
  type SQLInputIO[A] = F.FreeC[SQLInputOp, A]

  /**
   * Monad instance for [[SQLInputIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadSQLInputIO: Monad[SQLInputIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[SQLInputOp, α]})#λ]

  /**
   * Catchable instance for [[SQLInputIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableSQLInputIO: Catchable[SQLInputIO] =
    new Catchable[SQLInputIO] {
      def attempt[A](f: SQLInputIO[A]): SQLInputIO[Throwable \/ A] = sqlinput.attempt(f)
      def fail[A](err: Throwable): SQLInputIO[A] = sqlinput.delay(throw err)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftBlob[A](s: Blob, k: BlobIO[A]): SQLInputIO[A] =
    F.liftFC(LiftBlobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftCallableStatement[A](s: CallableStatement, k: CallableStatementIO[A]): SQLInputIO[A] =
    F.liftFC(LiftCallableStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftClob[A](s: Clob, k: ClobIO[A]): SQLInputIO[A] =
    F.liftFC(LiftClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftConnection[A](s: Connection, k: ConnectionIO[A]): SQLInputIO[A] =
    F.liftFC(LiftConnectionIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDatabaseMetaData[A](s: DatabaseMetaData, k: DatabaseMetaDataIO[A]): SQLInputIO[A] =
    F.liftFC(LiftDatabaseMetaDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDriver[A](s: Driver, k: DriverIO[A]): SQLInputIO[A] =
    F.liftFC(LiftDriverIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftNClob[A](s: NClob, k: NClobIO[A]): SQLInputIO[A] =
    F.liftFC(LiftNClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftPreparedStatement[A](s: PreparedStatement, k: PreparedStatementIO[A]): SQLInputIO[A] =
    F.liftFC(LiftPreparedStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftRef[A](s: Ref, k: RefIO[A]): SQLInputIO[A] =
    F.liftFC(LiftRefIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftResultSet[A](s: ResultSet, k: ResultSetIO[A]): SQLInputIO[A] =
    F.liftFC(LiftResultSetIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLData[A](s: SQLData, k: SQLDataIO[A]): SQLInputIO[A] =
    F.liftFC(LiftSQLDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLOutput[A](s: SQLOutput, k: SQLOutputIO[A]): SQLInputIO[A] =
    F.liftFC(LiftSQLOutputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftStatement[A](s: Statement, k: StatementIO[A]): SQLInputIO[A] =
    F.liftFC(LiftStatementIO(s, k))

  /** 
   * Lift a SQLInputIO[A] into an exception-capturing SQLInputIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: SQLInputIO[A]): SQLInputIO[Throwable \/ A] =
    F.liftFC[SQLInputOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): SQLInputIO[A] =
    F.liftFC(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  val readArray: SQLInputIO[SqlArray] =
    F.liftFC(ReadArray)

  /** 
   * @group Constructors (Primitives)
   */
  val readAsciiStream: SQLInputIO[InputStream] =
    F.liftFC(ReadAsciiStream)

  /** 
   * @group Constructors (Primitives)
   */
  val readBigDecimal: SQLInputIO[BigDecimal] =
    F.liftFC(ReadBigDecimal)

  /** 
   * @group Constructors (Primitives)
   */
  val readBinaryStream: SQLInputIO[InputStream] =
    F.liftFC(ReadBinaryStream)

  /** 
   * @group Constructors (Primitives)
   */
  val readBlob: SQLInputIO[Blob] =
    F.liftFC(ReadBlob)

  /** 
   * @group Constructors (Primitives)
   */
  val readBoolean: SQLInputIO[Boolean] =
    F.liftFC(ReadBoolean)

  /** 
   * @group Constructors (Primitives)
   */
  val readByte: SQLInputIO[Byte] =
    F.liftFC(ReadByte)

  /** 
   * @group Constructors (Primitives)
   */
  val readBytes: SQLInputIO[Array[Byte]] =
    F.liftFC(ReadBytes)

  /** 
   * @group Constructors (Primitives)
   */
  val readCharacterStream: SQLInputIO[Reader] =
    F.liftFC(ReadCharacterStream)

  /** 
   * @group Constructors (Primitives)
   */
  val readClob: SQLInputIO[Clob] =
    F.liftFC(ReadClob)

  /** 
   * @group Constructors (Primitives)
   */
  val readDate: SQLInputIO[Date] =
    F.liftFC(ReadDate)

  /** 
   * @group Constructors (Primitives)
   */
  val readDouble: SQLInputIO[Double] =
    F.liftFC(ReadDouble)

  /** 
   * @group Constructors (Primitives)
   */
  val readFloat: SQLInputIO[Float] =
    F.liftFC(ReadFloat)

  /** 
   * @group Constructors (Primitives)
   */
  val readInt: SQLInputIO[Int] =
    F.liftFC(ReadInt)

  /** 
   * @group Constructors (Primitives)
   */
  val readLong: SQLInputIO[Long] =
    F.liftFC(ReadLong)

  /** 
   * @group Constructors (Primitives)
   */
  val readNClob: SQLInputIO[NClob] =
    F.liftFC(ReadNClob)

  /** 
   * @group Constructors (Primitives)
   */
  val readNString: SQLInputIO[String] =
    F.liftFC(ReadNString)

  /** 
   * @group Constructors (Primitives)
   */
  val readObject: SQLInputIO[Object] =
    F.liftFC(ReadObject)

  /** 
   * @group Constructors (Primitives)
   */
  val readRef: SQLInputIO[Ref] =
    F.liftFC(ReadRef)

  /** 
   * @group Constructors (Primitives)
   */
  val readRowId: SQLInputIO[RowId] =
    F.liftFC(ReadRowId)

  /** 
   * @group Constructors (Primitives)
   */
  val readSQLXML: SQLInputIO[SQLXML] =
    F.liftFC(ReadSQLXML)

  /** 
   * @group Constructors (Primitives)
   */
  val readShort: SQLInputIO[Short] =
    F.liftFC(ReadShort)

  /** 
   * @group Constructors (Primitives)
   */
  val readString: SQLInputIO[String] =
    F.liftFC(ReadString)

  /** 
   * @group Constructors (Primitives)
   */
  val readTime: SQLInputIO[Time] =
    F.liftFC(ReadTime)

  /** 
   * @group Constructors (Primitives)
   */
  val readTimestamp: SQLInputIO[Timestamp] =
    F.liftFC(ReadTimestamp)

  /** 
   * @group Constructors (Primitives)
   */
  val readURL: SQLInputIO[URL] =
    F.liftFC(ReadURL)

  /** 
   * @group Constructors (Primitives)
   */
  val wasNull: SQLInputIO[Boolean] =
    F.liftFC(WasNull)

 /** 
  * Natural transformation from `SQLInputOp` to `Kleisli` for the given `M`, consuming a `java.sql.SQLInput`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: SQLInputOp ~> ({type l[a] = Kleisli[M, SQLInput, a]})#l =
   new (SQLInputOp ~> ({type l[a] = Kleisli[M, SQLInput, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: SQLInput => A): Kleisli[M, SQLInput, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: SQLInputOp[A]): Kleisli[M, SQLInput, A] = 
       op match {

        // Lifting
        case LiftBlobIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftCallableStatementIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftClobIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftConnectionIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftDatabaseMetaDataIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftDriverIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftNClobIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftPreparedStatementIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftRefIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftResultSetIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftSQLDataIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftSQLOutputIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftStatementIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
  
        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.liftK[M].attempt
  
        // Primitive Operations
        case ReadArray => primitive(_.readArray)
        case ReadAsciiStream => primitive(_.readAsciiStream)
        case ReadBigDecimal => primitive(_.readBigDecimal)
        case ReadBinaryStream => primitive(_.readBinaryStream)
        case ReadBlob => primitive(_.readBlob)
        case ReadBoolean => primitive(_.readBoolean)
        case ReadByte => primitive(_.readByte)
        case ReadBytes => primitive(_.readBytes)
        case ReadCharacterStream => primitive(_.readCharacterStream)
        case ReadClob => primitive(_.readClob)
        case ReadDate => primitive(_.readDate)
        case ReadDouble => primitive(_.readDouble)
        case ReadFloat => primitive(_.readFloat)
        case ReadInt => primitive(_.readInt)
        case ReadLong => primitive(_.readLong)
        case ReadNClob => primitive(_.readNClob)
        case ReadNString => primitive(_.readNString)
        case ReadObject => primitive(_.readObject)
        case ReadRef => primitive(_.readRef)
        case ReadRowId => primitive(_.readRowId)
        case ReadSQLXML => primitive(_.readSQLXML)
        case ReadShort => primitive(_.readShort)
        case ReadString => primitive(_.readString)
        case ReadTime => primitive(_.readTime)
        case ReadTimestamp => primitive(_.readTimestamp)
        case ReadURL => primitive(_.readURL)
        case WasNull => primitive(_.wasNull)
  
      }
  
    }

  /**
   * Syntax for `SQLInputIO`.
   * @group Algebra
   */
  implicit class SQLInputIOOps[A](ma: SQLInputIO[A]) {
    def liftK[M[_]: Monad: Catchable: Capture]: Kleisli[M, SQLInput, A] =
      F.runFC[SQLInputOp,({type l[a]=Kleisli[M,SQLInput,a]})#l,A](ma)(kleisliTrans[M])
  }

}

