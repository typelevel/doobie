package doobie.contrib.informix.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import java.lang.Class
import java.lang.String
import com.informix.jdbc.{IfxConnection, IfxSmartBlob}

import ifxconnection.IfxConnectionIO
import ifxsmartblob.IfxSmartBlobIO

/**
 * Algebra and free monad for primitive operations over a `org.informix.jdbc.IfxConnection`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.contrib.informix.hi` package.
 *
 * `IfxConnectionIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `IfxConnectionOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, IfxConnection, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: IfxConnectionIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: IfxConnection = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object ifxconnection {
  
  /** 
   * Sum type of primitive operations over a `com.informix.jdbc.IfxConnection`.
   * @group Algebra 
   */
  sealed trait IfxConnectionOp[A]

  /** 
   * Module of constructors for `IfxConnectionOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `ifxconnection` module.
   * @group Algebra 
   */
  object IfxConnectionOp {
    
    // Lifting
    case class LiftIfxSmartBlobIO[A](s: IfxSmartBlob, action: IfxSmartBlobIO[A]) extends IfxConnectionOp[A]

    // Combinators
    case class Attempt[A](action: IfxConnectionIO[A]) extends IfxConnectionOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends IfxConnectionOp[A]

    // Primitive Operations
    case object GetIfxSmartBlob extends IfxConnectionOp[IfxSmartBlob]

  }
  import IfxConnectionOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[IfxConnectionOp]]; abstractly, a computation that consumes 
   * a `com.informix.jdbc.IfxConnection` and produces a value of type `A`. 
   * @group Algebra 
   */
  type IfxConnectionIO[A] = F.FreeC[IfxConnectionOp, A]

  /**
   * Monad instance for [[IfxConnectionIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadIfxConnectionIO: Monad[IfxConnectionIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[IfxConnectionOp, α]})#λ]

  /**
   * Catchable instance for [[IfxConnectionIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableIfxConnectionIO: Catchable[IfxConnectionIO] =
    new Catchable[IfxConnectionIO] {
      def attempt[A](f: IfxConnectionIO[A]): IfxConnectionIO[Throwable \/ A] = ifxconnection.attempt(f)
      def fail[A](err: Throwable): IfxConnectionIO[A] = ifxconnection.delay(throw err)
    }

  /**
   * Capture instance for [[IfxConnectionIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureIfxConnectionIO: Capture[IfxConnectionIO] =
    new Capture[IfxConnectionIO] {
      def apply[A](a: => A): IfxConnectionIO[A] = ifxconnection.delay(a)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftIfxSmartBlob[A](s: IfxSmartBlob, action: IfxSmartBlobIO[A]): IfxConnectionIO[A] =
    F.liftFC(LiftIfxSmartBlobIO(s, action))

  /** 
   * Lift a IfxConnectionIO[A] into an exception-capturing IfxConnectionIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: IfxConnectionIO[A]): IfxConnectionIO[Throwable \/ A] =
    F.liftFC[IfxConnectionOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): IfxConnectionIO[A] =
    F.liftFC(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  val getIfxSmartBlob: IfxConnectionIO[IfxSmartBlob] =
    F.liftFC(GetIfxSmartBlob)

 /** 
  * Natural transformation from `IfxConnectionOp` to `Kleisli` for the given `M`, consuming a `com.informix.jdbc.IfxConnection`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: IfxConnectionOp ~> ({type l[a] = Kleisli[M, IfxConnection, a]})#l =
   new (IfxConnectionOp ~> ({type l[a] = Kleisli[M, IfxConnection, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: IfxConnection => A): Kleisli[M, IfxConnection, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: IfxConnectionOp[A]): Kleisli[M, IfxConnection, A] = 
       op match {

        // Lifting
        case LiftIfxSmartBlobIO(s, k) => Kleisli(_ => k.transK[M].run(s))

        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.transK[M].attempt
  
        // Primitive Operations
        case GetIfxSmartBlob => primitive(ifxconn => new IfxSmartBlob(ifxconn))
      }
    }

  /**
   * Syntax for `IfxConnectionIO`.
   * @group Algebra
   */
  implicit class IfxConnectionIOOps[A](ma: IfxConnectionIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, IfxConnection, A] =
      F.runFC[IfxConnectionOp,({type l[a]=Kleisli[M,IfxConnection,a]})#l,A](ma)(kleisliTrans[M])
  }

}

