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
 * `Free#foldMap`.
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
object blob extends BlobIOInstances {

  /**
   * Sum type of primitive operations over a `java.sql.Blob`.
   * @group Algebra
   */
  sealed trait BlobOp[A] {
#+scalaz
    protected def primitive[M[_]: Monad: Capture](f: Blob => A): Kleisli[M, Blob, A] =
      Kleisli((s: Blob) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Blob, A]
#-scalaz
#+fs2
    protected def primitive[M[_]: Catchable: Suspendable](f: Blob => A): Kleisli[M, Blob, A] =
      Kleisli((s: Blob) => Predef.implicitly[Suspendable[M]].delay(f(s)))
    def defaultTransK[M[_]: Catchable: Suspendable]: Kleisli[M, Blob, A]
#-fs2
  }

  /**
   * Module of constructors for `BlobOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `blob` module.
   * @group Algebra
   */
  object BlobOp {

    // This algebra has a default interpreter
    implicit val BlobKleisliTrans: KleisliTrans.Aux[BlobOp, Blob] =
      new KleisliTrans[BlobOp] {
        type J = Blob
#+scalaz
        def interpK[M[_]: Monad: Catchable: Capture]: BlobOp ~> Kleisli[M, Blob, ?] =
#-scalaz
#+fs2
        def interpK[M[_]: Catchable: Suspendable]: BlobOp ~> Kleisli[M, Blob, ?] =
#-fs2
          new (BlobOp ~> Kleisli[M, Blob, ?]) {
            def apply[A](op: BlobOp[A]): Kleisli[M, Blob, A] =
              op.defaultTransK[M]
          }
      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F[Op, A], mod: KleisliTrans.Aux[Op, J]) extends BlobOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = Kleisli(_ => mod.transK[M].apply(action).run(j))
#-fs2
    }

    // Combinators
    case class Attempt[A](action: BlobIO[A]) extends BlobOp[Throwable \/ A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] =
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] =
#-fs2
        Predef.implicitly[Catchable[Kleisli[M, Blob, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends BlobOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_ => a())
#-fs2
    }
    case class Raw[A](f: Blob => A) extends BlobOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(f)
#-fs2
    }

    // Primitive Operations
#+scalaz
    case object Free extends BlobOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.free())
    }
    case object GetBinaryStream extends BlobOp[InputStream] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBinaryStream())
    }
    case class  GetBinaryStream1(a: Long, b: Long) extends BlobOp[InputStream] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBinaryStream(a, b))
    }
    case class  GetBytes(a: Long, b: Int) extends BlobOp[Array[Byte]] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBytes(a, b))
    }
    case object Length extends BlobOp[Long] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.length())
    }
    case class  Position(a: Array[Byte], b: Long) extends BlobOp[Long] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.position(a, b))
    }
    case class  Position1(a: Blob, b: Long) extends BlobOp[Long] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.position(a, b))
    }
    case class  SetBinaryStream(a: Long) extends BlobOp[OutputStream] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBinaryStream(a))
    }
    case class  SetBytes(a: Long, b: Array[Byte]) extends BlobOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBytes(a, b))
    }
    case class  SetBytes1(a: Long, b: Array[Byte], c: Int, d: Int) extends BlobOp[Int] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBytes(a, b, c, d))
    }
    case class  Truncate(a: Long) extends BlobOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.truncate(a))
    }
#-scalaz
#+fs2
    case object Free extends BlobOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.free())
    }
    case object GetBinaryStream extends BlobOp[InputStream] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getBinaryStream())
    }
    case class  GetBinaryStream1(a: Long, b: Long) extends BlobOp[InputStream] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getBinaryStream(a, b))
    }
    case class  GetBytes(a: Long, b: Int) extends BlobOp[Array[Byte]] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getBytes(a, b))
    }
    case object Length extends BlobOp[Long] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.length())
    }
    case class  Position(a: Array[Byte], b: Long) extends BlobOp[Long] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.position(a, b))
    }
    case class  Position1(a: Blob, b: Long) extends BlobOp[Long] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.position(a, b))
    }
    case class  SetBinaryStream(a: Long) extends BlobOp[OutputStream] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setBinaryStream(a))
    }
    case class  SetBytes(a: Long, b: Array[Byte]) extends BlobOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setBytes(a, b))
    }
    case class  SetBytes1(a: Long, b: Array[Byte], c: Int, d: Int) extends BlobOp[Int] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setBytes(a, b, c, d))
    }
    case class  Truncate(a: Long) extends BlobOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.truncate(a))
    }
