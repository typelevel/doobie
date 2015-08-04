package doobie.contrib.informix.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import java.io.InputStream
import java.io.OutputStream
import java.lang.Class
import java.lang.Object
import java.lang.String
import com.informix.jdbc.IfxSmartBlob

import ifxsmartblob.IfxSmartBlobIO

/**
 * Algebra and free monad for primitive operations over a `com.informix.jdbc.IfxSmartBlob`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `IfxSmartBlobIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `IfxSmartBlobOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, IfxSmartBlob, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: IfxSmartBlobIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: IfxSmartBlob = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object ifxsmartblob {
  
  /** 
   * Sum type of primitive operations over a `com.informix.jdbc.IfxSmartBlob`.
   * @group Algebra 
   */
  sealed trait IfxSmartBlobOp[A]

  /** 
   * Module of constructors for `IfxSmartBlobOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `ifxsmartblob` module.
   * @group Algebra 
   */
  object IfxSmartBlobOp {
    
    // Lifting
    

    // Combinators
    case class Attempt[A](action: IfxSmartBlobIO[A]) extends IfxSmartBlobOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends IfxSmartBlobOp[A]

    // Primitive Operations
    case class IfxLoRead(fd:Int, buffer: Array[Byte], bufferSize: Int) extends IfxSmartBlobOp[Int]
  }
  import IfxSmartBlobOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[IfxSmartBlobOp]]; abstractly, a computation that consumes 
   * a `com.informix.jdbc.IfxSmartBlob` and produces a value of type `A`. 
   * @group Algebra 
   */
  type IfxSmartBlobIO[A] = F.FreeC[IfxSmartBlobOp, A]

  /**
   * Monad instance for [[IfxSmartBlobIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadIfxSmartBlobIO: Monad[IfxSmartBlobIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[IfxSmartBlobOp, α]})#λ]

  /**
   * Catchable instance for [[IfxSmartBlobIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableIfxSmartBlobIO: Catchable[IfxSmartBlobIO] =
    new Catchable[IfxSmartBlobIO] {
      def attempt[A](f: IfxSmartBlobIO[A]): IfxSmartBlobIO[Throwable \/ A] = ifxsmartblob.attempt(f)
      def fail[A](err: Throwable): IfxSmartBlobIO[A] = ifxsmartblob.delay(throw err)
    }

  /**
   * Capture instance for [[IfxSmartBlobIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureIfxSmartBlobIO: Capture[IfxSmartBlobIO] =
    new Capture[IfxSmartBlobIO] {
      def apply[A](a: => A): IfxSmartBlobIO[A] = ifxsmartblob.delay(a)
    }

  /** 
   * Lift a IfxSmartBlobIO[A] into an exception-capturing IfxSmartBlobIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: IfxSmartBlobIO[A]): IfxSmartBlobIO[Throwable \/ A] =
    F.liftFC[IfxSmartBlobOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): IfxSmartBlobIO[A] =
    F.liftFC(Pure(a _))

  def read(fd:Int, buf: Array[Byte], bufSize: Int): IfxSmartBlobIO[Int] =
    F.liftFC(IfxLoRead(fd, buf, bufSize))

 /** 
  * Natural transformation from `IfxSmartBlobOp` to `Kleisli` for the given `M`, consuming a `com.informix.jdbc.IfxSmartBlob`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: IfxSmartBlobOp ~> ({type l[a] = Kleisli[M, IfxSmartBlob, a]})#l =
   new (IfxSmartBlobOp ~> ({type l[a] = Kleisli[M, IfxSmartBlob, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: IfxSmartBlob => A): Kleisli[M, IfxSmartBlob, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: IfxSmartBlobOp[A]): Kleisli[M, IfxSmartBlob, A] = 
       op match {

        // Lifting
        
  
        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.transK[M].attempt
  
        // Primitive Operations
        case IfxLoRead(fd,buf,bufSize) => primitive(_.IfxLoRead(fd,buf,bufSize))
  
      }
  
    }

  /**
   * Syntax for `IfxSmartBlobIO`.
   * @group Algebra
   */
  implicit class IfxSmartBlobIOOps[A](ma: IfxSmartBlobIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, IfxSmartBlob, A] =
      F.runFC[IfxSmartBlobOp,({type l[a]=Kleisli[M,IfxSmartBlob,a]})#l,A](ma)(kleisliTrans[M])
  }

}

