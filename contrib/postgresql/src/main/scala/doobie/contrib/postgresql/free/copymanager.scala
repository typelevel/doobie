package doobie.contrib.postgresql.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.lang.Class
import java.lang.Object
import java.lang.String
import org.postgresql.copy.{ CopyIn => PGCopyIn }
import org.postgresql.copy.CopyManager
import org.postgresql.copy.{ CopyOut => PGCopyOut }

import copymanager.CopyManagerIO

/**
 * Algebra and free monad for primitive operations over a `org.postgresql.copy.CopyManager`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `CopyManagerIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `CopyManagerOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, CopyManager, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: CopyManagerIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: CopyManager = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object copymanager {
  
  /** 
   * Sum type of primitive operations over a `org.postgresql.copy.CopyManager`.
   * @group Algebra 
   */
  sealed trait CopyManagerOp[A]

  /** 
   * Module of constructors for `CopyManagerOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `copymanager` module.
   * @group Algebra 
   */
  object CopyManagerOp {
    
    // Lifting
    

    // Combinators
    case class Attempt[A](action: CopyManagerIO[A]) extends CopyManagerOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends CopyManagerOp[A]

    // Primitive Operations
    case class  CopyIn(a: String, b: InputStream, c: Int) extends CopyManagerOp[Long]
    case class  CopyIn1(a: String, b: Reader) extends CopyManagerOp[Long]
    case class  CopyIn2(a: String, b: Reader, c: Int) extends CopyManagerOp[Long]
    case class  CopyIn3(a: String, b: InputStream) extends CopyManagerOp[Long]
    case class  CopyIn4(a: String) extends CopyManagerOp[PGCopyIn]
    case class  CopyOut(a: String, b: OutputStream) extends CopyManagerOp[Long]
    case class  CopyOut1(a: String, b: Writer) extends CopyManagerOp[Long]
    case class  CopyOut2(a: String) extends CopyManagerOp[PGCopyOut]

  }
  import CopyManagerOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[CopyManagerOp]]; abstractly, a computation that consumes 
   * a `org.postgresql.copy.CopyManager` and produces a value of type `A`. 
   * @group Algebra 
   */
  type CopyManagerIO[A] = F.FreeC[CopyManagerOp, A]

  /**
   * Monad instance for [[CopyManagerIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadCopyManagerIO: Monad[CopyManagerIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[CopyManagerOp, α]})#λ]

  /**
   * Catchable instance for [[CopyManagerIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableCopyManagerIO: Catchable[CopyManagerIO] =
    new Catchable[CopyManagerIO] {
      def attempt[A](f: CopyManagerIO[A]): CopyManagerIO[Throwable \/ A] = copymanager.attempt(f)
      def fail[A](err: Throwable): CopyManagerIO[A] = copymanager.delay(throw err)
    }

  /**
   * Capture instance for [[CopyManagerIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureCopyManagerIO: Capture[CopyManagerIO] =
    new Capture[CopyManagerIO] {
      def apply[A](a: => A): CopyManagerIO[A] = copymanager.delay(a)
    }

  

  /** 
   * Lift a CopyManagerIO[A] into an exception-capturing CopyManagerIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: CopyManagerIO[A]): CopyManagerIO[Throwable \/ A] =
    F.liftFC[CopyManagerOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): CopyManagerIO[A] =
    F.liftFC(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  def copyIn(a: String, b: InputStream, c: Int): CopyManagerIO[Long] =
    F.liftFC(CopyIn(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def copyIn(a: String, b: Reader): CopyManagerIO[Long] =
    F.liftFC(CopyIn1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def copyIn(a: String, b: Reader, c: Int): CopyManagerIO[Long] =
    F.liftFC(CopyIn2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def copyIn(a: String, b: InputStream): CopyManagerIO[Long] =
    F.liftFC(CopyIn3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def copyIn(a: String): CopyManagerIO[PGCopyIn] =
    F.liftFC(CopyIn4(a))

  /** 
   * @group Constructors (Primitives)
   */
  def copyOut(a: String, b: OutputStream): CopyManagerIO[Long] =
    F.liftFC(CopyOut(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def copyOut(a: String, b: Writer): CopyManagerIO[Long] =
    F.liftFC(CopyOut1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def copyOut(a: String): CopyManagerIO[PGCopyOut] =
    F.liftFC(CopyOut2(a))

 /** 
  * Natural transformation from `CopyManagerOp` to `Kleisli` for the given `M`, consuming a `org.postgresql.copy.CopyManager`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: CopyManagerOp ~> ({type l[a] = Kleisli[M, CopyManager, a]})#l =
   new (CopyManagerOp ~> ({type l[a] = Kleisli[M, CopyManager, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: CopyManager => A): Kleisli[M, CopyManager, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: CopyManagerOp[A]): Kleisli[M, CopyManager, A] = 
       op match {

        // Lifting
        
  
        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.transK[M].attempt
  
        // Primitive Operations
        case CopyIn(a, b, c) => primitive(_.copyIn(a, b, c))
        case CopyIn1(a, b) => primitive(_.copyIn(a, b))
        case CopyIn2(a, b, c) => primitive(_.copyIn(a, b, c))
        case CopyIn3(a, b) => primitive(_.copyIn(a, b))
        case CopyIn4(a) => primitive(_.copyIn(a))
        case CopyOut(a, b) => primitive(_.copyOut(a, b))
        case CopyOut1(a, b) => primitive(_.copyOut(a, b))
        case CopyOut2(a) => primitive(_.copyOut(a))
  
      }
  
    }

  /**
   * Syntax for `CopyManagerIO`.
   * @group Algebra
   */
  implicit class CopyManagerIOOps[A](ma: CopyManagerIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, CopyManager, A] =
      F.runFC[CopyManagerOp,({type l[a]=Kleisli[M,CopyManager,a]})#l,A](ma)(kleisliTrans[M])
  }

}

