package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import java.io.InputStream
import java.io.OutputStream
import java.sql.Blob
import java.sql.CallableStatement
import java.sql.Clob
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.Driver
import java.sql.NClob
import java.sql.PreparedStatement
import java.sql.Ref
import java.sql.ResultSet
import java.sql.SQLData
import java.sql.SQLInput
import java.sql.SQLOutput
import java.sql.Statement

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
 * Algebra and free monad for primitive operations over a `java.sql.Blob`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `BlobIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `BlobOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, Blob, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: BlobIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: Blob = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object blob {
  
  /** 
   * Sum type of primitive operations over a `java.sql.Blob`.
   * @group Algebra 
   */
  sealed trait BlobOp[A]

  /** 
   * Module of constructors for `BlobOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `blob` module.
   * @group Algebra 
   */
  object BlobOp {
    
    // Lifting
    case class LiftCallableStatementIO[A](s: CallableStatement, action: CallableStatementIO[A]) extends BlobOp[A]
    case class LiftClobIO[A](s: Clob, action: ClobIO[A]) extends BlobOp[A]
    case class LiftConnectionIO[A](s: Connection, action: ConnectionIO[A]) extends BlobOp[A]
    case class LiftDatabaseMetaDataIO[A](s: DatabaseMetaData, action: DatabaseMetaDataIO[A]) extends BlobOp[A]
    case class LiftDriverIO[A](s: Driver, action: DriverIO[A]) extends BlobOp[A]
    case class LiftNClobIO[A](s: NClob, action: NClobIO[A]) extends BlobOp[A]
    case class LiftPreparedStatementIO[A](s: PreparedStatement, action: PreparedStatementIO[A]) extends BlobOp[A]
    case class LiftRefIO[A](s: Ref, action: RefIO[A]) extends BlobOp[A]
    case class LiftResultSetIO[A](s: ResultSet, action: ResultSetIO[A]) extends BlobOp[A]
    case class LiftSQLDataIO[A](s: SQLData, action: SQLDataIO[A]) extends BlobOp[A]
    case class LiftSQLInputIO[A](s: SQLInput, action: SQLInputIO[A]) extends BlobOp[A]
    case class LiftSQLOutputIO[A](s: SQLOutput, action: SQLOutputIO[A]) extends BlobOp[A]
    case class LiftStatementIO[A](s: Statement, action: StatementIO[A]) extends BlobOp[A]

    // Combinators
    case class Attempt[A](action: BlobIO[A]) extends BlobOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends BlobOp[A]

    // Primitive Operations
    case object Free extends BlobOp[Unit]
    case class  GetBinaryStream(a: Long, b: Long) extends BlobOp[InputStream]
    case object GetBinaryStream1 extends BlobOp[InputStream]
    case class  GetBytes(a: Long, b: Int) extends BlobOp[Array[Byte]]
    case object Length extends BlobOp[Long]
    case class  Position(a: Array[Byte], b: Long) extends BlobOp[Long]
    case class  Position1(a: Blob, b: Long) extends BlobOp[Long]
    case class  SetBinaryStream(a: Long) extends BlobOp[OutputStream]
    case class  SetBytes(a: Long, b: Array[Byte], c: Int, d: Int) extends BlobOp[Int]
    case class  SetBytes1(a: Long, b: Array[Byte]) extends BlobOp[Int]
    case class  Truncate(a: Long) extends BlobOp[Unit]

  }
  import BlobOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[BlobOp]]; abstractly, a computation that consumes 
   * a `java.sql.Blob` and produces a value of type `A`. 
   * @group Algebra 
   */
  type BlobIO[A] = F.FreeC[BlobOp, A]

  /**
   * Monad instance for [[BlobIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadBlobIO: Monad[BlobIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[BlobOp, α]})#λ]

  /**
   * Catchable instance for [[BlobIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableBlobIO: Catchable[BlobIO] =
    new Catchable[BlobIO] {
      def attempt[A](f: BlobIO[A]): BlobIO[Throwable \/ A] = blob.attempt(f)
      def fail[A](err: Throwable): BlobIO[A] = blob.delay(throw err)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftCallableStatement[A](s: CallableStatement, k: CallableStatementIO[A]): BlobIO[A] =
    F.liftFC(LiftCallableStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftClob[A](s: Clob, k: ClobIO[A]): BlobIO[A] =
    F.liftFC(LiftClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftConnection[A](s: Connection, k: ConnectionIO[A]): BlobIO[A] =
    F.liftFC(LiftConnectionIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDatabaseMetaData[A](s: DatabaseMetaData, k: DatabaseMetaDataIO[A]): BlobIO[A] =
    F.liftFC(LiftDatabaseMetaDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDriver[A](s: Driver, k: DriverIO[A]): BlobIO[A] =
    F.liftFC(LiftDriverIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftNClob[A](s: NClob, k: NClobIO[A]): BlobIO[A] =
    F.liftFC(LiftNClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftPreparedStatement[A](s: PreparedStatement, k: PreparedStatementIO[A]): BlobIO[A] =
    F.liftFC(LiftPreparedStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftRef[A](s: Ref, k: RefIO[A]): BlobIO[A] =
    F.liftFC(LiftRefIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftResultSet[A](s: ResultSet, k: ResultSetIO[A]): BlobIO[A] =
    F.liftFC(LiftResultSetIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLData[A](s: SQLData, k: SQLDataIO[A]): BlobIO[A] =
    F.liftFC(LiftSQLDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLInput[A](s: SQLInput, k: SQLInputIO[A]): BlobIO[A] =
    F.liftFC(LiftSQLInputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLOutput[A](s: SQLOutput, k: SQLOutputIO[A]): BlobIO[A] =
    F.liftFC(LiftSQLOutputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftStatement[A](s: Statement, k: StatementIO[A]): BlobIO[A] =
    F.liftFC(LiftStatementIO(s, k))

  /** 
   * Lift a BlobIO[A] into an exception-capturing BlobIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: BlobIO[A]): BlobIO[Throwable \/ A] =
    F.liftFC[BlobOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): BlobIO[A] =
    F.liftFC(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  val free: BlobIO[Unit] =
    F.liftFC(Free)

  /** 
   * @group Constructors (Primitives)
   */
  def getBinaryStream(a: Long, b: Long): BlobIO[InputStream] =
    F.liftFC(GetBinaryStream(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val getBinaryStream: BlobIO[InputStream] =
    F.liftFC(GetBinaryStream1)

  /** 
   * @group Constructors (Primitives)
   */
  def getBytes(a: Long, b: Int): BlobIO[Array[Byte]] =
    F.liftFC(GetBytes(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val length: BlobIO[Long] =
    F.liftFC(Length)

  /** 
   * @group Constructors (Primitives)
   */
  def position(a: Array[Byte], b: Long): BlobIO[Long] =
    F.liftFC(Position(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def position(a: Blob, b: Long): BlobIO[Long] =
    F.liftFC(Position1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: Long): BlobIO[OutputStream] =
    F.liftFC(SetBinaryStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setBytes(a: Long, b: Array[Byte], c: Int, d: Int): BlobIO[Int] =
    F.liftFC(SetBytes(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def setBytes(a: Long, b: Array[Byte]): BlobIO[Int] =
    F.liftFC(SetBytes1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def truncate(a: Long): BlobIO[Unit] =
    F.liftFC(Truncate(a))

 /** 
  * Natural transformation from `BlobOp` to `Kleisli` for the given `M`, consuming a `java.sql.Blob`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: BlobOp ~> ({type l[a] = Kleisli[M, Blob, a]})#l =
   new (BlobOp ~> ({type l[a] = Kleisli[M, Blob, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: Blob => A): Kleisli[M, Blob, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: BlobOp[A]): Kleisli[M, Blob, A] = 
       op match {

        // Lifting
        case LiftCallableStatementIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftClobIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftConnectionIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftDatabaseMetaDataIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftDriverIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftNClobIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftPreparedStatementIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftRefIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftResultSetIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftSQLDataIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftSQLInputIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftSQLOutputIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftStatementIO(s, k) => Kleisli(_ => k.transK[M].run(s))
  
        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.transK[M].attempt
  
        // Primitive Operations
        case Free => primitive(_.free)
        case GetBinaryStream(a, b) => primitive(_.getBinaryStream(a, b))
        case GetBinaryStream1 => primitive(_.getBinaryStream)
        case GetBytes(a, b) => primitive(_.getBytes(a, b))
        case Length => primitive(_.length)
        case Position(a, b) => primitive(_.position(a, b))
        case Position1(a, b) => primitive(_.position(a, b))
        case SetBinaryStream(a) => primitive(_.setBinaryStream(a))
        case SetBytes(a, b, c, d) => primitive(_.setBytes(a, b, c, d))
        case SetBytes1(a, b) => primitive(_.setBytes(a, b))
        case Truncate(a) => primitive(_.truncate(a))
  
      }
  
    }

  /**
   * Syntax for `BlobIO`.
   * @group Algebra
   */
  implicit class BlobIOOps[A](ma: BlobIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Blob, A] =
      F.runFC[BlobOp,({type l[a]=Kleisli[M,Blob,a]})#l,A](ma)(kleisliTrans[M])
  }

}

