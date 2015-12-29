package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.syntax.catchable._
import scalaz.syntax.monad._
import scalaz.concurrent.Task

import doobie.util.capture._
import doobie.util.trace.{ Trace, TraceOp }
import doobie.free.kleislitrans._

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
  sealed trait ClobOp[A] extends TraceOp[Clob, A] {
 
    protected def primitive[M[_]: Monad: Capture](f: Clob => A): Kleisli[M, Clob, A] = 
      Kleisli((s: Clob) => Capture[M].apply(f(s)))

    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Clob, A]

    def defaultTransKL[M[_]: Monad: Catchable: Capture]: Kleisli[M, (Trace[M], Clob), A] =
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
   * Module of constructors for `ClobOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `clob` module.
   * @group Algebra 
   */
  object ClobOp {
    
    // This algebra has a default interpreter
    implicit val ClobKleisliTrans: KleisliTrans.Aux[ClobOp, Clob] =
      new KleisliTrans[ClobOp] {
        type J = Clob

        def interpK[M[_]: Monad: Catchable: Capture]: ClobOp ~> Kleisli[M, Clob, ?] =
          new (ClobOp ~> Kleisli[M, Clob, ?]) {
            def apply[A](op: ClobOp[A]): Kleisli[M, Clob, A] =
              op.defaultTransK[M]
          }

        def interpKL[M[_]: Monad: Catchable: Capture]: ClobOp ~> Kleisli[M, (Trace[M], Clob), ?] =
          new (ClobOp ~> Kleisli[M, (Trace[M], Clob), ?]) {
            def apply[A](op: ClobOp[A]): Kleisli[M, (Trace[M], Clob), A] =
              op.defaultTransKL[M]
          }

      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F.FreeC[Op, A], mod: KleisliTrans.Aux[Op, J]) extends ClobOp[A] {
  
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
    case class Attempt[A](action: ClobIO[A]) extends ClobOp[Throwable \/ A] {
      import scalaz._, Scalaz._

      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, Clob, ?]]].attempt(ClobKleisliTrans.transK[M].apply(action))

      override def defaultTransKL[M[_]: Monad: Catchable: Capture] =
        Kleisli { case (log, c) =>
          for {
            k <- log.log(c, this)
            x <- Predef.implicitly[Catchable[Kleisli[M, (Trace[M], Clob), ?]]].attempt(ClobKleisliTrans.transKL[M].apply(action)).run((log, c))
            _ <- k(\/-(x))
         } yield x
       }

    }

    case class Fail[A](t: Throwable) extends ClobOp[A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, Clob, ?]]].fail[A](t)
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
    case class  GetCharacterStream(a: Long, b: Long) extends ClobOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCharacterStream(a, b))
    }
    case object GetCharacterStream1 extends ClobOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCharacterStream())
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
    case class  SetString(a: Long, b: String) extends ClobOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setString(a, b))
    }
    case class  SetString1(a: Long, b: String, c: Int, d: Int) extends ClobOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setString(a, b, c, d))
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
      def fail[A](err: Throwable): ClobIO[A] = clob.fail(err)
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
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F.FreeC[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): ClobIO[A] =
    F.liftFC(Lift(j, action, mod))

  /** 
   * Lift a ClobIO[A] into an exception-capturing ClobIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: ClobIO[A]): ClobIO[Throwable \/ A] =
    F.liftFC[ClobOp, Throwable \/ A](Attempt(a))
 
  def fail[A](t: Throwable): ClobIO[A] =
    F.liftFC(Fail(t))
 
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
  def getCharacterStream(a: Long, b: Long): ClobIO[Reader] =
    F.liftFC(GetCharacterStream(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val getCharacterStream: ClobIO[Reader] =
    F.liftFC(GetCharacterStream1)

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
  def setString(a: Long, b: String): ClobIO[Int] =
    F.liftFC(SetString(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setString(a: Long, b: String, c: Int, d: Int): ClobIO[Int] =
    F.liftFC(SetString1(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def truncate(a: Long): ClobIO[Unit] =
    F.liftFC(Truncate(a))

}

