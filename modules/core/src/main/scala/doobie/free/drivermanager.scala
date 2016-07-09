package doobie.free

import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._
import doobie.free.kleislitrans._

import java.io.PrintStream
import java.io.PrintWriter
import java.lang.Class
import java.lang.Object
import java.lang.String
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.util.Enumeration
import java.util.Properties

import connection.ConnectionIO
import driver.DriverIO

object drivermanager {
  
  /** 
   * Sum type of primitive operations over a `java.sql.DriverManager`.
   * @group Algebra 
   */
  sealed trait DriverManagerOp[A]

  /** 
   * Module of constructors for `DriverManagerOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `drivermanager` module.
   * @group Algebra 
   */
  object DriverManagerOp {
    
    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F[Op, A], mod: KleisliTrans.Aux[Op, J]) extends DriverManagerOp[A]

    // Combinators
    case class Attempt[A](action: DriverManagerIO[A]) extends DriverManagerOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends DriverManagerOp[A]

    // Primitive Operations
    case class  DeregisterDriver(a: Driver) extends DriverManagerOp[Unit]
    case class  GetConnection(a: String) extends DriverManagerOp[Connection]
    case class  GetConnection1(a: String, b: String, c: String) extends DriverManagerOp[Connection]
    case class  GetConnection2(a: String, b: Properties) extends DriverManagerOp[Connection]
    case class  GetDriver(a: String) extends DriverManagerOp[Driver]
    case object GetDrivers extends DriverManagerOp[Enumeration[Driver]]
    case object GetLogStream extends DriverManagerOp[PrintStream]
    case object GetLogWriter extends DriverManagerOp[PrintWriter]
    case object GetLoginTimeout extends DriverManagerOp[Int]
    case class  Println(a: String) extends DriverManagerOp[Unit]
    case class  RegisterDriver(a: Driver) extends DriverManagerOp[Unit]
    case class  SetLogStream(a: PrintStream) extends DriverManagerOp[Unit]
    case class  SetLogWriter(a: PrintWriter) extends DriverManagerOp[Unit]
    case class  SetLoginTimeout(a: Int) extends DriverManagerOp[Unit]

  }
  import DriverManagerOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[DriverManagerOp]]; abstractly, a computation that consumes 
   * a `java.sql.DriverManager` and produces a value of type `A`. 
   * @group Algebra 
   */
  type DriverManagerIO[A] = F[DriverManagerOp, A]

  /**
   * Catchable instance for [[DriverManagerIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableDriverManagerIO: Catchable[DriverManagerIO] =
    new Catchable[DriverManagerIO] {
      def attempt[A](f: DriverManagerIO[A]): DriverManagerIO[Throwable \/ A] = drivermanager.attempt(f)
      def fail[A](err: Throwable): DriverManagerIO[A] = drivermanager.delay(throw err)
    }

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): DriverManagerIO[A] =
    F.liftF(Lift(j, action, mod))

  /** 
   * Lift a DriverManagerIO[A] into an exception-capturing DriverManagerIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: DriverManagerIO[A]): DriverManagerIO[Throwable \/ A] =
    F.liftF[DriverManagerOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): DriverManagerIO[A] =
    F.liftF(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  def deregisterDriver(a: Driver): DriverManagerIO[Unit] =
    F.liftF(DeregisterDriver(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getConnection(a: String): DriverManagerIO[Connection] =
    F.liftF(GetConnection(a))

  /** 
   * @group Constructors (Primitives)
   */
  def getConnection(a: String, b: String, c: String): DriverManagerIO[Connection] =
    F.liftF(GetConnection1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def getConnection(a: String, b: Properties): DriverManagerIO[Connection] =
    F.liftF(GetConnection2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getDriver(a: String): DriverManagerIO[Driver] =
    F.liftF(GetDriver(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getDrivers: DriverManagerIO[Enumeration[Driver]] =
    F.liftF(GetDrivers)

  /** 
   * @group Constructors (Primitives)
   */
  val getLogStream: DriverManagerIO[PrintStream] =
    F.liftF(GetLogStream)

  /** 
   * @group Constructors (Primitives)
   */
  val getLogWriter: DriverManagerIO[PrintWriter] =
    F.liftF(GetLogWriter)

  /** 
   * @group Constructors (Primitives)
   */
  val getLoginTimeout: DriverManagerIO[Int] =
    F.liftF(GetLoginTimeout)

  /** 
   * @group Constructors (Primitives)
   */
  def println(a: String): DriverManagerIO[Unit] =
    F.liftF(Println(a))

  /** 
   * @group Constructors (Primitives)
   */
  def registerDriver(a: Driver): DriverManagerIO[Unit] =
    F.liftF(RegisterDriver(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setLogStream(a: PrintStream): DriverManagerIO[Unit] =
    F.liftF(SetLogStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setLogWriter(a: PrintWriter): DriverManagerIO[Unit] =
    F.liftF(SetLogWriter(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setLoginTimeout(a: Int): DriverManagerIO[Unit] =
    F.liftF(SetLoginTimeout(a))

 /** 
  * Natural transformation from `DriverManagerOp` to the given `M`. 
  * @group Algebra
  */
 def trans[M[_]: Monad: Catchable: Capture]: DriverManagerOp ~> M =
   new (DriverManagerOp ~>  M) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def apply[A](op: DriverManagerOp[A]): M[A] = 
       op match {

        // Lifting
        case Lift(s, a, mod) => mod.transK[M].apply(a).run(s)

        // Combinators
        case Pure(a) => L.apply(a())
        case Attempt(a) => a.trans[M].attempt
  
        // Primitive Operations
        case DeregisterDriver(a) => L.apply(DriverManager.deregisterDriver(a))
        case GetConnection(a) => L.apply(DriverManager.getConnection(a))
        case GetConnection1(a, b, c) => L.apply(DriverManager.getConnection(a, b, c))
        case GetConnection2(a, b) => L.apply(DriverManager.getConnection(a, b))
        case GetDriver(a) => L.apply(DriverManager.getDriver(a))
        case GetDrivers => L.apply(DriverManager.getDrivers)
        case GetLogStream => L.apply(DriverManager.getLogStream)
        case GetLogWriter => L.apply(DriverManager.getLogWriter)
        case GetLoginTimeout => L.apply(DriverManager.getLoginTimeout)
        case Println(a) => L.apply(DriverManager.println(a))
        case RegisterDriver(a) => L.apply(DriverManager.registerDriver(a))
        case SetLogStream(a) => L.apply(DriverManager.setLogStream(a))
        case SetLogWriter(a) => L.apply(DriverManager.setLogWriter(a))
        case SetLoginTimeout(a) => L.apply(DriverManager.setLoginTimeout(a))
  
      }
  
    }

  /**
   * Syntax for `DriverManagerIO`.
   * @group Algebra
   */
  implicit class DriverManagerIOOps[A](ma: DriverManagerIO[A]) {
    def trans[M[_]: Monad: Catchable: Capture]: M[A] =
      ma.foldMap(drivermanager.trans[M])
  }

}

