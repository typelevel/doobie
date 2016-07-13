package doobie.contrib.postgresql.free

import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import java.lang.Class
import java.lang.Object
import java.lang.String
import java.sql.ResultSet
import org.postgresql.fastpath.{ Fastpath => PGFastpath }
import org.postgresql.fastpath.FastpathArg

import fastpath.FastpathIO

/**
 * Algebra and free monad for primitive operations over a `org.postgresql.fastpath.Fastpath`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `FastpathIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `FastpathOp` to another monad via
 * `Free#foldMap`. 
 *
 * The library provides a natural transformation to `Kleisli[M, Fastpath, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: FastpathIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: Fastpath = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object fastpath { self =>
  
  /** 
   * Sum type of primitive operations over a `org.postgresql.fastpath.Fastpath`.
   * @group Algebra 
   */
  sealed trait FastpathOp[A]

  /** 
   * Module of constructors for `FastpathOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `fastpath` module.
   * @group Algebra 
   */
  object FastpathOp {
    
    // Lifting
    

    // Combinators
    case class Attempt[A](action: FastpathIO[A]) extends FastpathOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends FastpathOp[A]

    // Primitive Operations
    case class  AddFunction(a: String, b: Int) extends FastpathOp[Unit]
    case class  AddFunctions(a: ResultSet) extends FastpathOp[Unit]
    case class  Fastpath(a: String, b: Boolean, c: Array[FastpathArg]) extends FastpathOp[Object]
    case class  Fastpath1(a: Int, b: Boolean, c: Array[FastpathArg]) extends FastpathOp[Object]
    case class  GetData(a: String, b: Array[FastpathArg]) extends FastpathOp[Array[Byte]]
    case class  GetID(a: String) extends FastpathOp[Int]
    case class  GetInteger(a: String, b: Array[FastpathArg]) extends FastpathOp[Int]
    case class  GetOID(a: String, b: Array[FastpathArg]) extends FastpathOp[Long]

  }
  import FastpathOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[FastpathOp]]; abstractly, a computation that consumes 
   * a `org.postgresql.fastpath.Fastpath` and produces a value of type `A`. 
   * @group Algebra 
   */
  type FastpathIO[A] = F[FastpathOp, A]

  /**
   * Catchable instance for [[FastpathIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableFastpathIO: Catchable[FastpathIO] =
    new Catchable[FastpathIO] {
      def attempt[A](f: FastpathIO[A]): FastpathIO[Throwable \/ A] = self.attempt(f)
      def fail[A](err: Throwable): FastpathIO[A] = self.delay(throw err)
    }

  /**
   * Capture instance for [[FastpathIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureFastpathIO: Capture[FastpathIO] =
    new Capture[FastpathIO] {
      def apply[A](a: => A): FastpathIO[A] = self.delay(a)
    }

  

  /** 
   * Lift a FastpathIO[A] into an exception-capturing FastpathIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: FastpathIO[A]): FastpathIO[Throwable \/ A] =
    F.liftF[FastpathOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): FastpathIO[A] =
    F.liftF(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  def addFunction(a: String, b: Int): FastpathIO[Unit] =
    F.liftF(AddFunction(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def addFunctions(a: ResultSet): FastpathIO[Unit] =
    F.liftF(AddFunctions(a))

  /** 
   * @group Constructors (Primitives)
   */
  def fastpath(a: String, b: Boolean, c: Array[FastpathArg]): FastpathIO[Object] =
    F.liftF(Fastpath(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def fastpath(a: Int, b: Boolean, c: Array[FastpathArg]): FastpathIO[Object] =
    F.liftF(Fastpath1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def getData(a: String, b: Array[FastpathArg]): FastpathIO[Array[Byte]] =
    F.liftF(GetData(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getID(a: String): FastpathIO[Int] =
    F.liftF(GetID(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getInteger(a: String, b: Array[FastpathArg]): FastpathIO[Int] =
    F.liftF(GetInteger(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getOID(a: String, b: Array[FastpathArg]): FastpathIO[Long] =
    F.liftF(GetOID(a, b))

 /** 
  * Natural transformation from `FastpathOp` to `Kleisli` for the given `M`, consuming a `org.postgresql.fastpath.Fastpath`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: FastpathOp ~> ({type l[a] = Kleisli[M, PGFastpath, a]})#l =
   new (FastpathOp ~> ({type l[a] = Kleisli[M, PGFastpath, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: PGFastpath => A): Kleisli[M, PGFastpath, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: FastpathOp[A]): Kleisli[M, PGFastpath, A] = 
       op match {

        // Lifting
        
  
        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.transK[M].attempt
  
        // Primitive Operations
        case AddFunction(a, b) => primitive(_.addFunction(a, b))
        case AddFunctions(a) => primitive(_.addFunctions(a))
        case Fastpath(a, b, c) => primitive(_.fastpath(a, b, c))
        case Fastpath1(a, b, c) => primitive(_.fastpath(a, b, c))
        case GetData(a, b) => primitive(_.getData(a, b))
        case GetID(a) => primitive(_.getID(a))
        case GetInteger(a, b) => primitive(_.getInteger(a, b))
        case GetOID(a, b) => primitive(_.getOID(a, b))
  
      }
  
    }

  /**
   * Syntax for `FastpathIO`.
   * @group Algebra
   */
  implicit class FastpathIOOps[A](ma: FastpathIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, PGFastpath, A] =
      ma.foldMap[Kleisli[M, PGFastpath, ?]](kleisliTrans[M])
  }

}

