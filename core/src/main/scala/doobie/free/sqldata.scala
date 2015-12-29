package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.syntax.catchable._
import scalaz.syntax.monad._
import scalaz.concurrent.Task

import doobie.util.capture._
import doobie.util.trace.{ Trace, TraceOp }
import doobie.free.kleislitrans._

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
 * Algebra and free monad for primitive operations over a `java.sql.SQLData`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `SQLDataIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `SQLDataOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, SQLData, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: SQLDataIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: SQLData = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object sqldata {
  
  /** 
   * Sum type of primitive operations over a `java.sql.SQLData`.
   * @group Algebra 
   */
  sealed trait SQLDataOp[A] extends TraceOp[SQLData, A] {
 
    protected def primitive[M[_]: Monad: Capture](f: SQLData => A): Kleisli[M, SQLData, A] = 
      Kleisli((s: SQLData) => Capture[M].apply(f(s)))

    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, SQLData, A]

    def defaultTransKL[M[_]: Monad: Catchable: Capture]: Kleisli[M, (Trace[M], SQLData), A] =
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
   * Module of constructors for `SQLDataOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `sqldata` module.
   * @group Algebra 
   */
  object SQLDataOp {
    
    // This algebra has a default interpreter
    implicit val SQLDataKleisliTrans: KleisliTrans.Aux[SQLDataOp, SQLData] =
      new KleisliTrans[SQLDataOp] {
        type J = SQLData

        def interpK[M[_]: Monad: Catchable: Capture]: SQLDataOp ~> Kleisli[M, SQLData, ?] =
          new (SQLDataOp ~> Kleisli[M, SQLData, ?]) {
            def apply[A](op: SQLDataOp[A]): Kleisli[M, SQLData, A] =
              op.defaultTransK[M]
          }

        def interpKL[M[_]: Monad: Catchable: Capture]: SQLDataOp ~> Kleisli[M, (Trace[M], SQLData), ?] =
          new (SQLDataOp ~> Kleisli[M, (Trace[M], SQLData), ?]) {
            def apply[A](op: SQLDataOp[A]): Kleisli[M, (Trace[M], SQLData), A] =
              op.defaultTransKL[M]
          }

      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F.FreeC[Op, A], mod: KleisliTrans.Aux[Op, J]) extends SQLDataOp[A] {
  
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
    case class Attempt[A](action: SQLDataIO[A]) extends SQLDataOp[Throwable \/ A] {
      import scalaz._, Scalaz._

      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, SQLData, ?]]].attempt(SQLDataKleisliTrans.transK[M].apply(action))

      override def defaultTransKL[M[_]: Monad: Catchable: Capture] =
        Kleisli { case (log, c) =>
          for {
            k <- log.log(c, this)
            x <- Predef.implicitly[Catchable[Kleisli[M, (Trace[M], SQLData), ?]]].attempt(SQLDataKleisliTrans.transKL[M].apply(action)).run((log, c))
            _ <- k(\/-(x))
         } yield x
       }

    }

    case class Fail[A](t: Throwable) extends SQLDataOp[A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, SQLData, ?]]].fail[A](t)
    }

    case class Pure[A](a: () => A) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: SQLData => A) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case object GetSQLTypeName extends SQLDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSQLTypeName())
    }
    case class  ReadSQL(a: SQLInput, b: String) extends SQLDataOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readSQL(a, b))
    }
    case class  WriteSQL(a: SQLOutput) extends SQLDataOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeSQL(a))
    }

  }
  import SQLDataOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[SQLDataOp]]; abstractly, a computation that consumes 
   * a `java.sql.SQLData` and produces a value of type `A`. 
   * @group Algebra 
   */
  type SQLDataIO[A] = F.FreeC[SQLDataOp, A]

  /**
   * Monad instance for [[SQLDataIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadSQLDataIO: Monad[SQLDataIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[SQLDataOp, α]})#λ]

  /**
   * Catchable instance for [[SQLDataIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableSQLDataIO: Catchable[SQLDataIO] =
    new Catchable[SQLDataIO] {
      def attempt[A](f: SQLDataIO[A]): SQLDataIO[Throwable \/ A] = sqldata.attempt(f)
      def fail[A](err: Throwable): SQLDataIO[A] = sqldata.fail(err)
    }

  /**
   * Capture instance for [[SQLDataIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureSQLDataIO: Capture[SQLDataIO] =
    new Capture[SQLDataIO] {
      def apply[A](a: => A): SQLDataIO[A] = sqldata.delay(a)
    }

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F.FreeC[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): SQLDataIO[A] =
    F.liftFC(Lift(j, action, mod))

  /** 
   * Lift a SQLDataIO[A] into an exception-capturing SQLDataIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: SQLDataIO[A]): SQLDataIO[Throwable \/ A] =
    F.liftFC[SQLDataOp, Throwable \/ A](Attempt(a))
 
  def fail[A](t: Throwable): SQLDataIO[A] =
    F.liftFC(Fail(t))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): SQLDataIO[A] =
    F.liftFC(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying SQLData.
   * @group Constructors (Lifting)
   */
  def raw[A](f: SQLData => A): SQLDataIO[A] =
    F.liftFC(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  val getSQLTypeName: SQLDataIO[String] =
    F.liftFC(GetSQLTypeName)

  /** 
   * @group Constructors (Primitives)
   */
  def readSQL(a: SQLInput, b: String): SQLDataIO[Unit] =
    F.liftFC(ReadSQL(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def writeSQL(a: SQLOutput): SQLDataIO[Unit] =
    F.liftFC(WriteSQL(a))

}

