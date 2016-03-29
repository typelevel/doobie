package doobie.free

import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

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
object blob {
  
  /** 
   * Sum type of primitive operations over a `java.sql.Blob`.
   * @group Algebra 
   */
  sealed trait BlobOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: Blob => A): Kleisli[M, Blob, A] = 
      Kleisli((s: Blob) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Blob, A]
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
        def interpK[M[_]: Monad: Catchable: Capture]: BlobOp ~> Kleisli[M, Blob, ?] =
          new (BlobOp ~> Kleisli[M, Blob, ?]) {
            def apply[A](op: BlobOp[A]): Kleisli[M, Blob, A] =
              op.defaultTransK[M]
          }
      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F[Op, A], mod: KleisliTrans.Aux[Op, J]) extends BlobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
    }

    // Combinators
    case class Attempt[A](action: BlobIO[A]) extends BlobOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, Blob, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends BlobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: Blob => A) extends BlobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case object Free extends BlobOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.free())
    }
    case object GetBinaryStream extends BlobOp[InputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBinaryStream())
    }
    case class  GetBinaryStream1(a: Long, b: Long) extends BlobOp[InputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBinaryStream(a, b))
    }
    case class  GetBytes(a: Long, b: Int) extends BlobOp[Array[Byte]] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBytes(a, b))
    }
    case object Length extends BlobOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.length())
    }
    case class  Position(a: Blob, b: Long) extends BlobOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.position(a, b))
    }
    case class  Position1(a: Array[Byte], b: Long) extends BlobOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.position(a, b))
    }
    case class  SetBinaryStream(a: Long) extends BlobOp[OutputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBinaryStream(a))
    }
    case class  SetBytes(a: Long, b: Array[Byte], c: Int, d: Int) extends BlobOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBytes(a, b, c, d))
    }
    case class  SetBytes1(a: Long, b: Array[Byte]) extends BlobOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setBytes(a, b))
    }
    case class  Truncate(a: Long) extends BlobOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.truncate(a))
    }

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
      def attempt[A](f: BlobIO[A]): BlobIO[Throwable \/ A] = blob.attempt(f)
      def fail[A](err: Throwable): BlobIO[A] = blob.delay(throw err)
    }

  /**
   * Capture instance for [[BlobIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureBlobIO: Capture[BlobIO] =
    new Capture[BlobIO] {
      def apply[A](a: => A): BlobIO[A] = blob.delay(a)
    }

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
  def position(a: Blob, b: Long): BlobIO[Long] =
    F.liftF(Position(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def position(a: Array[Byte], b: Long): BlobIO[Long] =
    F.liftF(Position1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setBinaryStream(a: Long): BlobIO[OutputStream] =
    F.liftF(SetBinaryStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setBytes(a: Long, b: Array[Byte], c: Int, d: Int): BlobIO[Int] =
    F.liftF(SetBytes(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def setBytes(a: Long, b: Array[Byte]): BlobIO[Int] =
    F.liftF(SetBytes1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def truncate(a: Long): BlobIO[Unit] =
    F.liftF(Truncate(a))

 /** 
  * Natural transformation from `BlobOp` to `Kleisli` for the given `M`, consuming a `java.sql.Blob`. 
  * @group Algebra
  */
  def interpK[M[_]: Monad: Catchable: Capture]: BlobOp ~> Kleisli[M, Blob, ?] =
   BlobOp.BlobKleisliTrans.interpK

 /** 
  * Natural transformation from `BlobIO` to `Kleisli` for the given `M`, consuming a `java.sql.Blob`. 
  * @group Algebra
  */
  def transK[M[_]: Monad: Catchable: Capture]: BlobIO ~> Kleisli[M, Blob, ?] =
   BlobOp.BlobKleisliTrans.transK

 /** 
  * Natural transformation from `BlobIO` to `M`, given a `java.sql.Blob`. 
  * @group Algebra
  */
 def trans[M[_]: Monad: Catchable: Capture](c: Blob): BlobIO ~> M =
   BlobOp.BlobKleisliTrans.trans[M](c)

  /**
   * Syntax for `BlobIO`.
   * @group Algebra
   */
  implicit class BlobIOOps[A](ma: BlobIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Blob, A] =
      BlobOp.BlobKleisliTrans.transK[M].apply(ma)
  }

}

