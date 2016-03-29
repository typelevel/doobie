package doobie.free

import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._
import doobie.free.kleislitrans._

import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.lang.String
import java.sql.Blob
import java.sql.CallableStatement
import java.sql.Clob
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.Driver
import java.sql.NClob
import java.sql.PreparedStatement
import java.sql.Ref
import java.sql.ResultSet
import java.sql.SQLData
import java.sql.SQLInput
import java.sql.SQLOutput
import java.sql.Statement

import nclob.NClobIO
import blob.BlobIO
import clob.ClobIO
import databasemetadata.DatabaseMetaDataIO
import driver.DriverIO
import ref.RefIO
import sqldata.SQLDataIO
import sqlinput.SQLInputIO
import sqloutput.SQLOutputIO
import connection.ConnectionIO
import statement.StatementIO
import preparedstatement.PreparedStatementIO
import callablestatement.CallableStatementIO
import resultset.ResultSetIO

/**
 * Algebra and free monad for primitive operations over a `java.sql.NClob`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `NClobIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `NClobOp` to another monad via
 * `Free#foldMap`.
 *
 * The library provides a natural transformation to `Kleisli[M, NClob, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: NClobIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: NClob = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object nclob {
  
  /** 
   * Sum type of primitive operations over a `java.sql.NClob`.
   * @group Algebra 
   */
  sealed trait NClobOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: NClob => A): Kleisli[M, NClob, A] = 
      Kleisli((s: NClob) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, NClob, A]
  }

  /** 
   * Module of constructors for `NClobOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `nclob` module.
   * @group Algebra 
   */
  object NClobOp {
    
    // This algebra has a default interpreter
    implicit val NClobKleisliTrans: KleisliTrans.Aux[NClobOp, NClob] =
      new KleisliTrans[NClobOp] {
        type J = NClob
        def interpK[M[_]: Monad: Catchable: Capture]: NClobOp ~> Kleisli[M, NClob, ?] =
          new (NClobOp ~> Kleisli[M, NClob, ?]) {
            def apply[A](op: NClobOp[A]): Kleisli[M, NClob, A] =
              op.defaultTransK[M]
          }
      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F[Op, A], mod: KleisliTrans.Aux[Op, J]) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
    }

    // Combinators
    case class Attempt[A](action: NClobIO[A]) extends NClobOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, NClob, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: NClob => A) extends NClobOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case object Free extends NClobOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.free())
    }
    case object GetAsciiStream extends NClobOp[InputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getAsciiStream())
    }
    case object GetCharacterStream extends NClobOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCharacterStream())
    }
    case class  GetCharacterStream1(a: Long, b: Long) extends NClobOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCharacterStream(a, b))
    }
    case class  GetSubString(a: Long, b: Int) extends NClobOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSubString(a, b))
    }
    case object Length extends NClobOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.length())
    }
    case class  Position(a: Clob, b: Long) extends NClobOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.position(a, b))
    }
    case class  Position1(a: String, b: Long) extends NClobOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.position(a, b))
    }
    case class  SetAsciiStream(a: Long) extends NClobOp[OutputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAsciiStream(a))
    }
    case class  SetCharacterStream(a: Long) extends NClobOp[Writer] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCharacterStream(a))
    }
    case class  SetString(a: Long, b: String, c: Int, d: Int) extends NClobOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setString(a, b, c, d))
    }
    case class  SetString1(a: Long, b: String) extends NClobOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setString(a, b))
    }
    case class  Truncate(a: Long) extends NClobOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.truncate(a))
    }

  }
  import NClobOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[NClobOp]]; abstractly, a computation that consumes 
   * a `java.sql.NClob` and produces a value of type `A`. 
   * @group Algebra 
   */
  type NClobIO[A] = F[NClobOp, A]

  /**
   * Catchable instance for [[NClobIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableNClobIO: Catchable[NClobIO] =
    new Catchable[NClobIO] {
      def attempt[A](f: NClobIO[A]): NClobIO[Throwable \/ A] = nclob.attempt(f)
      def fail[A](err: Throwable): NClobIO[A] = nclob.delay(throw err)
    }

  /**
   * Capture instance for [[NClobIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureNClobIO: Capture[NClobIO] =
    new Capture[NClobIO] {
      def apply[A](a: => A): NClobIO[A] = nclob.delay(a)
    }

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): NClobIO[A] =
    F.liftF(Lift(j, action, mod))

  /** 
   * Lift a NClobIO[A] into an exception-capturing NClobIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: NClobIO[A]): NClobIO[Throwable \/ A] =
    F.liftF[NClobOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): NClobIO[A] =
    F.liftF(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying NClob.
   * @group Constructors (Lifting)
   */
  def raw[A](f: NClob => A): NClobIO[A] =
    F.liftF(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  val free: NClobIO[Unit] =
    F.liftF(Free)

  /** 
   * @group Constructors (Primitives)
   */
  val getAsciiStream: NClobIO[InputStream] =
    F.liftF(GetAsciiStream)

  /** 
   * @group Constructors (Primitives)
   */
  val getCharacterStream: NClobIO[Reader] =
    F.liftF(GetCharacterStream)

  /** 
   * @group Constructors (Primitives)
   */
  def getCharacterStream(a: Long, b: Long): NClobIO[Reader] =
    F.liftF(GetCharacterStream1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def getSubString(a: Long, b: Int): NClobIO[String] =
    F.liftF(GetSubString(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val length: NClobIO[Long] =
    F.liftF(Length)

  /** 
   * @group Constructors (Primitives)
   */
  def position(a: Clob, b: Long): NClobIO[Long] =
    F.liftF(Position(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def position(a: String, b: Long): NClobIO[Long] =
    F.liftF(Position1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setAsciiStream(a: Long): NClobIO[OutputStream] =
    F.liftF(SetAsciiStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setCharacterStream(a: Long): NClobIO[Writer] =
    F.liftF(SetCharacterStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setString(a: Long, b: String, c: Int, d: Int): NClobIO[Int] =
    F.liftF(SetString(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def setString(a: Long, b: String): NClobIO[Int] =
    F.liftF(SetString1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def truncate(a: Long): NClobIO[Unit] =
    F.liftF(Truncate(a))

 /** 
  * Natural transformation from `NClobOp` to `Kleisli` for the given `M`, consuming a `java.sql.NClob`. 
  * @group Algebra
  */
  def interpK[M[_]: Monad: Catchable: Capture]: NClobOp ~> Kleisli[M, NClob, ?] =
   NClobOp.NClobKleisliTrans.interpK

 /** 
  * Natural transformation from `NClobIO` to `Kleisli` for the given `M`, consuming a `java.sql.NClob`. 
  * @group Algebra
  */
  def transK[M[_]: Monad: Catchable: Capture]: NClobIO ~> Kleisli[M, NClob, ?] =
   NClobOp.NClobKleisliTrans.transK

 /** 
  * Natural transformation from `NClobIO` to `M`, given a `java.sql.NClob`. 
  * @group Algebra
  */
 def trans[M[_]: Monad: Catchable: Capture](c: NClob): NClobIO ~> M =
   NClobOp.NClobKleisliTrans.trans[M](c)

  /**
   * Syntax for `NClobIO`.
   * @group Algebra
   */
  implicit class NClobIOOps[A](ma: NClobIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, NClob, A] =
      NClobOp.NClobKleisliTrans.transK[M].apply(ma)
  }

}