#-fs2

  }
  import BlobOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[BlobOp]]; abstractly, a computation that consumes
   * a `java.sql.Blob` and produces a value of type `A`.
   * @group Algebra
   */
  type BlobIO[A] = F[BlobOp, A]

  /**
   * Catchable instance for [[BlobIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableBlobIO: Catchable[BlobIO] =
    new Catchable[BlobIO] {
#+fs2
      def pure[A](a: A): BlobIO[A] = blob.delay(a)
      override def map[A, B](fa: BlobIO[A])(f: A => B): BlobIO[B] = fa.map(f)
      def flatMap[A, B](fa: BlobIO[A])(f: A => BlobIO[B]): BlobIO[B] = fa.flatMap(f)
#-fs2
      def attempt[A](f: BlobIO[A]): BlobIO[Throwable \/ A] = blob.attempt(f)
      def fail[A](err: Throwable): BlobIO[A] = blob.delay(throw err)
    }

#+scalaz
  /**
   * Capture instance for [[BlobIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureBlobIO: Capture[BlobIO] =
    new Capture[BlobIO] {
      def apply[A](a: => A): BlobIO[A] = blob.delay(a)
    }
#-scalaz

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): BlobIO[A] =
    F.liftF(Lift(j, action, mod))

  /**
   * Lift a BlobIO[A] into an exception-capturing BlobIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: BlobIO[A]): BlobIO[Throwable \/ A] =
    F.liftF[BlobOp, Throwable \/ A](Attempt(a))

  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): BlobIO[A] =
    F.liftF(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying Blob.
   * @group Constructors (Lifting)
   */
  def raw[A](f: Blob => A): BlobIO[A] =
    F.liftF(Raw(f))

  /**
   * @group Constructors (Primitives)
   */
  val free: BlobIO[Unit] =
    F.liftF(Free)

  /**
   * @group Constructors (Primitives)
   */
  val getBinaryStream: BlobIO[InputStream] =
    F.liftF(GetBinaryStream)

  /**
   * @group Constructors (Primitives)
   */
  def getBinaryStream(a: Long, b: Long): BlobIO[InputStream] =
    F.liftF(GetBinaryStream1(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def getBytes(a: Long, b: Int): BlobIO[Array[Byte]] =
    F.liftF(GetBytes(a, b))

  /**
   * @group Constructors (Primitives)
   */
  val length: BlobIO[Long] =
    F.liftF(Length)

  /**
   * @group Constructors (Primitives)
   */
  def position(a: Array[Byte], b: Long): BlobIO[Long] =
    F.liftF(Position(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def position(a: Blob, b: Long): BlobIO[Long] =
    F.liftF(Position1(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: Long): BlobIO[OutputStream] =
    F.liftF(SetBinaryStream(a))

  /**
   * @group Constructors (Primitives)
   */
  def setBytes(a: Long, b: Array[Byte]): BlobIO[Int] =
    F.liftF(SetBytes(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def setBytes(a: Long, b: Array[Byte], c: Int, d: Int): BlobIO[Int] =
    F.liftF(SetBytes1(a, b, c, d))

  /**
   * @group Constructors (Primitives)
   */
  def truncate(a: Long): BlobIO[Unit] =
    F.liftF(Truncate(a))

 /**
  * Natural transformation from `BlobOp` to `Kleisli` for the given `M`, consuming a `java.sql.Blob`.
  * @group Algebra
  */
#+scalaz
  def interpK[M[_]: Monad: Catchable: Capture]: BlobOp ~> Kleisli[M, Blob, ?] =
   BlobOp.BlobKleisliTrans.interpK
#-scalaz
#+fs2
  def interpK[M[_]: Catchable: Suspendable]: BlobOp ~> Kleisli[M, Blob, ?] =
   BlobOp.BlobKleisliTrans.interpK
#-fs2

 /**
  * Natural transformation from `BlobIO` to `Kleisli` for the given `M`, consuming a `java.sql.Blob`.
  * @group Algebra
  */
#+scalaz
  def transK[M[_]: Monad: Catchable: Capture]: BlobIO ~> Kleisli[M, Blob, ?] =
   BlobOp.BlobKleisliTrans.transK
#-scalaz
#+fs2
  def transK[M[_]: Catchable: Suspendable]: BlobIO ~> Kleisli[M, Blob, ?] =
   BlobOp.BlobKleisliTrans.transK
#-fs2

 /**
  * Natural transformation from `BlobIO` to `M`, given a `java.sql.Blob`.
  * @group Algebra
  */
#+scalaz
 def trans[M[_]: Monad: Catchable: Capture](c: Blob): BlobIO ~> M =
   BlobOp.BlobKleisliTrans.trans[M](c)
#-scalaz
#+fs2
 def trans[M[_]: Catchable: Suspendable](c: Blob): BlobIO ~> M =
   BlobOp.BlobKleisliTrans.trans[M](c)
#-fs2

  /**
   * Syntax for `BlobIO`.
   * @group Algebra
   */
  implicit class BlobIOOps[A](ma: BlobIO[A]) {
#+scalaz
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Blob, A] =
      BlobOp.BlobKleisliTrans.transK[M].apply(ma)
#-scalaz
#+fs2
    def transK[M[_]: Catchable: Suspendable]: Kleisli[M, Blob, A] =
      BlobOp.BlobKleisliTrans.transK[M].apply(ma)
#-fs2
  }

}

private[free] trait BlobIOInstances {
#+fs2
  /**
   * Suspendable instance for [[BlobIO]].
   * @group Typeclass Instances
   */
  implicit val SuspendableBlobIO: Suspendable[BlobIO] =
    new Suspendable[BlobIO] {
      def pure[A](a: A): BlobIO[A] = blob.delay(a)
      override def map[A, B](fa: BlobIO[A])(f: A => B): BlobIO[B] = fa.map(f)
      def flatMap[A, B](fa: BlobIO[A])(f: A => BlobIO[B]): BlobIO[B] = fa.flatMap(f)
      def suspend[A](fa: => BlobIO[A]): BlobIO[A] = F.suspend(fa)
      override def delay[A](a: => A): BlobIO[A] = blob.delay(a)
    }
#-fs2
}

