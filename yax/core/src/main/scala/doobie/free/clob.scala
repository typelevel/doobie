package doobie.free

#+scalaz
import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
#-scalaz
#+cats
import cats.~>
import cats.data.Kleisli
import cats.free.{ Free => F }
import scala.util.{ Either => \/ }
#-cats
#+fs2
import fs2.util.{ Catchable, Suspendable }
import fs2.interop.cats._
#-fs2

import doobie.util.capture._
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
 * `Free#foldMap`.
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
object clob extends ClobIOInstances {

  /**
   * Sum type of primitive operations over a `java.sql.Clob`.
   * @group Algebra
   */
  sealed trait ClobOp[A] {
#+scalaz
    protected def primitive[M[_]: Monad: Capture](f: Clob => A): Kleisli[M, Clob, A] =
      Kleisli((s: Clob) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Clob, A]
#-scalaz
#+fs2
    protected def primitive[M[_]: Suspendable](f: Clob => A): Kleisli[M, Clob, A] =
      Kleisli((s: Clob) => Predef.implicitly[Suspendable[M]].delay(f(s)))
    def defaultTransK[M[_]: Catchable: Suspendable]: Kleisli[M, Clob, A]
#-fs2
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
#+scalaz
        def interpK[M[_]: Monad: Catchable: Capture]: ClobOp ~> Kleisli[M, Clob, ?] =
#-scalaz
#+fs2
        def interpK[M[_]: Catchable: Suspendable]: ClobOp ~> Kleisli[M, Clob, ?] =
#-fs2
          new (ClobOp ~> Kleisli[M, Clob, ?]) {
            def apply[A](op: ClobOp[A]): Kleisli[M, Clob, A] =
              op.defaultTransK[M]
          }
      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F[Op, A], mod: KleisliTrans.Aux[Op, J]) extends ClobOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = Kleisli(_ => mod.transK[M].apply(action).run(j))
#-fs2
    }

    // Combinators
    case class Attempt[A](action: ClobIO[A]) extends ClobOp[Throwable \/ A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] =
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] =
#-fs2
        Predef.implicitly[Catchable[Kleisli[M, Clob, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends ClobOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_ => a())
#-fs2
    }
    case class Raw[A](f: Clob => A) extends ClobOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(f)
#-fs2
    }

    // Primitive Operations
#+scalaz
    case object Free extends ClobOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.free())
    }
    case object GetAsciiStream extends ClobOp[InputStream] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getAsciiStream())
    }
    case object GetCharacterStream extends ClobOp[Reader] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCharacterStream())
    }
    case class  GetCharacterStream1(a: Long, b: Long) extends ClobOp[Reader] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCharacterStream(a, b))
    }
    case class  GetSubString(a: Long, b: Int) extends ClobOp[String] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSubString(a, b))
    }
    case object Length extends ClobOp[Long] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.length())
    }
    case class  Position(a: Clob, b: Long) extends ClobOp[Long] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.position(a, b))
    }
    case class  Position1(a: String, b: Long) extends ClobOp[Long] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.position(a, b))
    }
    case class  SetAsciiStream(a: Long) extends ClobOp[OutputStream] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a))
    }
    case class  SetCharacterStream(a: Long) extends ClobOp[Writer] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a))
    }
    case class  SetString(a: Long, b: String) extends ClobOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setString(a, b))
    }
    case class  SetString1(a: Long, b: String, c: Int, d: Int) extends ClobOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setString(a, b, c, d))
    }
    case class  Truncate(a: Long) extends ClobOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.truncate(a))
    }
#-scalaz
#+fs2
    case object Free extends ClobOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.free())
    }
    case object GetAsciiStream extends ClobOp[InputStream] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getAsciiStream())
    }
    case object GetCharacterStream extends ClobOp[Reader] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getCharacterStream())
    }
    case class  GetCharacterStream1(a: Long, b: Long) extends ClobOp[Reader] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getCharacterStream(a, b))
    }
    case class  GetSubString(a: Long, b: Int) extends ClobOp[String] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getSubString(a, b))
    }
    case object Length extends ClobOp[Long] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.length())
    }
    case class  Position(a: Clob, b: Long) extends ClobOp[Long] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.position(a, b))
    }
    case class  Position1(a: String, b: Long) extends ClobOp[Long] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.position(a, b))
    }
    case class  SetAsciiStream(a: Long) extends ClobOp[OutputStream] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setAsciiStream(a))
    }
    case class  SetCharacterStream(a: Long) extends ClobOp[Writer] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setCharacterStream(a))
    }
    case class  SetString(a: Long, b: String) extends ClobOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setString(a, b))
    }
    case class  SetString1(a: Long, b: String, c: Int, d: Int) extends ClobOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setString(a, b, c, d))
    }
    case class  Truncate(a: Long) extends ClobOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.truncate(a))
    }
