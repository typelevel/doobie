package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.syntax.catchable._
import scalaz.syntax.monad._
import scalaz.concurrent.Task

import doobie.util.capture._
import doobie.util.trace.{ Trace, TraceOp }
import doobie.free.kleislitrans._

import java.lang.String
import java.sql.Blob
import java.sql.CallableStatement
import java.sql.Clob
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.Driver
import java.sql.DriverPropertyInfo
import java.sql.NClob
import java.sql.PreparedStatement
import java.sql.Ref
import java.sql.ResultSet
import java.sql.SQLData
import java.sql.SQLInput
import java.sql.SQLOutput
import java.sql.Statement
import java.util.Properties
import java.util.logging.Logger

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
 * Algebra and free monad for primitive operations over a `java.sql.Driver`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `DriverIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `DriverOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, Driver, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: DriverIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: Driver = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object driver {
  
  /** 
   * Sum type of primitive operations over a `java.sql.Driver`.
   * @group Algebra 
   */
  sealed trait DriverOp[A] extends TraceOp[Driver, A] {
 
    protected def primitive[M[_]: Monad: Capture](f: Driver => A): Kleisli[M, Driver, A] = 
      Kleisli((s: Driver) => Capture[M].apply(f(s)))

    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Driver, A]

    def defaultTransKL[M[_]: Monad: Catchable: Capture]: Kleisli[M, (Trace[M], Driver), A] =
      Kleisli { case (log, c) =>
        for {
          k <- log.log(c, this)
          x <- defaultTransK[M].attempt.run(c)
          _ <- k(x)
          a <- x.fold[M[A]](Catchable[M].fail(_), _.point[M])
        } yield a
      }

  }

  /** 
   * Module of constructors for `DriverOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `driver` module.
   * @group Algebra 
   */
  object DriverOp {
    
    // This algebra has a default interpreter
    implicit val DriverKleisliTrans: KleisliTrans.Aux[DriverOp, Driver] =
      new KleisliTrans[DriverOp] {
        type J = Driver

        def interpK[M[_]: Monad: Catchable: Capture]: DriverOp ~> Kleisli[M, Driver, ?] =
          new (DriverOp ~> Kleisli[M, Driver, ?]) {
            def apply[A](op: DriverOp[A]): Kleisli[M, Driver, A] =
              op.defaultTransK[M]
          }

        def interpKL[M[_]: Monad: Catchable: Capture]: DriverOp ~> Kleisli[M, (Trace[M], Driver), ?] =
          new (DriverOp ~> Kleisli[M, (Trace[M], Driver), ?]) {
            def apply[A](op: DriverOp[A]): Kleisli[M, (Trace[M], Driver), A] =
              op.defaultTransKL[M]
          }

      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F.FreeC[Op, A], mod: KleisliTrans.Aux[Op, J]) extends DriverOp[A] {
  
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Kleisli(_ => mod.transK[M].apply(action).run(j))

      override def defaultTransKL[M[_]: Monad: Catchable: Capture] =
        Kleisli { case (log, c) =>
          for {
            k <- log.log(c, this)
            x <- mod.transKL[M].apply(action).attempt.run((log, j))
            _ <- k(x)
            a <- x.fold[M[A]](Catchable[M].fail(_), _.point[M])
          } yield a
        }

    }

    // Combinators
    case class Attempt[A](action: DriverIO[A]) extends DriverOp[Throwable \/ A] {
      import scalaz._, Scalaz._

      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, Driver, ?]]].attempt(DriverKleisliTrans.transK[M].apply(action))

      override def defaultTransKL[M[_]: Monad: Catchable: Capture] =
        Kleisli { case (log, c) =>
          for {
            k <- log.log(c, this)
            x <- Predef.implicitly[Catchable[Kleisli[M, (Trace[M], Driver), ?]]].attempt(DriverKleisliTrans.transKL[M].apply(action)).run((log, c))
            _ <- k(\/-(x))
         } yield x
       }

    }

    case class Fail[A](t: Throwable) extends DriverOp[A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, Driver, ?]]].fail[A](t)
    }

    case class Pure[A](a: () => A) extends DriverOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: Driver => A) extends DriverOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case class  AcceptsURL(a: String) extends DriverOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.acceptsURL(a))
    }
    case class  Connect(a: String, b: Properties) extends DriverOp[Connection] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.connect(a, b))
    }
    case object GetMajorVersion extends DriverOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMajorVersion())
    }
    case object GetMinorVersion extends DriverOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMinorVersion())
    }
    case object GetParentLogger extends DriverOp[Logger] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getParentLogger())
    }
    case class  GetPropertyInfo(a: String, b: Properties) extends DriverOp[Array[DriverPropertyInfo]] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getPropertyInfo(a, b))
    }
    case object JdbcCompliant extends DriverOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.jdbcCompliant())
    }

  }
  import DriverOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[DriverOp]]; abstractly, a computation that consumes 
   * a `java.sql.Driver` and produces a value of type `A`. 
   * @group Algebra 
   */
  type DriverIO[A] = F.FreeC[DriverOp, A]

  /**
   * Monad instance for [[DriverIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadDriverIO: Monad[DriverIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[DriverOp, α]})#λ]

  /**
   * Catchable instance for [[DriverIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableDriverIO: Catchable[DriverIO] =
    new Catchable[DriverIO] {
      def attempt[A](f: DriverIO[A]): DriverIO[Throwable \/ A] = driver.attempt(f)
      def fail[A](err: Throwable): DriverIO[A] = driver.fail(err)
    }

  /**
   * Capture instance for [[DriverIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureDriverIO: Capture[DriverIO] =
    new Capture[DriverIO] {
      def apply[A](a: => A): DriverIO[A] = driver.delay(a)
    }

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F.FreeC[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): DriverIO[A] =
    F.liftFC(Lift(j, action, mod))

  /** 
   * Lift a DriverIO[A] into an exception-capturing DriverIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: DriverIO[A]): DriverIO[Throwable \/ A] =
    F.liftFC[DriverOp, Throwable \/ A](Attempt(a))
 
  def fail[A](t: Throwable): DriverIO[A] =
    F.liftFC(Fail(t))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): DriverIO[A] =
    F.liftFC(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying Driver.
   * @group Constructors (Lifting)
   */
  def raw[A](f: Driver => A): DriverIO[A] =
    F.liftFC(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  def acceptsURL(a: String): DriverIO[Boolean] =
    F.liftFC(AcceptsURL(a))

  /** 
   * @group Constructors (Primitives)
   */
  def connect(a: String, b: Properties): DriverIO[Connection] =
    F.liftFC(Connect(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val getMajorVersion: DriverIO[Int] =
    F.liftFC(GetMajorVersion)

  /** 
   * @group Constructors (Primitives)
   */
  val getMinorVersion: DriverIO[Int] =
    F.liftFC(GetMinorVersion)

  /** 
   * @group Constructors (Primitives)
   */
  val getParentLogger: DriverIO[Logger] =
    F.liftFC(GetParentLogger)

  /** 
   * @group Constructors (Primitives)
   */
  def getPropertyInfo(a: String, b: Properties): DriverIO[Array[DriverPropertyInfo]] =
    F.liftFC(GetPropertyInfo(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val jdbcCompliant: DriverIO[Boolean] =
    F.liftFC(JdbcCompliant)

}

