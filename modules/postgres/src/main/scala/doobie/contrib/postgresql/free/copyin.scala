package doobie.contrib.postgresql.free

import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import org.postgresql.copy.CopyIn
import org.postgresql.copy.CopyOut

import copyin.CopyInIO
import copyout.CopyOutIO

/**
 * Algebra and free monad for primitive operations over a `org.postgresql.copy.CopyIn`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `CopyInIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `CopyInOp` to another monad via
 * `Free#foldMap`. 
 *
 * The library provides a natural transformation to `Kleisli[M, CopyIn, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: CopyInIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: CopyIn = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object copyin {
  
  /** 
   * Sum type of primitive operations over a `org.postgresql.copy.CopyIn`.
   * @group Algebra 
   */
  sealed trait CopyInOp[A]

  /** 
   * Module of constructors for `CopyInOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `copyin` module.
   * @group Algebra 
   */
  object CopyInOp {
    
    // Lifting
    case class LiftCopyOutIO[A](s: CopyOut, action: CopyOutIO[A]) extends CopyInOp[A]

    // Combinators
    case class Attempt[A](action: CopyInIO[A]) extends CopyInOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends CopyInOp[A]

    // Primitive Operations
    case object CancelCopy extends CopyInOp[Unit]
    case object EndCopy extends CopyInOp[Long]
    case object FlushCopy extends CopyInOp[Unit]
    case object GetFieldCount extends CopyInOp[Int]
    case class  GetFieldFormat(a: Int) extends CopyInOp[Int]
    case object GetFormat extends CopyInOp[Int]
    case object GetHandledRowCount extends CopyInOp[Long]
    case object IsActive extends CopyInOp[Boolean]
    case class  WriteToCopy(a: Array[Byte], b: Int, c: Int) extends CopyInOp[Unit]

  }
  import CopyInOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[CopyInOp]]; abstractly, a computation that consumes 
   * a `org.postgresql.copy.CopyIn` and produces a value of type `A`. 
   * @group Algebra 
   */
  type CopyInIO[A] = F[CopyInOp, A]

  /**
   * Catchable instance for [[CopyInIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableCopyInIO: Catchable[CopyInIO] =
    new Catchable[CopyInIO] {
      def attempt[A](f: CopyInIO[A]): CopyInIO[Throwable \/ A] = copyin.attempt(f)
      def fail[A](err: Throwable): CopyInIO[A] = copyin.delay(throw err)
    }

  /**
   * Capture instance for [[CopyInIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureCopyInIO: Capture[CopyInIO] =
    new Capture[CopyInIO] {
      def apply[A](a: => A): CopyInIO[A] = copyin.delay(a)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftCopyOut[A](s: CopyOut, k: CopyOutIO[A]): CopyInIO[A] =
    F.liftF(LiftCopyOutIO(s, k))

  /** 
   * Lift a CopyInIO[A] into an exception-capturing CopyInIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: CopyInIO[A]): CopyInIO[Throwable \/ A] =
    F.liftF[CopyInOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): CopyInIO[A] =
    F.liftF(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  val cancelCopy: CopyInIO[Unit] =
    F.liftF(CancelCopy)

  /** 
   * @group Constructors (Primitives)
   */
  val endCopy: CopyInIO[Long] =
    F.liftF(EndCopy)

  /** 
   * @group Constructors (Primitives)
   */
  val flushCopy: CopyInIO[Unit] =
    F.liftF(FlushCopy)

  /** 
   * @group Constructors (Primitives)
   */
  val getFieldCount: CopyInIO[Int] =
    F.liftF(GetFieldCount)

  /** 
   * @group Constructors (Primitives)
   */
  def getFieldFormat(a: Int): CopyInIO[Int] =
    F.liftF(GetFieldFormat(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getFormat: CopyInIO[Int] =
    F.liftF(GetFormat)

  /** 
   * @group Constructors (Primitives)
   */
  val getHandledRowCount: CopyInIO[Long] =
    F.liftF(GetHandledRowCount)

  /** 
   * @group Constructors (Primitives)
   */
  val isActive: CopyInIO[Boolean] =
    F.liftF(IsActive)

  /** 
   * @group Constructors (Primitives)
   */
  def writeToCopy(a: Array[Byte], b: Int, c: Int): CopyInIO[Unit] =
    F.liftF(WriteToCopy(a, b, c))

 /** 
  * Natural transformation from `CopyInOp` to `Kleisli` for the given `M`, consuming a `org.postgresql.copy.CopyIn`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: CopyInOp ~> ({type l[a] = Kleisli[M, CopyIn, a]})#l =
   new (CopyInOp ~> ({type l[a] = Kleisli[M, CopyIn, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: CopyIn => A): Kleisli[M, CopyIn, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: CopyInOp[A]): Kleisli[M, CopyIn, A] = 
       op match {

        // Lifting
        case LiftCopyOutIO(s, k) => Kleisli(_ => k.transK[M].run(s))
  
        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.transK[M].attempt
  
        // Primitive Operations
        case CancelCopy => primitive(_.cancelCopy)
        case EndCopy => primitive(_.endCopy)
        case FlushCopy => primitive(_.flushCopy)
        case GetFieldCount => primitive(_.getFieldCount)
        case GetFieldFormat(a) => primitive(_.getFieldFormat(a))
        case GetFormat => primitive(_.getFormat)
        case GetHandledRowCount => primitive(_.getHandledRowCount)
        case IsActive => primitive(_.isActive)
        case WriteToCopy(a, b, c) => primitive(_.writeToCopy(a, b, c))
  
      }
  
    }

  /**
   * Syntax for `CopyInIO`.
   * @group Algebra
   */
  implicit class CopyInIOOps[A](ma: CopyInIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, CopyIn, A] =
      ma.foldMap[Kleisli[M, CopyIn, ?]](kleisliTrans[M])
  }

}

