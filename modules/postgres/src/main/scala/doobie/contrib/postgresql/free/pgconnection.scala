package doobie.contrib.postgresql.free

import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import java.lang.Class
import java.lang.String
import org.postgresql.PGConnection
import org.postgresql.PGNotification
import org.postgresql.copy.CopyManager
import org.postgresql.fastpath.Fastpath
import org.postgresql.largeobject.LargeObjectManager

import pgconnection.PGConnectionIO
import copymanager.CopyManagerIO
import fastpath.FastpathIO
import largeobjectmanager.LargeObjectManagerIO

/**
 * Algebra and free monad for primitive operations over a `org.postgresql.PGConnection`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.contrib.postgresql.hi` package.
 *
 * `PGConnectionIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `PGConnectionOp` to another monad via
 * `Free#foldMap`.
 *
 * The library provides a natural transformation to `Kleisli[M, PGConnection, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: PGConnectionIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: PGConnection = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object pgconnection {
  
  /** 
   * Sum type of primitive operations over a `org.postgresql.PGConnection`.
   * @group Algebra 
   */
  sealed trait PGConnectionOp[A]

  /** 
   * Module of constructors for `PGConnectionOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `pgconnection` module.
   * @group Algebra 
   */
  object PGConnectionOp {
    
    // Lifting
    case class LiftCopyManagerIO[A](s: CopyManager, action: CopyManagerIO[A]) extends PGConnectionOp[A]
    case class LiftFastpathIO[A](s: Fastpath, action: FastpathIO[A]) extends PGConnectionOp[A]
    case class LiftLargeObjectManagerIO[A](s: LargeObjectManager, action: LargeObjectManagerIO[A]) extends PGConnectionOp[A]

    // Combinators
    case class Attempt[A](action: PGConnectionIO[A]) extends PGConnectionOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends PGConnectionOp[A]

    // Primitive Operations
    case class  AddDataType(a: String, b: String) extends PGConnectionOp[Unit]
    case class  AddDataType1(a: String, b: Class[_]) extends PGConnectionOp[Unit]
    case object GetBackendPID extends PGConnectionOp[Int]
    case object GetCopyAPI extends PGConnectionOp[CopyManager]
    case object GetFastpathAPI extends PGConnectionOp[Fastpath]
    case object GetLargeObjectAPI extends PGConnectionOp[LargeObjectManager]
    case object GetNotifications extends PGConnectionOp[Array[PGNotification]]
    case object GetPrepareThreshold extends PGConnectionOp[Int]
    case class  SetPrepareThreshold(a: Int) extends PGConnectionOp[Unit]

  }
  import PGConnectionOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[PGConnectionOp]]; abstractly, a computation that consumes 
   * a `org.postgresql.PGConnection` and produces a value of type `A`. 
   * @group Algebra 
   */
  type PGConnectionIO[A] = F[PGConnectionOp, A]

  /**
   * Catchable instance for [[PGConnectionIO]].
   * @group Typeclass Instances
   */
  implicit val CatchablePGConnectionIO: Catchable[PGConnectionIO] =
    new Catchable[PGConnectionIO] {
      def attempt[A](f: PGConnectionIO[A]): PGConnectionIO[Throwable \/ A] = pgconnection.attempt(f)
      def fail[A](err: Throwable): PGConnectionIO[A] = pgconnection.delay(throw err)
    }

  /**
   * Capture instance for [[PGConnectionIO]].
   * @group Typeclass Instances
   */
  implicit val CapturePGConnectionIO: Capture[PGConnectionIO] =
    new Capture[PGConnectionIO] {
      def apply[A](a: => A): PGConnectionIO[A] = pgconnection.delay(a)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftCopyManager[A](s: CopyManager, action: CopyManagerIO[A]): PGConnectionIO[A] =
    F.liftF(LiftCopyManagerIO(s, action))

  /**
   * @group Constructors (Lifting)
   */
  def liftFastpath[A](s: Fastpath, action: FastpathIO[A]): PGConnectionIO[A] =
    F.liftF(LiftFastpathIO(s, action))

  /**
   * @group Constructors (Lifting)
   */
  def liftLargeObjectManager[A](s: LargeObjectManager, action: LargeObjectManagerIO[A]): PGConnectionIO[A] =
    F.liftF(LiftLargeObjectManagerIO(s, action))

  /** 
   * Lift a PGConnectionIO[A] into an exception-capturing PGConnectionIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: PGConnectionIO[A]): PGConnectionIO[Throwable \/ A] =
    F.liftF[PGConnectionOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): PGConnectionIO[A] =
    F.liftF(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  def addDataType(a: String, b: String): PGConnectionIO[Unit] =
    F.liftF(AddDataType(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def addDataType(a: String, b: Class[_]): PGConnectionIO[Unit] =
    F.liftF(AddDataType1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val getBackendPID: PGConnectionIO[Int] =
    F.liftF(GetBackendPID)

  /** 
   * @group Constructors (Primitives)
   */
  val getCopyAPI: PGConnectionIO[CopyManager] =
    F.liftF(GetCopyAPI)

  /** 
   * @group Constructors (Primitives)
   */
  val getFastpathAPI: PGConnectionIO[Fastpath] =
    F.liftF(GetFastpathAPI)

  /** 
   * @group Constructors (Primitives)
   */
  val getLargeObjectAPI: PGConnectionIO[LargeObjectManager] =
    F.liftF(GetLargeObjectAPI)

  /** 
   * @group Constructors (Primitives)
   */
  val getNotifications: PGConnectionIO[Array[PGNotification]] =
    F.liftF(GetNotifications)

  /** 
   * @group Constructors (Primitives)
   */
  val getPrepareThreshold: PGConnectionIO[Int] =
    F.liftF(GetPrepareThreshold)

  /** 
   * @group Constructors (Primitives)
   */
  def setPrepareThreshold(a: Int): PGConnectionIO[Unit] =
    F.liftF(SetPrepareThreshold(a))

 /** 
  * Natural transformation from `PGConnectionOp` to `Kleisli` for the given `M`, consuming a `org.postgresql.PGConnection`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: PGConnectionOp ~> ({type l[a] = Kleisli[M, PGConnection, a]})#l =
   new (PGConnectionOp ~> ({type l[a] = Kleisli[M, PGConnection, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: PGConnection => A): Kleisli[M, PGConnection, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: PGConnectionOp[A]): Kleisli[M, PGConnection, A] = 
       op match {

        // Lifting
        case LiftCopyManagerIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftFastpathIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftLargeObjectManagerIO(s, k) => Kleisli(_ => k.transK[M].run(s))

        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.transK[M].attempt
  
        // Primitive Operations
        case AddDataType(a, b) => primitive(_.addDataType(a, b))
        case AddDataType1(a, b) => primitive(_.addDataType(a, b))
        case GetBackendPID => primitive(_.getBackendPID)
        case GetCopyAPI => primitive(_.getCopyAPI)
        case GetFastpathAPI => primitive(_.getFastpathAPI)
        case GetLargeObjectAPI => primitive(_.getLargeObjectAPI)
        case GetNotifications => primitive(_.getNotifications)
        case GetPrepareThreshold => primitive(_.getPrepareThreshold)
        case SetPrepareThreshold(a) => primitive(_.setPrepareThreshold(a))
  
      }
  
    }

  /**
   * Syntax for `PGConnectionIO`.
   * @group Algebra
   */
  implicit class PGConnectionIOOps[A](ma: PGConnectionIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, PGConnection, A] =
      ma.foldMap[Kleisli[M, PGConnection, ?]](kleisliTrans[M])
  }

}

