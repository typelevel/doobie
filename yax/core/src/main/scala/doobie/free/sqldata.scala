package doobie.free

#+scalaz
import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
#-scalaz
#+cats
import cats.~>
import cats.data.Kleisli
import cats.free.{ Free => F }
import scala.util.{ Either => \/ }
#-cats
#+fs2
import fs2.util.{ Catchable, Suspendable }
import fs2.interop.cats._
#-fs2

import doobie.util.capture._
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
 * `Free#foldMap`.
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
object sqldata extends SQLDataIOInstances {

  /**
   * Sum type of primitive operations over a `java.sql.SQLData`.
   * @group Algebra
   */
  sealed trait SQLDataOp[A] {
#+scalaz
    protected def primitive[M[_]: Monad: Capture](f: SQLData => A): Kleisli[M, SQLData, A] =
      Kleisli((s: SQLData) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, SQLData, A]
#-scalaz
#+fs2
    protected def primitive[M[_]: Suspendable](f: SQLData => A): Kleisli[M, SQLData, A] =
      Kleisli((s: SQLData) => Predef.implicitly[Suspendable[M]].delay(f(s)))
    def defaultTransK[M[_]: Catchable: Suspendable]: Kleisli[M, SQLData, A]
#-fs2
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
#+scalaz
        def interpK[M[_]: Monad: Catchable: Capture]: SQLDataOp ~> Kleisli[M, SQLData, ?] =
#-scalaz
#+fs2
        def interpK[M[_]: Catchable: Suspendable]: SQLDataOp ~> Kleisli[M, SQLData, ?] =
#-fs2
          new (SQLDataOp ~> Kleisli[M, SQLData, ?]) {
            def apply[A](op: SQLDataOp[A]): Kleisli[M, SQLData, A] =
              op.defaultTransK[M]
          }
      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F[Op, A], mod: KleisliTrans.Aux[Op, J]) extends SQLDataOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = Kleisli(_ => mod.transK[M].apply(action).run(j))
#-fs2
    }

    // Combinators
    case class Attempt[A](action: SQLDataIO[A]) extends SQLDataOp[Throwable \/ A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] =
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] =
#-fs2
        Predef.implicitly[Catchable[Kleisli[M, SQLData, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends SQLDataOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_ => a())
#-fs2
    }
    case class Raw[A](f: SQLData => A) extends SQLDataOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(f)
#-fs2
    }

    // Primitive Operations
#+scalaz
    case object GetSQLTypeName extends SQLDataOp[String] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSQLTypeName())
    }
    case class  ReadSQL(a: SQLInput, b: String) extends SQLDataOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readSQL(a, b))
    }
    case class  WriteSQL(a: SQLOutput) extends SQLDataOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeSQL(a))
    }
#-scalaz
#+fs2
    case object GetSQLTypeName extends SQLDataOp[String] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getSQLTypeName())
    }
    case class  ReadSQL(a: SQLInput, b: String) extends SQLDataOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.readSQL(a, b))
    }
    case class  WriteSQL(a: SQLOutput) extends SQLDataOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.writeSQL(a))
    }
