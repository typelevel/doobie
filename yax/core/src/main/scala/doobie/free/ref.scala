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
 * `Free#foldMap`.
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
object ref extends RefInstances {

  /**
   * Sum type of primitive operations over a `java.sql.Ref`.
   * @group Algebra
   */
  sealed trait RefOp[A] {
#+scalaz
    protected def primitive[M[_]: Monad: Capture](f: Ref => A): Kleisli[M, Ref, A] =
      Kleisli((s: Ref) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Ref, A]
#-scalaz
#+fs2
    protected def primitive[M[_]: Catchable: Suspendable](f: Ref => A): Kleisli[M, Ref, A] =
      Kleisli((s: Ref) => Predef.implicitly[Suspendable[M]].delay(f(s)))
    def defaultTransK[M[_]: Catchable: Suspendable]: Kleisli[M, Ref, A]
#-fs2
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
#+scalaz
        def interpK[M[_]: Monad: Catchable: Capture]: RefOp ~> Kleisli[M, Ref, ?] =
#-scalaz
#+fs2
        def interpK[M[_]: Catchable: Suspendable]: RefOp ~> Kleisli[M, Ref, ?] =
#-fs2
          new (RefOp ~> Kleisli[M, Ref, ?]) {
            def apply[A](op: RefOp[A]): Kleisli[M, Ref, A] =
              op.defaultTransK[M]
          }
      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F[Op, A], mod: KleisliTrans.Aux[Op, J]) extends RefOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = Kleisli(_ => mod.transK[M].apply(action).run(j))
#-fs2
    }

    // Combinators
    case class Attempt[A](action: RefIO[A]) extends RefOp[Throwable \/ A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] =
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] =
#-fs2
        Predef.implicitly[Catchable[Kleisli[M, Ref, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends RefOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_ => a())
#-fs2
    }
    case class Raw[A](f: Ref => A) extends RefOp[A] {
#+scalaz
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
#-scalaz
#+fs2
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(f)
#-fs2
    }

    // Primitive Operations
#+scalaz
    case object GetBaseTypeName extends RefOp[String] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBaseTypeName())
    }
    case object GetObject extends RefOp[AnyRef] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject())
    }
    case class  GetObject1(a: Map[String, Class[_]]) extends RefOp[AnyRef] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a))
    }
    case class  SetObject(a: AnyRef) extends RefOp[Unit] {
      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setObject(a))
    }
#-scalaz
#+fs2
    case object GetBaseTypeName extends RefOp[String] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getBaseTypeName())
    }
    case object GetObject extends RefOp[AnyRef] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getObject())
    }
    case class  GetObject1(a: Map[String, Class[_]]) extends RefOp[AnyRef] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.getObject(a))
    }
    case class  SetObject(a: AnyRef) extends RefOp[Unit] {
      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_.setObject(a))
    }
