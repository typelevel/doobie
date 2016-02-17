package doobie.contrib.postgresql.free

import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import java.io.InputStream
import java.io.OutputStream
import java.lang.Class
import java.lang.Object
import java.lang.String
import org.postgresql.largeobject.LargeObject

import largeobject.LargeObjectIO

/**
 * Algebra and free monad for primitive operations over a `org.postgresql.largeobject.LargeObject`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `LargeObjectIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `LargeObjectOp` to another monad via
 * `Free#foldMap`. 
 *
 * The library provides a natural transformation to `Kleisli[M, LargeObject, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: LargeObjectIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: LargeObject = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object largeobject {
  
  /** 
   * Sum type of primitive operations over a `org.postgresql.largeobject.LargeObject`.
   * @group Algebra 
   */
  sealed trait LargeObjectOp[A]

  /** 
   * Module of constructors for `LargeObjectOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `largeobject` module.
   * @group Algebra 
   */
  object LargeObjectOp {
    
    // Lifting
    

    // Combinators
    case class Attempt[A](action: LargeObjectIO[A]) extends LargeObjectOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends LargeObjectOp[A]

    // Primitive Operations
    case object Close extends LargeObjectOp[Unit]
    case object Copy extends LargeObjectOp[LargeObject]
    case object GetInputStream extends LargeObjectOp[InputStream]
    case object GetLongOID extends LargeObjectOp[Long]
    case object GetOID extends LargeObjectOp[Int]
    case object GetOutputStream extends LargeObjectOp[OutputStream]
    case class  Read(a: Int) extends LargeObjectOp[Array[Byte]]
    case class  Read1(a: Array[Byte], b: Int, c: Int) extends LargeObjectOp[Int]
    case class  Seek(a: Int) extends LargeObjectOp[Unit]
    case class  Seek1(a: Int, b: Int) extends LargeObjectOp[Unit]
    case object Size extends LargeObjectOp[Int]
    case object Tell extends LargeObjectOp[Int]
    case class  Truncate(a: Int) extends LargeObjectOp[Unit]
    case class  Write(a: Array[Byte], b: Int, c: Int) extends LargeObjectOp[Unit]
    case class  Write1(a: Array[Byte]) extends LargeObjectOp[Unit]

  }
  import LargeObjectOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[LargeObjectOp]]; abstractly, a computation that consumes 
   * a `org.postgresql.largeobject.LargeObject` and produces a value of type `A`. 
   * @group Algebra 
   */
  type LargeObjectIO[A] = F[LargeObjectOp, A]

  /**
   * Catchable instance for [[LargeObjectIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableLargeObjectIO: Catchable[LargeObjectIO] =
    new Catchable[LargeObjectIO] {
      def attempt[A](f: LargeObjectIO[A]): LargeObjectIO[Throwable \/ A] = largeobject.attempt(f)
      def fail[A](err: Throwable): LargeObjectIO[A] = largeobject.delay(throw err)
    }

  /**
   * Capture instance for [[LargeObjectIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureLargeObjectIO: Capture[LargeObjectIO] =
    new Capture[LargeObjectIO] {
      def apply[A](a: => A): LargeObjectIO[A] = largeobject.delay(a)
    }

  

  /** 
   * Lift a LargeObjectIO[A] into an exception-capturing LargeObjectIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: LargeObjectIO[A]): LargeObjectIO[Throwable \/ A] =
    F.liftF[LargeObjectOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): LargeObjectIO[A] =
    F.liftF(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  val close: LargeObjectIO[Unit] =
    F.liftF(Close)

  /** 
   * @group Constructors (Primitives)
   */
  val copy: LargeObjectIO[LargeObject] =
    F.liftF(Copy)

  /** 
   * @group Constructors (Primitives)
   */
  val getInputStream: LargeObjectIO[InputStream] =
    F.liftF(GetInputStream)

  /** 
   * @group Constructors (Primitives)
   */
  val getLongOID: LargeObjectIO[Long] =
    F.liftF(GetLongOID)

  /** 
   * @group Constructors (Primitives)
   */
  val getOID: LargeObjectIO[Int] =
    F.liftF(GetOID)

  /** 
   * @group Constructors (Primitives)
   */
  val getOutputStream: LargeObjectIO[OutputStream] =
    F.liftF(GetOutputStream)

  /** 
   * @group Constructors (Primitives)
   */
  def read(a: Int): LargeObjectIO[Array[Byte]] =
    F.liftF(Read(a))

  /** 
   * @group Constructors (Primitives)
   */
  def read(a: Array[Byte], b: Int, c: Int): LargeObjectIO[Int] =
    F.liftF(Read1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def seek(a: Int): LargeObjectIO[Unit] =
    F.liftF(Seek(a))

  /** 
   * @group Constructors (Primitives)
   */
  def seek(a: Int, b: Int): LargeObjectIO[Unit] =
    F.liftF(Seek1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val size: LargeObjectIO[Int] =
    F.liftF(Size)

  /** 
   * @group Constructors (Primitives)
   */
  val tell: LargeObjectIO[Int] =
    F.liftF(Tell)

  /** 
   * @group Constructors (Primitives)
   */
  def truncate(a: Int): LargeObjectIO[Unit] =
    F.liftF(Truncate(a))

  /** 
   * @group Constructors (Primitives)
   */
  def write(a: Array[Byte], b: Int, c: Int): LargeObjectIO[Unit] =
    F.liftF(Write(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def write(a: Array[Byte]): LargeObjectIO[Unit] =
    F.liftF(Write1(a))

 /** 
  * Natural transformation from `LargeObjectOp` to `Kleisli` for the given `M`, consuming a `org.postgresql.largeobject.LargeObject`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: LargeObjectOp ~> ({type l[a] = Kleisli[M, LargeObject, a]})#l =
   new (LargeObjectOp ~> ({type l[a] = Kleisli[M, LargeObject, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: LargeObject => A): Kleisli[M, LargeObject, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: LargeObjectOp[A]): Kleisli[M, LargeObject, A] = 
       op match {

        // Lifting
        
  
        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.transK[M].attempt
  
        // Primitive Operations
        case Close => primitive(_.close)
        case Copy => primitive(_.copy)
        case GetInputStream => primitive(_.getInputStream)
        case GetLongOID => primitive(_.getLongOID)
        case GetOID => primitive(_.getOID)
        case GetOutputStream => primitive(_.getOutputStream)
        case Read(a) => primitive(_.read(a))
        case Read1(a, b, c) => primitive(_.read(a, b, c))
        case Seek(a) => primitive(_.seek(a))
        case Seek1(a, b) => primitive(_.seek(a, b))
        case Size => primitive(_.size)
        case Tell => primitive(_.tell)
        case Truncate(a) => primitive(_.truncate(a))
        case Write(a, b, c) => primitive(_.write(a, b, c))
        case Write1(a) => primitive(_.write(a))
  
      }
  
    }

  /**
   * Syntax for `LargeObjectIO`.
   * @group Algebra
   */
  implicit class LargeObjectIOOps[A](ma: LargeObjectIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, LargeObject, A] =
      ma.foldMap[Kleisli[M, LargeObject, ?]](kleisliTrans[M])
  }

}

