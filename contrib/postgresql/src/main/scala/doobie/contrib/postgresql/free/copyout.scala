package doobie.contrib.postgresql.free

import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import org.postgresql.copy.CopyIn
import org.postgresql.copy.CopyOut

import copyin.CopyInIO
import copyout.CopyOutIO

/**
 * Algebra and free monad for primitive operations over a `org.postgresql.copy.CopyOut`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `CopyOutIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `CopyOutOp` to another monad via
 * `Free#foldMap`.
 *
 * The library provides a natural transformation to `Kleisli[M, CopyOut, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: CopyOutIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: CopyOut = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object copyout {
  
  /** 
   * Sum type of primitive operations over a `org.postgresql.copy.CopyOut`.
   * @group Algebra 
   */
  sealed trait CopyOutOp[A]

  /** 
   * Module of constructors for `CopyOutOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `copyout` module.
   * @group Algebra 
   */
  object CopyOutOp {
    
    // Lifting
    case class LiftCopyInIO[A](s: CopyIn, action: CopyInIO[A]) extends CopyOutOp[A]

    // Combinators
    case class Attempt[A](action: CopyOutIO[A]) extends CopyOutOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends CopyOutOp[A]

    // Primitive Operations
    case object CancelCopy extends CopyOutOp[Unit]
    case object GetFieldCount extends CopyOutOp[Int]
    case class  GetFieldFormat(a: Int) extends CopyOutOp[Int]
    case object GetFormat extends CopyOutOp[Int]
    case object GetHandledRowCount extends CopyOutOp[Long]
    case object IsActive extends CopyOutOp[Boolean]
    case object ReadFromCopy extends CopyOutOp[Array[Byte]]

  }
  import CopyOutOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[CopyOutOp]]; abstractly, a computation that consumes 
   * a `org.postgresql.copy.CopyOut` and produces a value of type `A`. 
   * @group Algebra 
   */
  type CopyOutIO[A] = F[CopyOutOp, A]

  /**
   * Catchable instance for [[CopyOutIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableCopyOutIO: Catchable[CopyOutIO] =
    new Catchable[CopyOutIO] {
      def attempt[A](f: CopyOutIO[A]): CopyOutIO[Throwable \/ A] = copyout.attempt(f)
      def fail[A](err: Throwable): CopyOutIO[A] = copyout.delay(throw err)
    }

  /**
   * Capture instance for [[CopyOutIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureCopyOutIO: Capture[CopyOutIO] =
    new Capture[CopyOutIO] {
      def apply[A](a: => A): CopyOutIO[A] = copyout.delay(a)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftCopyIn[A](s: CopyIn, k: CopyInIO[A]): CopyOutIO[A] =
    F.liftF(LiftCopyInIO(s, k))

  /** 
   * Lift a CopyOutIO[A] into an exception-capturing CopyOutIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: CopyOutIO[A]): CopyOutIO[Throwable \/ A] =
    F.liftF[CopyOutOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): CopyOutIO[A] =
    F.liftF(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  val cancelCopy: CopyOutIO[Unit] =
    F.liftF(CancelCopy)

  /** 
   * @group Constructors (Primitives)
   */
  val getFieldCount: CopyOutIO[Int] =
    F.liftF(GetFieldCount)

  /** 
   * @group Constructors (Primitives)
   */
  def getFieldFormat(a: Int): CopyOutIO[Int] =
    F.liftF(GetFieldFormat(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getFormat: CopyOutIO[Int] =
    F.liftF(GetFormat)

  /** 
   * @group Constructors (Primitives)
   */
  val getHandledRowCount: CopyOutIO[Long] =
    F.liftF(GetHandledRowCount)

  /** 
   * @group Constructors (Primitives)
   */
  val isActive: CopyOutIO[Boolean] =
    F.liftF(IsActive)

  /** 
   * @group Constructors (Primitives)
   */
  val readFromCopy: CopyOutIO[Array[Byte]] =
    F.liftF(ReadFromCopy)

 /** 
  * Natural transformation from `CopyOutOp` to `Kleisli` for the given `M`, consuming a `org.postgresql.copy.CopyOut`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: CopyOutOp ~> ({type l[a] = Kleisli[M, CopyOut, a]})#l =
   new (CopyOutOp ~> ({type l[a] = Kleisli[M, CopyOut, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: CopyOut => A): Kleisli[M, CopyOut, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: CopyOutOp[A]): Kleisli[M, CopyOut, A] = 
       op match {

        // Lifting
        case LiftCopyInIO(s, k) => Kleisli(_ => k.transK[M].run(s))
  
        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.transK[M].attempt
  
        // Primitive Operations
        case CancelCopy => primitive(_.cancelCopy)
        case GetFieldCount => primitive(_.getFieldCount)
        case GetFieldFormat(a) => primitive(_.getFieldFormat(a))
        case GetFormat => primitive(_.getFormat)
        case GetHandledRowCount => primitive(_.getHandledRowCount)
        case IsActive => primitive(_.isActive)
        case ReadFromCopy => primitive(_.readFromCopy)
  
      }
  
    }

  /**
   * Syntax for `CopyOutIO`.
   * @group Algebra
   */
  implicit class CopyOutIOOps[A](ma: CopyOutIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, CopyOut, A] =
      ma.foldMap[Kleisli[M, CopyOut, ?]](kleisliTrans[M])
  }

}

