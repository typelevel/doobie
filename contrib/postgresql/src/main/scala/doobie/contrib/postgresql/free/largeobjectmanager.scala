package doobie.contrib.postgresql.free

import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import java.lang.Class
import java.lang.Object
import java.lang.String
import org.postgresql.largeobject.LargeObject
import org.postgresql.largeobject.LargeObjectManager

import largeobjectmanager.LargeObjectManagerIO
import largeobject.LargeObjectIO

/**
 * Algebra and free monad for primitive operations over a `org.postgresql.largeobject.LargeObjectManager`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `LargeObjectManagerIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `LargeObjectManagerOp` to another monad via
 * `Free#foldMap`. 
 *
 * The library provides a natural transformation to `Kleisli[M, LargeObjectManager, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: LargeObjectManagerIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: LargeObjectManager = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object largeobjectmanager {
  
  /** 
   * Sum type of primitive operations over a `org.postgresql.largeobject.LargeObjectManager`.
   * @group Algebra 
   */
  sealed trait LargeObjectManagerOp[A]

  /** 
   * Module of constructors for `LargeObjectManagerOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `largeobjectmanager` module.
   * @group Algebra 
   */
  object LargeObjectManagerOp {
    
    // Lifting
    case class LiftLargeObjectIO[A](s: LargeObject, action: LargeObjectIO[A]) extends LargeObjectManagerOp[A]

    // Combinators
    case class Attempt[A](action: LargeObjectManagerIO[A]) extends LargeObjectManagerOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends LargeObjectManagerOp[A]

    // Primitive Operations
    case object CreateLO extends LargeObjectManagerOp[Long]
    case class  CreateLO1(a: Int) extends LargeObjectManagerOp[Long]
    case class  Delete(a: Long) extends LargeObjectManagerOp[Unit]
    case class  Open(a: Long, b: Int) extends LargeObjectManagerOp[LargeObject]
    case class  Open1(a: Long) extends LargeObjectManagerOp[LargeObject]
    case class  Unlink(a: Long) extends LargeObjectManagerOp[Unit]

  }
  import LargeObjectManagerOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[LargeObjectManagerOp]]; abstractly, a computation that consumes 
   * a `org.postgresql.largeobject.LargeObjectManager` and produces a value of type `A`. 
   * @group Algebra 
   */
  type LargeObjectManagerIO[A] = F[LargeObjectManagerOp, A]

  /**
   * Catchable instance for [[LargeObjectManagerIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableLargeObjectManagerIO: Catchable[LargeObjectManagerIO] =
    new Catchable[LargeObjectManagerIO] {
      def attempt[A](f: LargeObjectManagerIO[A]): LargeObjectManagerIO[Throwable \/ A] = largeobjectmanager.attempt(f)
      def fail[A](err: Throwable): LargeObjectManagerIO[A] = largeobjectmanager.delay(throw err)
    }

  /**
   * Capture instance for [[LargeObjectManagerIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureLargeObjectManagerIO: Capture[LargeObjectManagerIO] =
    new Capture[LargeObjectManagerIO] {
      def apply[A](a: => A): LargeObjectManagerIO[A] = largeobjectmanager.delay(a)
    }
  
  /**
   * @group Constructors (Lifting)
   */
  def liftLargeObject[A](s: LargeObject, action: LargeObjectIO[A]): LargeObjectManagerIO[A] =
    F.liftF(LiftLargeObjectIO(s, action))

  /** 
   * Lift a LargeObjectManagerIO[A] into an exception-capturing LargeObjectManagerIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: LargeObjectManagerIO[A]): LargeObjectManagerIO[Throwable \/ A] =
    F.liftF[LargeObjectManagerOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): LargeObjectManagerIO[A] =
    F.liftF(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  val createLO: LargeObjectManagerIO[Long] =
    F.liftF(CreateLO)

  /** 
   * @group Constructors (Primitives)
   */
  def createLO(a: Int): LargeObjectManagerIO[Long] =
    F.liftF(CreateLO1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def delete(a: Long): LargeObjectManagerIO[Unit] =
    F.liftF(Delete(a))

  /** 
   * @group Constructors (Primitives)
   */
  def open(a: Long, b: Int): LargeObjectManagerIO[LargeObject] =
    F.liftF(Open(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def open(a: Long): LargeObjectManagerIO[LargeObject] =
    F.liftF(Open1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def unlink(a: Long): LargeObjectManagerIO[Unit] =
    F.liftF(Unlink(a))

 /** 
  * Natural transformation from `LargeObjectManagerOp` to `Kleisli` for the given `M`, consuming a `org.postgresql.largeobject.LargeObjectManager`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: LargeObjectManagerOp ~> ({type l[a] = Kleisli[M, LargeObjectManager, a]})#l =
   new (LargeObjectManagerOp ~> ({type l[a] = Kleisli[M, LargeObjectManager, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: LargeObjectManager => A): Kleisli[M, LargeObjectManager, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: LargeObjectManagerOp[A]): Kleisli[M, LargeObjectManager, A] = 
       op match {

        // Lifting
        case LiftLargeObjectIO(s, k) => Kleisli(_ => k.transK[M].run(s))
  
        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.transK[M].attempt
  
        // Primitive Operations
        case CreateLO => primitive(_.createLO)
        case CreateLO1(a) => primitive(_.createLO(a))
        case Delete(a) => primitive(_.delete(a))
        case Open(a, b) => primitive(_.open(a, b))
        case Open1(a) => primitive(_.open(a))
        case Unlink(a) => primitive(_.unlink(a))
  
      }
  
    }

  /**
   * Syntax for `LargeObjectManagerIO`.
   * @group Algebra
   */
  implicit class LargeObjectManagerIOOps[A](ma: LargeObjectManagerIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, LargeObjectManager, A] =
      ma.foldMap[Kleisli[M, LargeObjectManager, ?]](kleisliTrans[M])
  }

}

