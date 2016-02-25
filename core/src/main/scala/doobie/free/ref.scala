package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.syntax.catchable._
import scalaz.syntax.monad._
import scalaz.concurrent.Task

import doobie.util.capture._
import doobie.util.trace.{ Trace, TraceOp }
import doobie.free.kleislitrans._

import java.lang.Object
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
import java.util.Map

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
 * Algebra and free monad for primitive operations over a `java.sql.Ref`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `RefIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `RefOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, Ref, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: RefIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: Ref = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object ref {
  
  /** 
   * Sum type of primitive operations over a `java.sql.Ref`.
   * @group Algebra 
   */
  sealed trait RefOp[A] extends TraceOp[Ref, A] {
 
    protected def primitive[M[_]: Monad: Capture](f: Ref => A): Kleisli[M, Ref, A] = 
      Kleisli((s: Ref) => Capture[M].apply(f(s)))

    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Ref, A]

    def defaultTransKL[M[_]: Monad: Catchable: Capture]: Kleisli[M, (Trace[M], Ref), A] =
      Kleisli { case (log, c) =>
        for {
          k <- log.log(c, this)
          x <- defaultTransK[M].attempt.run(c)
          _ <- k(x)
          a <- x.fold[M[A]](Catchable[M].fail(_), _.point[M])
        } yield a
      }

  }

  /** 
   * Module of constructors for `RefOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `ref` module.
   * @group Algebra 
   */
  object RefOp {
    
    // This algebra has a default interpreter
    implicit val RefKleisliTrans: KleisliTrans.Aux[RefOp, Ref] =
      new KleisliTrans[RefOp] {
        type J = Ref

        def interpK[M[_]: Monad: Catchable: Capture]: RefOp ~> Kleisli[M, Ref, ?] =
          new (RefOp ~> Kleisli[M, Ref, ?]) {
            def apply[A](op: RefOp[A]): Kleisli[M, Ref, A] =
              op.defaultTransK[M]
          }

        def interpKL[M[_]: Monad: Catchable: Capture]: RefOp ~> Kleisli[M, (Trace[M], Ref), ?] =
          new (RefOp ~> Kleisli[M, (Trace[M], Ref), ?]) {
            def apply[A](op: RefOp[A]): Kleisli[M, (Trace[M], Ref), A] =
              op.defaultTransKL[M]
          }

      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F.FreeC[Op, A], mod: KleisliTrans.Aux[Op, J]) extends RefOp[A] {
  
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Kleisli(_ => mod.transK[M].apply(action).run(j))

      override def defaultTransKL[M[_]: Monad: Catchable: Capture] =
        Kleisli { case (log, c) =>
          for {
            k <- log.log(c, this)
            x <- mod.transKL[M].apply(action).attempt.run((log, j))
            _ <- k(x)
            a <- x.fold[M[A]](Catchable[M].fail(_), _.point[M])
          } yield a
        }

    }

    // Combinators
    case class Attempt[A](action: RefIO[A]) extends RefOp[Throwable \/ A] {
      import scalaz._, Scalaz._

      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, Ref, ?]]].attempt(RefKleisliTrans.transK[M].apply(action))

      override def defaultTransKL[M[_]: Monad: Catchable: Capture] =
        Kleisli { case (log, c) =>
          for {
            k <- log.log(c, this)
            x <- Predef.implicitly[Catchable[Kleisli[M, (Trace[M], Ref), ?]]].attempt(RefKleisliTrans.transKL[M].apply(action)).run((log, c))
            _ <- k(\/-(x))
         } yield x
       }

    }

    case class Fail[A](t: Throwable) extends RefOp[A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, Ref, ?]]].fail[A](t)
    }

    case class Pure[A](a: () => A) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: Ref => A) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case object GetBaseTypeName extends RefOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBaseTypeName())
    }
    case object GetObject extends RefOp[Object] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject())
    }
    case class  GetObject1(a: Map[String, Class[_]]) extends RefOp[Object] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a))
    }
    case class  SetObject(a: Object) extends RefOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a))
    }

  }
  import RefOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[RefOp]]; abstractly, a computation that consumes 
   * a `java.sql.Ref` and produces a value of type `A`. 
   * @group Algebra 
   */
  type RefIO[A] = F.FreeC[RefOp, A]

  /**
   * Monad instance for [[RefIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadRefIO: Monad[RefIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[RefOp, α]})#λ]

  /**
   * Catchable instance for [[RefIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableRefIO: Catchable[RefIO] =
    new Catchable[RefIO] {
      def attempt[A](f: RefIO[A]): RefIO[Throwable \/ A] = ref.attempt(f)
      def fail[A](err: Throwable): RefIO[A] = ref.fail(err)
    }

  /**
   * Capture instance for [[RefIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureRefIO: Capture[RefIO] =
    new Capture[RefIO] {
      def apply[A](a: => A): RefIO[A] = ref.delay(a)
    }

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F.FreeC[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): RefIO[A] =
    F.liftFC(Lift(j, action, mod))

  /** 
   * Lift a RefIO[A] into an exception-capturing RefIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: RefIO[A]): RefIO[Throwable \/ A] =
    F.liftFC[RefOp, Throwable \/ A](Attempt(a))
 
  def fail[A](t: Throwable): RefIO[A] =
    F.liftFC(Fail(t))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): RefIO[A] =
    F.liftFC(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying Ref.
   * @group Constructors (Lifting)
   */
  def raw[A](f: Ref => A): RefIO[A] =
    F.liftFC(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  val getBaseTypeName: RefIO[String] =
    F.liftFC(GetBaseTypeName)

  /** 
   * @group Constructors (Primitives)
   */
  val getObject: RefIO[Object] =
    F.liftFC(GetObject)

  /** 
   * @group Constructors (Primitives)
   */
  def getObject(a: Map[String, Class[_]]): RefIO[Object] =
    F.liftFC(GetObject1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setObject(a: Object): RefIO[Unit] =
    F.liftFC(SetObject(a))

}