#-fs2

  }
  import SQLDataOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[SQLDataOp]]; abstractly, a computation that consumes
   * a `java.sql.SQLData` and produces a value of type `A`.
   * @group Algebra
   */
  type SQLDataIO[A] = F[SQLDataOp, A]

  /**
   * Catchable instance for [[SQLDataIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableSQLDataIO: Catchable[SQLDataIO] =
    new Catchable[SQLDataIO] {
#+fs2
      def pure[A](a: A): SQLDataIO[A] = sqldata.delay(a)
      override def map[A, B](fa: SQLDataIO[A])(f: A => B): SQLDataIO[B] = fa.map(f)
      def flatMap[A, B](fa: SQLDataIO[A])(f: A => SQLDataIO[B]): SQLDataIO[B] = fa.flatMap(f)
#-fs2
      def attempt[A](f: SQLDataIO[A]): SQLDataIO[Throwable \/ A] = sqldata.attempt(f)
      def fail[A](err: Throwable): SQLDataIO[A] = sqldata.delay(throw err)
    }

#+scalaz
  /**
   * Capture instance for [[SQLDataIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureSQLDataIO: Capture[SQLDataIO] =
    new Capture[SQLDataIO] {
      def apply[A](a: => A): SQLDataIO[A] = sqldata.delay(a)
    }
#-scalaz

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): SQLDataIO[A] =
    F.liftF[SQLDataOp, A](Lift(j, action, mod))

  /**
   * Lift a SQLDataIO[A] into an exception-capturing SQLDataIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: SQLDataIO[A]): SQLDataIO[Throwable \/ A] =
    F.liftF[SQLDataOp, Throwable \/ A](Attempt(a))

  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): SQLDataIO[A] =
    F.liftF(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying SQLData.
   * @group Constructors (Lifting)
   */
  def raw[A](f: SQLData => A): SQLDataIO[A] =
    F.liftF(Raw(f))

  /**
   * @group Constructors (Primitives)
   */
  val getSQLTypeName: SQLDataIO[String] =
    F.liftF(GetSQLTypeName)

  /**
   * @group Constructors (Primitives)
   */
  def readSQL(a: SQLInput, b: String): SQLDataIO[Unit] =
    F.liftF(ReadSQL(a, b))

  /**
   * @group Constructors (Primitives)
   */
  def writeSQL(a: SQLOutput): SQLDataIO[Unit] =
    F.liftF(WriteSQL(a))

 /**
  * Natural transformation from `SQLDataOp` to `Kleisli` for the given `M`, consuming a `java.sql.SQLData`.
  * @group Algebra
  */
#+scalaz
  def interpK[M[_]: Monad: Catchable: Capture]: SQLDataOp ~> Kleisli[M, SQLData, ?] =
   SQLDataOp.SQLDataKleisliTrans.interpK
#-scalaz
#+fs2
  def interpK[M[_]: Catchable: Suspendable]: SQLDataOp ~> Kleisli[M, SQLData, ?] =
   SQLDataOp.SQLDataKleisliTrans.interpK
#-fs2

 /**
  * Natural transformation from `SQLDataIO` to `Kleisli` for the given `M`, consuming a `java.sql.SQLData`.
  * @group Algebra
  */
#+scalaz
  def transK[M[_]: Monad: Catchable: Capture]: SQLDataIO ~> Kleisli[M, SQLData, ?] =
   SQLDataOp.SQLDataKleisliTrans.transK
#-scalaz
#+fs2
  def transK[M[_]: Catchable: Suspendable]: SQLDataIO ~> Kleisli[M, SQLData, ?] =
   SQLDataOp.SQLDataKleisliTrans.transK
#-fs2

 /**
  * Natural transformation from `SQLDataIO` to `M`, given a `java.sql.SQLData`.
  * @group Algebra
  */
#+scalaz
 def trans[M[_]: Monad: Catchable: Capture](c: SQLData): SQLDataIO ~> M =
#-scalaz
#+fs2
 def trans[M[_]: Catchable: Suspendable](c: SQLData): SQLDataIO ~> M =
#-fs2
   SQLDataOp.SQLDataKleisliTrans.trans[M](c)

  /**
   * Syntax for `SQLDataIO`.
   * @group Algebra
   */
  implicit class SQLDataIOOps[A](ma: SQLDataIO[A]) {
#+scalaz
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, SQLData, A] =
#-scalaz
#+fs2
    def transK[M[_]: Catchable: Suspendable]: Kleisli[M, SQLData, A] =
#-fs2
      SQLDataOp.SQLDataKleisliTrans.transK[M].apply(ma)
  }

}

private[free] trait SQLDataIOInstances {
#+fs2
  /**
   * Suspendable instance for [[SQLDataIO]].
   * @group Typeclass Instances
   */
  implicit val SuspendableSQLDataIO: Suspendable[SQLDataIO] =
    new Suspendable[SQLDataIO] {
      def pure[A](a: A): SQLDataIO[A] = sqldata.delay(a)
      override def map[A, B](fa: SQLDataIO[A])(f: A => B): SQLDataIO[B] = fa.map(f)
      def flatMap[A, B](fa: SQLDataIO[A])(f: A => SQLDataIO[B]): SQLDataIO[B] = fa.flatMap(f)
      def suspend[A](fa: => SQLDataIO[A]): SQLDataIO[A] = F.suspend(fa)
      override def delay[A](a: => A): SQLDataIO[A] = sqldata.delay(a)
    }
#-fs2
}

