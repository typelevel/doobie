package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

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
    
    // Lifting
    case class LiftBlobIO[A](s: Blob, action: BlobIO[A]) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftCallableStatementIO[A](s: CallableStatement, action: CallableStatementIO[A]) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftClobIO[A](s: Clob, action: ClobIO[A]) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftConnectionIO[A](s: Connection, action: ConnectionIO[A]) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDatabaseMetaDataIO[A](s: DatabaseMetaData, action: DatabaseMetaDataIO[A]) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDriverIO[A](s: Driver, action: DriverIO[A]) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftNClobIO[A](s: NClob, action: NClobIO[A]) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftPreparedStatementIO[A](s: PreparedStatement, action: PreparedStatementIO[A]) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftResultSetIO[A](s: ResultSet, action: ResultSetIO[A]) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLDataIO[A](s: SQLData, action: SQLDataIO[A]) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLInputIO[A](s: SQLInput, action: SQLInputIO[A]) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLOutputIO[A](s: SQLOutput, action: SQLOutputIO[A]) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftStatementIO[A](s: Statement, action: StatementIO[A]) extends RefOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
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
   * @group Constructors (Lifting)
   */
  def liftBlob[A](s: Blob, k: BlobIO[A]): RefIO[A] =
    F.liftFC(LiftBlobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftCallableStatement[A](s: CallableStatement, k: CallableStatementIO[A]): RefIO[A] =
    F.liftFC(LiftCallableStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftClob[A](s: Clob, k: ClobIO[A]): RefIO[A] =
    F.liftFC(LiftClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftConnection[A](s: Connection, k: ConnectionIO[A]): RefIO[A] =
    F.liftFC(LiftConnectionIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDatabaseMetaData[A](s: DatabaseMetaData, k: DatabaseMetaDataIO[A]): RefIO[A] =
    F.liftFC(LiftDatabaseMetaDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDriver[A](s: Driver, k: DriverIO[A]): RefIO[A] =
    F.liftFC(LiftDriverIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftNClob[A](s: NClob, k: NClobIO[A]): RefIO[A] =
    F.liftFC(LiftNClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftPreparedStatement[A](s: PreparedStatement, k: PreparedStatementIO[A]): RefIO[A] =
    F.liftFC(LiftPreparedStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftResultSet[A](s: ResultSet, k: ResultSetIO[A]): RefIO[A] =
    F.liftFC(LiftResultSetIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLData[A](s: SQLData, k: SQLDataIO[A]): RefIO[A] =
    F.liftFC(LiftSQLDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLInput[A](s: SQLInput, k: SQLInputIO[A]): RefIO[A] =
    F.liftFC(LiftSQLInputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLOutput[A](s: SQLOutput, k: SQLOutputIO[A]): RefIO[A] =
    F.liftFC(LiftSQLOutputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftStatement[A](s: Statement, k: StatementIO[A]): RefIO[A] =
    F.liftFC(LiftStatementIO(s, k))

  /** 
   * Lift a RefIO[A] into an exception-capturing RefIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: RefIO[A]): RefIO[Throwable \/ A] =
    F.liftFC[RefOp, Throwable \/ A](Attempt(a))
 
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

 /** 
  * Natural transformation from `RefOp` to `Kleisli` for the given `M`, consuming a `java.sql.Ref`. 
  * @group Algebra
  */
  def kleisliTrans[M[_]: Monad: Catchable: Capture]: RefOp ~> Kleisli[M, Ref, ?] =
    new (RefOp ~> Kleisli[M, Ref, ?]) {
      def apply[A](op: RefOp[A]): Kleisli[M, Ref, A] =
        op.defaultTransK[M]
    }

  /**
   * Syntax for `RefIO`.
   * @group Algebra
   */
  implicit class RefIOOps[A](ma: RefIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Ref, A] =
      F.runFC[RefOp, Kleisli[M, Ref, ?], A](ma)(kleisliTrans[M])
  }

}