#-fs2

  }
  import ClobOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[ClobOp]]; abstractly, a computation that consumes
   * a `java.sql.Clob` and produces a value of type `A`.
   * @group Algebra
   */
  type ClobIO[A] = F[ClobOp, A]

  /**
   * Catchable instance for [[ClobIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableClobIO: Catchable[ClobIO] =
    new Catchable[ClobIO] {
#+fs2
      def pure[A](a: A): ClobIO[A] = clob.delay(a)
      override def map[A, B](fa: ClobIO[A])(f: A => B): ClobIO[B] = fa.map(f)
      def flatMap[A, B](fa: ClobIO[A])(f: A => ClobIO[B]): ClobIO[B] = fa.flatMap(f)
#-fs2
      def attempt[A](f: ClobIO[A]): ClobIO[Throwable \/ A] = clob.attempt(f)
      def fail[A](err: Throwable): ClobIO[A] = clob.delay(throw err)
    }

#+scalaz
  /**
   * Capture instance for [[ClobIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureClobIO: Capture[ClobIO] =
    new Capture[ClobIO] {
      def apply[A](a: => A): ClobIO[A] = clob.delay(a)
    }
#-scalaz

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): ClobIO[A] =
    F.liftF[ClobOp, A](Lift(j, action, mod))

  /**
   * Lift a ClobIO[A] into an exception-capturing ClobIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: ClobIO[A]): ClobIO[Throwable \/ A] =
    F.liftF[ClobOp, Throwable \/ A](Attempt(a))

  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): ClobIO[A] =
    F.liftF(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying Clob.
   * @group Constructors (Lifting)
   */
  def raw[A](f: Clob => A): ClobIO[A] =
    F.liftF(Raw(f))

  /**
   * @group Constructors (Primitives)
   */
  val free: ClobIO[Unit] =
    F.liftF(Free)

  /**
   * @group Constructors (Primitives)
   */
  val getAsciiStream: ClobIO[InputStream] =
    F.liftF(GetAsciiStream)

  /**
   * @group Constructors (Primitives)
   */
  val getCharacterStream: ClobIO[Reader] =
    F.liftF(GetCharacterStream)

  /**
   * @group Constructors (Primitives)
   */
  def getCharacterStream(a: Long, b: Long): ClobIO[Reader] =
    F.liftF(GetCharacterStream1(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def getSubString(a: Long, b: Int): ClobIO[String] =
    F.liftF(GetSubString(a, b))

  /**
   * @group Constructors (Primitives)
   */
  val length: ClobIO[Long] =
    F.liftF(Length)

  /**
   * @group Constructors (Primitives)
   */
  def position(a: Clob, b: Long): ClobIO[Long] =
    F.liftF(Position(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def position(a: String, b: Long): ClobIO[Long] =
    F.liftF(Position1(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: Long): ClobIO[OutputStream] =
    F.liftF(SetAsciiStream(a))

  /**
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: Long): ClobIO[Writer] =
    F.liftF(SetCharacterStream(a))

  /**
   * @group Constructors (Primitives)
   */
  def setString(a: Long, b: String): ClobIO[Int] =
    F.liftF(SetString(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setString(a: Long, b: String, c: Int, d: Int): ClobIO[Int] =
    F.liftF(SetString1(a, b, c, d))

  /**
   * @group Constructors (Primitives)
   */
  def truncate(a: Long): ClobIO[Unit] =
    F.liftF(Truncate(a))

 /**
  * Natural transformation from `ClobOp` to `Kleisli` for the given `M`, consuming a `java.sql.Clob`.
  * @group Algebra
  */
#+scalaz
  def interpK[M[_]: Monad: Catchable: Capture]: ClobOp ~> Kleisli[M, Clob, ?] =
   ClobOp.ClobKleisliTrans.interpK
#-scalaz
#+fs2
  def interpK[M[_]: Catchable: Suspendable]: ClobOp ~> Kleisli[M, Clob, ?] =
   ClobOp.ClobKleisliTrans.interpK
#-fs2

 /**
  * Natural transformation from `ClobIO` to `Kleisli` for the given `M`, consuming a `java.sql.Clob`.
  * @group Algebra
  */
#+scalaz
  def transK[M[_]: Monad: Catchable: Capture]: ClobIO ~> Kleisli[M, Clob, ?] =
   ClobOp.ClobKleisliTrans.transK
#-scalaz
#+fs2
  def transK[M[_]: Catchable: Suspendable]: ClobIO ~> Kleisli[M, Clob, ?] =
   ClobOp.ClobKleisliTrans.transK
#-fs2

 /**
  * Natural transformation from `ClobIO` to `M`, given a `java.sql.Clob`.
  * @group Algebra
  */
#+scalaz
 def trans[M[_]: Monad: Catchable: Capture](c: Clob): ClobIO ~> M =
#-scalaz
#+fs2
 def trans[M[_]: Catchable: Suspendable](c: Clob): ClobIO ~> M =
#-fs2
   ClobOp.ClobKleisliTrans.trans[M](c)

  /**
   * Syntax for `ClobIO`.
   * @group Algebra
   */
  implicit class ClobIOOps[A](ma: ClobIO[A]) {
#+scalaz
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Clob, A] =
#-scalaz
#+fs2
    def transK[M[_]: Catchable: Suspendable]: Kleisli[M, Clob, A] =
#-fs2
      ClobOp.ClobKleisliTrans.transK[M].apply(ma)
  }

}

private[free] trait ClobIOInstances {
#+fs2
  /**
   * Suspendable instance for [[ClobIO]].
   * @group Typeclass Instances
   */
  implicit val SuspendableClobIO: Suspendable[ClobIO] =
    new Suspendable[ClobIO] {
      def pure[A](a: A): ClobIO[A] = clob.delay(a)
      override def map[A, B](fa: ClobIO[A])(f: A => B): ClobIO[B] = fa.map(f)
      def flatMap[A, B](fa: ClobIO[A])(f: A => ClobIO[B]): ClobIO[B] = fa.flatMap(f)
      def suspend[A](fa: => ClobIO[A]): ClobIO[A] = F.suspend(fa)
      override def delay[A](a: => A): ClobIO[A] = clob.delay(a)
    }
#-fs2
}

