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
 * Algebra and free monad for primitive operations over a `java.sql.NClob`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `NClobIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `NClobOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, NClob, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: NClobIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: NClob = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object nclob {
  
  /** 
   * Sum type of primitive operations over a `java.sql.NClob`.
   * @group Algebra 
   */
  sealed trait NClobOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: NClob => A): Kleisli[M, NClob, A] = 
      Kleisli((s: NClob) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, NClob, A]
  }

  /** 
   * Module of constructors for `NClobOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `nclob` module.
   * @group Algebra 
   */
  object NClobOp {
    
    // Lifting
    case class LiftBlobIO[A](s: Blob, action: BlobIO[A]) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftCallableStatementIO[A](s: CallableStatement, action: CallableStatementIO[A]) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftClobIO[A](s: Clob, action: ClobIO[A]) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftConnectionIO[A](s: Connection, action: ConnectionIO[A]) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDatabaseMetaDataIO[A](s: DatabaseMetaData, action: DatabaseMetaDataIO[A]) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDriverIO[A](s: Driver, action: DriverIO[A]) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftPreparedStatementIO[A](s: PreparedStatement, action: PreparedStatementIO[A]) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftRefIO[A](s: Ref, action: RefIO[A]) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftResultSetIO[A](s: ResultSet, action: ResultSetIO[A]) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLDataIO[A](s: SQLData, action: SQLDataIO[A]) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLInputIO[A](s: SQLInput, action: SQLInputIO[A]) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLOutputIO[A](s: SQLOutput, action: SQLOutputIO[A]) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftStatementIO[A](s: Statement, action: StatementIO[A]) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }

    // Combinators
    case class Attempt[A](action: NClobIO[A]) extends NClobOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, NClob, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: NClob => A) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case object Free extends NClobOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.free())
    }
    case object GetAsciiStream extends NClobOp[InputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getAsciiStream())
    }
    case object GetCharacterStream extends NClobOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCharacterStream())
    }
    case class  GetCharacterStream1(a: Long, b: Long) extends NClobOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCharacterStream(a, b))
    }
    case class  GetSubString(a: Long, b: Int) extends NClobOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSubString(a, b))
    }
    case object Length extends NClobOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.length())
    }
    case class  Position(a: Clob, b: Long) extends NClobOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.position(a, b))
    }
    case class  Position1(a: String, b: Long) extends NClobOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.position(a, b))
    }
    case class  SetAsciiStream(a: Long) extends NClobOp[OutputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a))
    }
    case class  SetCharacterStream(a: Long) extends NClobOp[Writer] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a))
    }
    case class  SetString(a: Long, b: String, c: Int, d: Int) extends NClobOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setString(a, b, c, d))
    }
    case class  SetString1(a: Long, b: String) extends NClobOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setString(a, b))
    }
    case class  Truncate(a: Long) extends NClobOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.truncate(a))
    }

  }
  import NClobOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[NClobOp]]; abstractly, a computation that consumes 
   * a `java.sql.NClob` and produces a value of type `A`. 
   * @group Algebra 
   */
  type NClobIO[A] = F.FreeC[NClobOp, A]

  /**
   * Monad instance for [[NClobIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadNClobIO: Monad[NClobIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[NClobOp, α]})#λ]

  /**
   * Catchable instance for [[NClobIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableNClobIO: Catchable[NClobIO] =
    new Catchable[NClobIO] {
      def attempt[A](f: NClobIO[A]): NClobIO[Throwable \/ A] = nclob.attempt(f)
      def fail[A](err: Throwable): NClobIO[A] = nclob.delay(throw err)
    }

  /**
   * Capture instance for [[NClobIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureNClobIO: Capture[NClobIO] =
    new Capture[NClobIO] {
      def apply[A](a: => A): NClobIO[A] = nclob.delay(a)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftBlob[A](s: Blob, k: BlobIO[A]): NClobIO[A] =
    F.liftFC(LiftBlobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftCallableStatement[A](s: CallableStatement, k: CallableStatementIO[A]): NClobIO[A] =
    F.liftFC(LiftCallableStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftClob[A](s: Clob, k: ClobIO[A]): NClobIO[A] =
    F.liftFC(LiftClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftConnection[A](s: Connection, k: ConnectionIO[A]): NClobIO[A] =
    F.liftFC(LiftConnectionIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDatabaseMetaData[A](s: DatabaseMetaData, k: DatabaseMetaDataIO[A]): NClobIO[A] =
    F.liftFC(LiftDatabaseMetaDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDriver[A](s: Driver, k: DriverIO[A]): NClobIO[A] =
    F.liftFC(LiftDriverIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftPreparedStatement[A](s: PreparedStatement, k: PreparedStatementIO[A]): NClobIO[A] =
    F.liftFC(LiftPreparedStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftRef[A](s: Ref, k: RefIO[A]): NClobIO[A] =
    F.liftFC(LiftRefIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftResultSet[A](s: ResultSet, k: ResultSetIO[A]): NClobIO[A] =
    F.liftFC(LiftResultSetIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLData[A](s: SQLData, k: SQLDataIO[A]): NClobIO[A] =
    F.liftFC(LiftSQLDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLInput[A](s: SQLInput, k: SQLInputIO[A]): NClobIO[A] =
    F.liftFC(LiftSQLInputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLOutput[A](s: SQLOutput, k: SQLOutputIO[A]): NClobIO[A] =
    F.liftFC(LiftSQLOutputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftStatement[A](s: Statement, k: StatementIO[A]): NClobIO[A] =
    F.liftFC(LiftStatementIO(s, k))

  /** 
   * Lift a NClobIO[A] into an exception-capturing NClobIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: NClobIO[A]): NClobIO[Throwable \/ A] =
    F.liftFC[NClobOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): NClobIO[A] =
    F.liftFC(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying NClob.
   * @group Constructors (Lifting)
   */
  def raw[A](f: NClob => A): NClobIO[A] =
    F.liftFC(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  val free: NClobIO[Unit] =
    F.liftFC(Free)

  /** 
   * @group Constructors (Primitives)
   */
  val getAsciiStream: NClobIO[InputStream] =
    F.liftFC(GetAsciiStream)

  /** 
   * @group Constructors (Primitives)
   */
  val getCharacterStream: NClobIO[Reader] =
    F.liftFC(GetCharacterStream)

  /** 
   * @group Constructors (Primitives)
   */
  def getCharacterStream(a: Long, b: Long): NClobIO[Reader] =
    F.liftFC(GetCharacterStream1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getSubString(a: Long, b: Int): NClobIO[String] =
    F.liftFC(GetSubString(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val length: NClobIO[Long] =
    F.liftFC(Length)

  /** 
   * @group Constructors (Primitives)
   */
  def position(a: Clob, b: Long): NClobIO[Long] =
    F.liftFC(Position(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def position(a: String, b: Long): NClobIO[Long] =
    F.liftFC(Position1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: Long): NClobIO[OutputStream] =
    F.liftFC(SetAsciiStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: Long): NClobIO[Writer] =
    F.liftFC(SetCharacterStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setString(a: Long, b: String, c: Int, d: Int): NClobIO[Int] =
    F.liftFC(SetString(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def setString(a: Long, b: String): NClobIO[Int] =
    F.liftFC(SetString1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def truncate(a: Long): NClobIO[Unit] =
    F.liftFC(Truncate(a))

 /** 
  * Natural transformation from `NClobOp` to `Kleisli` for the given `M`, consuming a `java.sql.NClob`. 
  * @group Algebra
  */
  def kleisliTrans[M[_]: Monad: Catchable: Capture]: NClobOp ~> Kleisli[M, NClob, ?] =
    new (NClobOp ~> Kleisli[M, NClob, ?]) {
      def apply[A](op: NClobOp[A]): Kleisli[M, NClob, A] =
        op.defaultTransK[M]
    }

  /**
   * Syntax for `NClobIO`.
   * @group Algebra
   */
  implicit class NClobIOOps[A](ma: NClobIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, NClob, A] =
      F.runFC[NClobOp, Kleisli[M, NClob, ?], A](ma)(kleisliTrans[M])
  }

}

