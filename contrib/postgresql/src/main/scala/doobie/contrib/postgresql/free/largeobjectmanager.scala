package doobie.contrib.postgresql.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import java.lang.Class
import java.lang.Object
import java.lang.String
import org.postgresql.largeobject.LargeObject
import org.postgresql.largeobject.LargeObjectManager

import largeobjectmanager.LargeObjectManagerIO

/**
 * Algebra and free monad for primitive operations over a `org.postgresql.largeobject.LargeObjectManager`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `LargeObjectManagerIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `LargeObjectManagerOp` to another monad via
 * `Free.runFC`. 
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
    

    // Combinators
    case class Attempt[A](action: LargeObjectManagerIO[A]) extends LargeObjectManagerOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends LargeObjectManagerOp[A]

    // Primitive Operations
    case class  Create(a: Int) extends LargeObjectManagerOp[Int]
    case object Create1 extends LargeObjectManagerOp[Int]
    case object CreateLO extends LargeObjectManagerOp[Long]
    case class  CreateLO1(a: Int) extends LargeObjectManagerOp[Long]
    case class  Delete(a: Int) extends LargeObjectManagerOp[Unit]
    case class  Delete1(a: Long) extends LargeObjectManagerOp[Unit]
    case class  Open(a: Long, b: Int) extends LargeObjectManagerOp[LargeObject]
    case class  Open1(a: Int, b: Boolean) extends LargeObjectManagerOp[LargeObject]
    case class  Open2(a: Long, b: Int, c: Boolean) extends LargeObjectManagerOp[LargeObject]
    case class  Open3(a: Int) extends LargeObjectManagerOp[LargeObject]
    case class  Open4(a: Long, b: Boolean) extends LargeObjectManagerOp[LargeObject]
    case class  Open5(a: Long) extends LargeObjectManagerOp[LargeObject]
    case class  Open6(a: Int, b: Int) extends LargeObjectManagerOp[LargeObject]
    case class  Open7(a: Int, b: Int, c: Boolean) extends LargeObjectManagerOp[LargeObject]
    case class  Unlink(a: Long) extends LargeObjectManagerOp[Unit]
    case class  Unlink1(a: Int) extends LargeObjectManagerOp[Unit]

  }
  import LargeObjectManagerOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[LargeObjectManagerOp]]; abstractly, a computation that consumes 
   * a `org.postgresql.largeobject.LargeObjectManager` and produces a value of type `A`. 
   * @group Algebra 
   */
  type LargeObjectManagerIO[A] = F.FreeC[LargeObjectManagerOp, A]

  /**
   * Monad instance for [[LargeObjectManagerIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadLargeObjectManagerIO: Monad[LargeObjectManagerIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[LargeObjectManagerOp, α]})#λ]

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
   * Lift a LargeObjectManagerIO[A] into an exception-capturing LargeObjectManagerIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: LargeObjectManagerIO[A]): LargeObjectManagerIO[Throwable \/ A] =
    F.liftFC[LargeObjectManagerOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): LargeObjectManagerIO[A] =
    F.liftFC(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  def create(a: Int): LargeObjectManagerIO[Int] =
    F.liftFC(Create(a))

  /** 
   * @group Constructors (Primitives)
   */
  val create: LargeObjectManagerIO[Int] =
    F.liftFC(Create1)

  /** 
   * @group Constructors (Primitives)
   */
  val createLO: LargeObjectManagerIO[Long] =
    F.liftFC(CreateLO)

  /** 
   * @group Constructors (Primitives)
   */
  def createLO(a: Int): LargeObjectManagerIO[Long] =
    F.liftFC(CreateLO1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def delete(a: Int): LargeObjectManagerIO[Unit] =
    F.liftFC(Delete(a))

  /** 
   * @group Constructors (Primitives)
   */
  def delete(a: Long): LargeObjectManagerIO[Unit] =
    F.liftFC(Delete1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def open(a: Long, b: Int): LargeObjectManagerIO[LargeObject] =
    F.liftFC(Open(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def open(a: Int, b: Boolean): LargeObjectManagerIO[LargeObject] =
    F.liftFC(Open1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def open(a: Long, b: Int, c: Boolean): LargeObjectManagerIO[LargeObject] =
    F.liftFC(Open2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def open(a: Int): LargeObjectManagerIO[LargeObject] =
    F.liftFC(Open3(a))

  /** 
   * @group Constructors (Primitives)
   */
  def open(a: Long, b: Boolean): LargeObjectManagerIO[LargeObject] =
    F.liftFC(Open4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def open(a: Long): LargeObjectManagerIO[LargeObject] =
    F.liftFC(Open5(a))

  /** 
   * @group Constructors (Primitives)
   */
  def open(a: Int, b: Int): LargeObjectManagerIO[LargeObject] =
    F.liftFC(Open6(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def open(a: Int, b: Int, c: Boolean): LargeObjectManagerIO[LargeObject] =
    F.liftFC(Open7(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def unlink(a: Long): LargeObjectManagerIO[Unit] =
    F.liftFC(Unlink(a))

  /** 
   * @group Constructors (Primitives)
   */
  def unlink(a: Int): LargeObjectManagerIO[Unit] =
    F.liftFC(Unlink1(a))

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
        
  
        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.transK[M].attempt
  
        // Primitive Operations
        case Create(a) => primitive(_.create(a))
        case Create1 => primitive(_.create)
        case CreateLO => primitive(_.createLO)
        case CreateLO1(a) => primitive(_.createLO(a))
        case Delete(a) => primitive(_.delete(a))
        case Delete1(a) => primitive(_.delete(a))
        case Open(a, b) => primitive(_.open(a, b))
        case Open1(a, b) => primitive(_.open(a, b))
        case Open2(a, b, c) => primitive(_.open(a, b, c))
        case Open3(a) => primitive(_.open(a))
        case Open4(a, b) => primitive(_.open(a, b))
        case Open5(a) => primitive(_.open(a))
        case Open6(a, b) => primitive(_.open(a, b))
        case Open7(a, b, c) => primitive(_.open(a, b, c))
        case Unlink(a) => primitive(_.unlink(a))
        case Unlink1(a) => primitive(_.unlink(a))
  
      }
  
    }

  /**
   * Syntax for `LargeObjectManagerIO`.
   * @group Algebra
   */
  implicit class LargeObjectManagerIOOps[A](ma: LargeObjectManagerIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, LargeObjectManager, A] =
      F.runFC[LargeObjectManagerOp,({type l[a]=Kleisli[M,LargeObjectManager,a]})#l,A](ma)(kleisliTrans[M])
  }

}

