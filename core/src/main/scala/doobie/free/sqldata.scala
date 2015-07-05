package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

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
  sealed trait SQLDataOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: SQLData => A): Kleisli[M, SQLData, A] = 
      Kleisli((s: SQLData) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, SQLData, A]
  }

  /** 
   * Module of constructors for `SQLDataOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `sqldata` module.
   * @group Algebra 
   */
  object SQLDataOp {
    
    // Lifting
    case class LiftBlobIO[A](s: Blob, action: BlobIO[A]) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftCallableStatementIO[A](s: CallableStatement, action: CallableStatementIO[A]) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftClobIO[A](s: Clob, action: ClobIO[A]) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftConnectionIO[A](s: Connection, action: ConnectionIO[A]) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDatabaseMetaDataIO[A](s: DatabaseMetaData, action: DatabaseMetaDataIO[A]) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDriverIO[A](s: Driver, action: DriverIO[A]) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftNClobIO[A](s: NClob, action: NClobIO[A]) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftPreparedStatementIO[A](s: PreparedStatement, action: PreparedStatementIO[A]) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftRefIO[A](s: Ref, action: RefIO[A]) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftResultSetIO[A](s: ResultSet, action: ResultSetIO[A]) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLInputIO[A](s: SQLInput, action: SQLInputIO[A]) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLOutputIO[A](s: SQLOutput, action: SQLOutputIO[A]) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftStatementIO[A](s: Statement, action: StatementIO[A]) extends SQLDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }

    // Combinators
    case class Attempt[A](action: SQLDataIO[A]) extends SQLDataOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, SQLData, ?]]].attempt(action.transK[M])
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
      def fail[A](err: Throwable): SQLDataIO[A] = sqldata.delay(throw err)
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
   * @group Constructors (Lifting)
   */
  def liftBlob[A](s: Blob, k: BlobIO[A]): SQLDataIO[A] =
    F.liftFC(LiftBlobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftCallableStatement[A](s: CallableStatement, k: CallableStatementIO[A]): SQLDataIO[A] =
    F.liftFC(LiftCallableStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftClob[A](s: Clob, k: ClobIO[A]): SQLDataIO[A] =
    F.liftFC(LiftClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftConnection[A](s: Connection, k: ConnectionIO[A]): SQLDataIO[A] =
    F.liftFC(LiftConnectionIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDatabaseMetaData[A](s: DatabaseMetaData, k: DatabaseMetaDataIO[A]): SQLDataIO[A] =
    F.liftFC(LiftDatabaseMetaDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDriver[A](s: Driver, k: DriverIO[A]): SQLDataIO[A] =
    F.liftFC(LiftDriverIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftNClob[A](s: NClob, k: NClobIO[A]): SQLDataIO[A] =
    F.liftFC(LiftNClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftPreparedStatement[A](s: PreparedStatement, k: PreparedStatementIO[A]): SQLDataIO[A] =
    F.liftFC(LiftPreparedStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftRef[A](s: Ref, k: RefIO[A]): SQLDataIO[A] =
    F.liftFC(LiftRefIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftResultSet[A](s: ResultSet, k: ResultSetIO[A]): SQLDataIO[A] =
    F.liftFC(LiftResultSetIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLInput[A](s: SQLInput, k: SQLInputIO[A]): SQLDataIO[A] =
    F.liftFC(LiftSQLInputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLOutput[A](s: SQLOutput, k: SQLOutputIO[A]): SQLDataIO[A] =
    F.liftFC(LiftSQLOutputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftStatement[A](s: Statement, k: StatementIO[A]): SQLDataIO[A] =
    F.liftFC(LiftStatementIO(s, k))

  /** 
   * Lift a SQLDataIO[A] into an exception-capturing SQLDataIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: SQLDataIO[A]): SQLDataIO[Throwable \/ A] =
    F.liftFC[SQLDataOp, Throwable \/ A](Attempt(a))
 
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

 /** 
  * Natural transformation from `SQLDataOp` to `Kleisli` for the given `M`, consuming a `java.sql.SQLData`. 
  * @group Algebra
  */
  def kleisliTrans[M[_]: Monad: Catchable: Capture]: SQLDataOp ~> Kleisli[M, SQLData, ?] =
    new (SQLDataOp ~> Kleisli[M, SQLData, ?]) {
      def apply[A](op: SQLDataOp[A]): Kleisli[M, SQLData, A] =
        op.defaultTransK[M]
    }

  /**
   * Syntax for `SQLDataIO`.
   * @group Algebra
   */
  implicit class SQLDataIOOps[A](ma: SQLDataIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, SQLData, A] =
      F.runFC[SQLDataOp, Kleisli[M, SQLData, ?], A](ma)(kleisliTrans[M])
  }

}