#-fs2

  }
  import RefOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[RefOp]]; abstractly, a computation that consumes
   * a `java.sql.Ref` and produces a value of type `A`.
   * @group Algebra
   */
  type RefIO[A] = F[RefOp, A]

  /**
   * Catchable instance for [[RefIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableRefIO: Catchable[RefIO] =
    new Catchable[RefIO] {
#+fs2
      def pure[A](a: A): RefIO[A] = ref.delay(a)
      override def map[A, B](fa: RefIO[A])(f: A => B): RefIO[B] = fa.map(f)
      def flatMap[A, B](fa: RefIO[A])(f: A => RefIO[B]): RefIO[B] = fa.flatMap(f)
#-fs2
      def attempt[A](f: RefIO[A]): RefIO[Throwable \/ A] = ref.attempt(f)
      def fail[A](err: Throwable): RefIO[A] = ref.delay(throw err)
    }

#+scalaz
  /**
   * Capture instance for [[RefIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureRefIO: Capture[RefIO] =
    new Capture[RefIO] {
      def apply[A](a: => A): RefIO[A] = ref.delay(a)
    }
#-scalaz

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): RefIO[A] =
    F.liftF(Lift(j, action, mod))

  /**
   * Lift a RefIO[A] into an exception-capturing RefIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: RefIO[A]): RefIO[Throwable \/ A] =
    F.liftF[RefOp, Throwable \/ A](Attempt(a))

  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): RefIO[A] =
    F.liftF(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying Ref.
   * @group Constructors (Lifting)
   */
  def raw[A](f: Ref => A): RefIO[A] =
    F.liftF(Raw(f))

  /**
   * @group Constructors (Primitives)
   */
  val getBaseTypeName: RefIO[String] =
    F.liftF(GetBaseTypeName)

  /**
   * @group Constructors (Primitives)
   */
  val getObject: RefIO[AnyRef] =
    F.liftF(GetObject)

  /**
   * @group Constructors (Primitives)
   */
  def getObject(a: Map[String, Class[_]]): RefIO[AnyRef] =
    F.liftF(GetObject1(a))

  /**
   * @group Constructors (Primitives)
   */
  def setObject(a: AnyRef): RefIO[Unit] =
    F.liftF(SetObject(a))

 /**
  * Natural transformation from `RefOp` to `Kleisli` for the given `M`, consuming a `java.sql.Ref`.
  * @group Algebra
  */
#+scalaz
  def interpK[M[_]: Monad: Catchable: Capture]: RefOp ~> Kleisli[M, Ref, ?] =
   RefOp.RefKleisliTrans.interpK
#-scalaz
#+fs2
  def interpK[M[_]: Catchable: Suspendable]: RefOp ~> Kleisli[M, Ref, ?] =
   RefOp.RefKleisliTrans.interpK
#-fs2

 /**
  * Natural transformation from `RefIO` to `Kleisli` for the given `M`, consuming a `java.sql.Ref`.
  * @group Algebra
  */
#+scalaz
  def transK[M[_]: Monad: Catchable: Capture]: RefIO ~> Kleisli[M, Ref, ?] =
   RefOp.RefKleisliTrans.transK
#-scalaz
#+fs2
  def transK[M[_]: Catchable: Suspendable]: RefIO ~> Kleisli[M, Ref, ?] =
   RefOp.RefKleisliTrans.transK
#-fs2

 /**
  * Natural transformation from `RefIO` to `M`, given a `java.sql.Ref`.
  * @group Algebra
  */
#+scalaz
 def trans[M[_]: Monad: Catchable: Capture](c: Ref): RefIO ~> M =
   RefOp.RefKleisliTrans.trans[M](c)
#-scalaz
#+fs2
 def trans[M[_]: Catchable: Suspendable](c: Ref): RefIO ~> M =
   RefOp.RefKleisliTrans.trans[M](c)
#-fs2

  /**
   * Syntax for `RefIO`.
   * @group Algebra
   */
  implicit class RefIOOps[A](ma: RefIO[A]) {
#+scalaz
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Ref, A] =
      RefOp.RefKleisliTrans.transK[M].apply(ma)
#-scalaz
#+fs2
    def transK[M[_]: Catchable: Suspendable]: Kleisli[M, Ref, A] =
      RefOp.RefKleisliTrans.transK[M].apply(ma)
#-fs2
  }

}

private[free] trait RefInstances {
#+fs2
  /**
   * Suspendable instance for [[RefIO]].
   * @group Typeclass Instances
   */
  implicit val SuspendableRefIO: Suspendable[RefIO] =
    new Suspendable[RefIO] {
      def pure[A](a: A): RefIO[A] = ref.delay(a)
      override def map[A, B](fa: RefIO[A])(f: A => B): RefIO[B] = fa.map(f)
      def flatMap[A, B](fa: RefIO[A])(f: A => RefIO[B]): RefIO[B] = fa.flatMap(f)
      def suspend[A](fa: => RefIO[A]): RefIO[A] = F.suspend(fa)
      override def delay[A](a: => A): RefIO[A] = ref.delay(a)
    }
#-fs2
}

