package doobie.free

import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

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
object ref {
  
  /** 
   * Sum type of primitive operations over a `java.sql.Ref`.
   * @group Algebra 
   */
  sealed trait RefOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: Ref => A): Kleisli[M, Ref, A] = 
      Kleisli((s: Ref) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Ref, A]
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
      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F[Op, A], mod: KleisliTrans.Aux[Op, J]) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
    }

    // Combinators
    case class Attempt[A](action: RefIO[A]) extends RefOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, Ref, ?]]].attempt(action.transK[M])
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
    case class  GetObject(a: Map[String, Class[_]]) extends RefOp[Object] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject(a))
    }
    case object GetObject1 extends RefOp[Object] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getObject())
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
  type RefIO[A] = F[RefOp, A]

  /**
   * Catchable instance for [[RefIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableRefIO: Catchable[RefIO] =
    new Catchable[RefIO] {
      def attempt[A](f: RefIO[A]): RefIO[Throwable \/ A] = ref.attempt(f)
      def fail[A](err: Throwable): RefIO[A] = ref.delay(throw err)
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
  def getObject(a: Map[String, Class[_]]): RefIO[Object] =
    F.liftF(GetObject(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getObject: RefIO[Object] =
    F.liftF(GetObject1)

  /** 
   * @group Constructors (Primitives)
   */
  def setObject(a: Object): RefIO[Unit] =
    F.liftF(SetObject(a))

 /** 
  * Natural transformation from `RefOp` to `Kleisli` for the given `M`, consuming a `java.sql.Ref`. 
  * @group Algebra
  */
  def interpK[M[_]: Monad: Catchable: Capture]: RefOp ~> Kleisli[M, Ref, ?] =
   RefOp.RefKleisliTrans.interpK

 /** 
  * Natural transformation from `RefIO` to `Kleisli` for the given `M`, consuming a `java.sql.Ref`. 
  * @group Algebra
  */
  def transK[M[_]: Monad: Catchable: Capture]: RefIO ~> Kleisli[M, Ref, ?] =
   RefOp.RefKleisliTrans.transK

 /** 
  * Natural transformation from `RefIO` to `M`, given a `java.sql.Ref`. 
  * @group Algebra
  */
 def trans[M[_]: Monad: Catchable: Capture](c: Ref): RefIO ~> M =
   RefOp.RefKleisliTrans.trans[M](c)

  /**
   * Syntax for `RefIO`.
   * @group Algebra
   */
  implicit class RefIOOps[A](ma: RefIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Ref, A] =
      RefOp.RefKleisliTrans.transK[M].apply(ma)
  }

}

