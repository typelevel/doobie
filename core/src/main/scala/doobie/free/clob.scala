package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.lang.String
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
 * Algebra and free monad for primitive operations over a `java.sql.Clob`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `ClobIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `ClobOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, Clob, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: ClobIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: Clob = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object clob {
  
  /** 
   * Sum type of primitive operations over a `java.sql.Clob`.
   * @group Algebra 
   */
  sealed trait ClobOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: Clob => A): Kleisli[M, Clob, A] = 
      Kleisli((s: Clob) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Clob, A]
  }

  /** 
   * Module of constructors for `ClobOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `clob` module.
   * @group Algebra 
   */
  object ClobOp {
    
    // Lifting
    case class LiftBlobIO[A](s: Blob, action: BlobIO[A]) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftCallableStatementIO[A](s: CallableStatement, action: CallableStatementIO[A]) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftConnectionIO[A](s: Connection, action: ConnectionIO[A]) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDatabaseMetaDataIO[A](s: DatabaseMetaData, action: DatabaseMetaDataIO[A]) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDriverIO[A](s: Driver, action: DriverIO[A]) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftNClobIO[A](s: NClob, action: NClobIO[A]) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftPreparedStatementIO[A](s: PreparedStatement, action: PreparedStatementIO[A]) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftRefIO[A](s: Ref, action: RefIO[A]) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftResultSetIO[A](s: ResultSet, action: ResultSetIO[A]) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLDataIO[A](s: SQLData, action: SQLDataIO[A]) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLInputIO[A](s: SQLInput, action: SQLInputIO[A]) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLOutputIO[A](s: SQLOutput, action: SQLOutputIO[A]) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftStatementIO[A](s: Statement, action: StatementIO[A]) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }

    // Combinators
    case class Attempt[A](action: ClobIO[A]) extends ClobOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, Clob, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: Clob => A) extends ClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case object Free extends ClobOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.free())
    }
    case object GetAsciiStream extends ClobOp[InputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getAsciiStream())
    }
    case object GetCharacterStream extends ClobOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCharacterStream())
    }
    case class  GetCharacterStream1(a: Long, b: Long) extends ClobOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCharacterStream(a, b))
    }
    case class  GetSubString(a: Long, b: Int) extends ClobOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSubString(a, b))
    }
    case object Length extends ClobOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.length())
    }
    case class  Position(a: Clob, b: Long) extends ClobOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.position(a, b))
    }
    case class  Position1(a: String, b: Long) extends ClobOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.position(a, b))
    }
    case class  SetAsciiStream(a: Long) extends ClobOp[OutputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a))
    }
    case class  SetCharacterStream(a: Long) extends ClobOp[Writer] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a))
    }
    case class  SetString(a: Long, b: String, c: Int, d: Int) extends ClobOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setString(a, b, c, d))
    }
    case class  SetString1(a: Long, b: String) extends ClobOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setString(a, b))
    }
    case class  Truncate(a: Long) extends ClobOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.truncate(a))
    }

  }
  import ClobOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[ClobOp]]; abstractly, a computation that consumes 
   * a `java.sql.Clob` and produces a value of type `A`. 
   * @group Algebra 
   */
  type ClobIO[A] = F.FreeC[ClobOp, A]

  /**
   * Monad instance for [[ClobIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadClobIO: Monad[ClobIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[ClobOp, α]})#λ]

  /**
   * Catchable instance for [[ClobIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableClobIO: Catchable[ClobIO] =
    new Catchable[ClobIO] {
      def attempt[A](f: ClobIO[A]): ClobIO[Throwable \/ A] = clob.attempt(f)
      def fail[A](err: Throwable): ClobIO[A] = clob.delay(throw err)
    }

  /**
   * Capture instance for [[ClobIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureClobIO: Capture[ClobIO] =
    new Capture[ClobIO] {
      def apply[A](a: => A): ClobIO[A] = clob.delay(a)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftBlob[A](s: Blob, k: BlobIO[A]): ClobIO[A] =
    F.liftFC(LiftBlobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftCallableStatement[A](s: CallableStatement, k: CallableStatementIO[A]): ClobIO[A] =
    F.liftFC(LiftCallableStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftConnection[A](s: Connection, k: ConnectionIO[A]): ClobIO[A] =
    F.liftFC(LiftConnectionIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDatabaseMetaData[A](s: DatabaseMetaData, k: DatabaseMetaDataIO[A]): ClobIO[A] =
    F.liftFC(LiftDatabaseMetaDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDriver[A](s: Driver, k: DriverIO[A]): ClobIO[A] =
    F.liftFC(LiftDriverIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftNClob[A](s: NClob, k: NClobIO[A]): ClobIO[A] =
    F.liftFC(LiftNClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftPreparedStatement[A](s: PreparedStatement, k: PreparedStatementIO[A]): ClobIO[A] =
    F.liftFC(LiftPreparedStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftRef[A](s: Ref, k: RefIO[A]): ClobIO[A] =
    F.liftFC(LiftRefIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftResultSet[A](s: ResultSet, k: ResultSetIO[A]): ClobIO[A] =
    F.liftFC(LiftResultSetIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLData[A](s: SQLData, k: SQLDataIO[A]): ClobIO[A] =
    F.liftFC(LiftSQLDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLInput[A](s: SQLInput, k: SQLInputIO[A]): ClobIO[A] =
    F.liftFC(LiftSQLInputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLOutput[A](s: SQLOutput, k: SQLOutputIO[A]): ClobIO[A] =
    F.liftFC(LiftSQLOutputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftStatement[A](s: Statement, k: StatementIO[A]): ClobIO[A] =
    F.liftFC(LiftStatementIO(s, k))

  /** 
   * Lift a ClobIO[A] into an exception-capturing ClobIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: ClobIO[A]): ClobIO[Throwable \/ A] =
    F.liftFC[ClobOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): ClobIO[A] =
    F.liftFC(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying Clob.
   * @group Constructors (Lifting)
   */
  def raw[A](f: Clob => A): ClobIO[A] =
    F.liftFC(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  val free: ClobIO[Unit] =
    F.liftFC(Free)

  /** 
   * @group Constructors (Primitives)
   */
  val getAsciiStream: ClobIO[InputStream] =
    F.liftFC(GetAsciiStream)

  /** 
   * @group Constructors (Primitives)
   */
  val getCharacterStream: ClobIO[Reader] =
    F.liftFC(GetCharacterStream)

  /** 
   * @group Constructors (Primitives)
   */
  def getCharacterStream(a: Long, b: Long): ClobIO[Reader] =
    F.liftFC(GetCharacterStream1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getSubString(a: Long, b: Int): ClobIO[String] =
    F.liftFC(GetSubString(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val length: ClobIO[Long] =
    F.liftFC(Length)

  /** 
   * @group Constructors (Primitives)
   */
  def position(a: Clob, b: Long): ClobIO[Long] =
    F.liftFC(Position(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def position(a: String, b: Long): ClobIO[Long] =
    F.liftFC(Position1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: Long): ClobIO[OutputStream] =
    F.liftFC(SetAsciiStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: Long): ClobIO[Writer] =
    F.liftFC(SetCharacterStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setString(a: Long, b: String, c: Int, d: Int): ClobIO[Int] =
    F.liftFC(SetString(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def setString(a: Long, b: String): ClobIO[Int] =
    F.liftFC(SetString1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def truncate(a: Long): ClobIO[Unit] =
    F.liftFC(Truncate(a))

 /** 
  * Natural transformation from `ClobOp` to `Kleisli` for the given `M`, consuming a `java.sql.Clob`. 
  * @group Algebra
  */
  def kleisliTrans[M[_]: Monad: Catchable: Capture]: ClobOp ~> Kleisli[M, Clob, ?] =
    new (ClobOp ~> Kleisli[M, Clob, ?]) {
      def apply[A](op: ClobOp[A]): Kleisli[M, Clob, A] =
        op.defaultTransK[M]
    }

  /**
   * Syntax for `ClobIO`.
   * @group Algebra
   */
  implicit class ClobIOOps[A](ma: ClobIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Clob, A] =
      F.runFC[ClobOp, Kleisli[M, Clob, ?], A](ma)(kleisliTrans[M])
  }

}

