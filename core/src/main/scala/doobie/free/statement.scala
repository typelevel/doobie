package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import java.lang.Class
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
import java.sql.SQLWarning
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
 * Algebra and free monad for primitive operations over a `java.sql.Statement`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `StatementIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `StatementOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, Statement, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: StatementIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: Statement = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object statement {
  
  /** 
   * Sum type of primitive operations over a `java.sql.Statement`.
   * @group Algebra 
   */
  sealed trait StatementOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: Statement => A): Kleisli[M, Statement, A] = 
      Kleisli((s: Statement) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Statement, A]
  }

  /** 
   * Module of constructors for `StatementOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `statement` module.
   * @group Algebra 
   */
  object StatementOp {
    
    // Lifting
    case class LiftBlobIO[A](s: Blob, action: BlobIO[A]) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftCallableStatementIO[A](s: CallableStatement, action: CallableStatementIO[A]) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftClobIO[A](s: Clob, action: ClobIO[A]) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftConnectionIO[A](s: Connection, action: ConnectionIO[A]) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDatabaseMetaDataIO[A](s: DatabaseMetaData, action: DatabaseMetaDataIO[A]) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftDriverIO[A](s: Driver, action: DriverIO[A]) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftNClobIO[A](s: NClob, action: NClobIO[A]) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftPreparedStatementIO[A](s: PreparedStatement, action: PreparedStatementIO[A]) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftRefIO[A](s: Ref, action: RefIO[A]) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftResultSetIO[A](s: ResultSet, action: ResultSetIO[A]) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLDataIO[A](s: SQLData, action: SQLDataIO[A]) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLInputIO[A](s: SQLInput, action: SQLInputIO[A]) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }
    case class LiftSQLOutputIO[A](s: SQLOutput, action: SQLOutputIO[A]) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => action.transK[M].run(s))
    }

    // Combinators
    case class Attempt[A](action: StatementIO[A]) extends StatementOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, Statement, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: Statement => A) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case class  AddBatch(a: String) extends StatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.addBatch(a))
    }
    case object Cancel extends StatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.cancel())
    }
    case object ClearBatch extends StatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.clearBatch())
    }
    case object ClearWarnings extends StatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.clearWarnings())
    }
    case object Close extends StatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.close())
    }
    case object CloseOnCompletion extends StatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.closeOnCompletion())
    }
    case class  Execute(a: String, b: Int) extends StatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a, b))
    }
    case class  Execute1(a: String, b: Array[Int]) extends StatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a, b))
    }
    case class  Execute2(a: String) extends StatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a))
    }
    case class  Execute3(a: String, b: Array[String]) extends StatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a, b))
    }
    case object ExecuteBatch extends StatementOp[Array[Int]] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeBatch())
    }
    case class  ExecuteQuery(a: String) extends StatementOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeQuery(a))
    }
    case class  ExecuteUpdate(a: String, b: Int) extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate1(a: String, b: Array[Int]) extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate2(a: String, b: Array[String]) extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate3(a: String) extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a))
    }
    case object GetConnection extends StatementOp[Connection] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getConnection())
    }
    case object GetFetchDirection extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFetchDirection())
    }
    case object GetFetchSize extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFetchSize())
    }
    case object GetGeneratedKeys extends StatementOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getGeneratedKeys())
    }
    case object GetMaxFieldSize extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxFieldSize())
    }
    case object GetMaxRows extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxRows())
    }
    case class  GetMoreResults(a: Int) extends StatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMoreResults(a))
    }
    case object GetMoreResults1 extends StatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMoreResults())
    }
    case object GetQueryTimeout extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getQueryTimeout())
    }
    case object GetResultSet extends StatementOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSet())
    }
    case object GetResultSetConcurrency extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSetConcurrency())
    }
    case object GetResultSetHoldability extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSetHoldability())
    }
    case object GetResultSetType extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSetType())
    }
    case object GetUpdateCount extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getUpdateCount())
    }
    case object GetWarnings extends StatementOp[SQLWarning] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getWarnings())
    }
    case object IsCloseOnCompletion extends StatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isCloseOnCompletion())
    }
    case object IsClosed extends StatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isClosed())
    }
    case object IsPoolable extends StatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isPoolable())
    }
    case class  IsWrapperFor(a: Class[_]) extends StatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isWrapperFor(a))
    }
    case class  SetCursorName(a: String) extends StatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCursorName(a))
    }
    case class  SetEscapeProcessing(a: Boolean) extends StatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setEscapeProcessing(a))
    }
    case class  SetFetchDirection(a: Int) extends StatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setFetchDirection(a))
    }
    case class  SetFetchSize(a: Int) extends StatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setFetchSize(a))
    }
    case class  SetMaxFieldSize(a: Int) extends StatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setMaxFieldSize(a))
    }
    case class  SetMaxRows(a: Int) extends StatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setMaxRows(a))
    }
    case class  SetPoolable(a: Boolean) extends StatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setPoolable(a))
    }
    case class  SetQueryTimeout(a: Int) extends StatementOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setQueryTimeout(a))
    }
    case class  Unwrap[T](a: Class[T]) extends StatementOp[T] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.unwrap(a))
    }

  }
  import StatementOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[StatementOp]]; abstractly, a computation that consumes 
   * a `java.sql.Statement` and produces a value of type `A`. 
   * @group Algebra 
   */
  type StatementIO[A] = F.FreeC[StatementOp, A]

  /**
   * Monad instance for [[StatementIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadStatementIO: Monad[StatementIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[StatementOp, α]})#λ]

  /**
   * Catchable instance for [[StatementIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableStatementIO: Catchable[StatementIO] =
    new Catchable[StatementIO] {
      def attempt[A](f: StatementIO[A]): StatementIO[Throwable \/ A] = statement.attempt(f)
      def fail[A](err: Throwable): StatementIO[A] = statement.delay(throw err)
    }

  /**
   * Capture instance for [[StatementIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureStatementIO: Capture[StatementIO] =
    new Capture[StatementIO] {
      def apply[A](a: => A): StatementIO[A] = statement.delay(a)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftBlob[A](s: Blob, k: BlobIO[A]): StatementIO[A] =
    F.liftFC(LiftBlobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftCallableStatement[A](s: CallableStatement, k: CallableStatementIO[A]): StatementIO[A] =
    F.liftFC(LiftCallableStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftClob[A](s: Clob, k: ClobIO[A]): StatementIO[A] =
    F.liftFC(LiftClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftConnection[A](s: Connection, k: ConnectionIO[A]): StatementIO[A] =
    F.liftFC(LiftConnectionIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDatabaseMetaData[A](s: DatabaseMetaData, k: DatabaseMetaDataIO[A]): StatementIO[A] =
    F.liftFC(LiftDatabaseMetaDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDriver[A](s: Driver, k: DriverIO[A]): StatementIO[A] =
    F.liftFC(LiftDriverIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftNClob[A](s: NClob, k: NClobIO[A]): StatementIO[A] =
    F.liftFC(LiftNClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftPreparedStatement[A](s: PreparedStatement, k: PreparedStatementIO[A]): StatementIO[A] =
    F.liftFC(LiftPreparedStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftRef[A](s: Ref, k: RefIO[A]): StatementIO[A] =
    F.liftFC(LiftRefIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftResultSet[A](s: ResultSet, k: ResultSetIO[A]): StatementIO[A] =
    F.liftFC(LiftResultSetIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLData[A](s: SQLData, k: SQLDataIO[A]): StatementIO[A] =
    F.liftFC(LiftSQLDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLInput[A](s: SQLInput, k: SQLInputIO[A]): StatementIO[A] =
    F.liftFC(LiftSQLInputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLOutput[A](s: SQLOutput, k: SQLOutputIO[A]): StatementIO[A] =
    F.liftFC(LiftSQLOutputIO(s, k))

  /** 
   * Lift a StatementIO[A] into an exception-capturing StatementIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: StatementIO[A]): StatementIO[Throwable \/ A] =
    F.liftFC[StatementOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): StatementIO[A] =
    F.liftFC(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying Statement.
   * @group Constructors (Lifting)
   */
  def raw[A](f: Statement => A): StatementIO[A] =
    F.liftFC(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  def addBatch(a: String): StatementIO[Unit] =
    F.liftFC(AddBatch(a))

  /** 
   * @group Constructors (Primitives)
   */
  val cancel: StatementIO[Unit] =
    F.liftFC(Cancel)

  /** 
   * @group Constructors (Primitives)
   */
  val clearBatch: StatementIO[Unit] =
    F.liftFC(ClearBatch)

  /** 
   * @group Constructors (Primitives)
   */
  val clearWarnings: StatementIO[Unit] =
    F.liftFC(ClearWarnings)

  /** 
   * @group Constructors (Primitives)
   */
  val close: StatementIO[Unit] =
    F.liftFC(Close)

  /** 
   * @group Constructors (Primitives)
   */
  val closeOnCompletion: StatementIO[Unit] =
    F.liftFC(CloseOnCompletion)

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Int): StatementIO[Boolean] =
    F.liftFC(Execute(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Array[Int]): StatementIO[Boolean] =
    F.liftFC(Execute1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String): StatementIO[Boolean] =
    F.liftFC(Execute2(a))

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Array[String]): StatementIO[Boolean] =
    F.liftFC(Execute3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val executeBatch: StatementIO[Array[Int]] =
    F.liftFC(ExecuteBatch)

  /** 
   * @group Constructors (Primitives)
   */
  def executeQuery(a: String): StatementIO[ResultSet] =
    F.liftFC(ExecuteQuery(a))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Int): StatementIO[Int] =
    F.liftFC(ExecuteUpdate(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Array[Int]): StatementIO[Int] =
    F.liftFC(ExecuteUpdate1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Array[String]): StatementIO[Int] =
    F.liftFC(ExecuteUpdate2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String): StatementIO[Int] =
    F.liftFC(ExecuteUpdate3(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getConnection: StatementIO[Connection] =
    F.liftFC(GetConnection)

  /** 
   * @group Constructors (Primitives)
   */
  val getFetchDirection: StatementIO[Int] =
    F.liftFC(GetFetchDirection)

  /** 
   * @group Constructors (Primitives)
   */
  val getFetchSize: StatementIO[Int] =
    F.liftFC(GetFetchSize)

  /** 
   * @group Constructors (Primitives)
   */
  val getGeneratedKeys: StatementIO[ResultSet] =
    F.liftFC(GetGeneratedKeys)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxFieldSize: StatementIO[Int] =
    F.liftFC(GetMaxFieldSize)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxRows: StatementIO[Int] =
    F.liftFC(GetMaxRows)

  /** 
   * @group Constructors (Primitives)
   */
  def getMoreResults(a: Int): StatementIO[Boolean] =
    F.liftFC(GetMoreResults(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getMoreResults: StatementIO[Boolean] =
    F.liftFC(GetMoreResults1)

  /** 
   * @group Constructors (Primitives)
   */
  val getQueryTimeout: StatementIO[Int] =
    F.liftFC(GetQueryTimeout)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSet: StatementIO[ResultSet] =
    F.liftFC(GetResultSet)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSetConcurrency: StatementIO[Int] =
    F.liftFC(GetResultSetConcurrency)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSetHoldability: StatementIO[Int] =
    F.liftFC(GetResultSetHoldability)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSetType: StatementIO[Int] =
    F.liftFC(GetResultSetType)

  /** 
   * @group Constructors (Primitives)
   */
  val getUpdateCount: StatementIO[Int] =
    F.liftFC(GetUpdateCount)

  /** 
   * @group Constructors (Primitives)
   */
  val getWarnings: StatementIO[SQLWarning] =
    F.liftFC(GetWarnings)

  /** 
   * @group Constructors (Primitives)
   */
  val isCloseOnCompletion: StatementIO[Boolean] =
    F.liftFC(IsCloseOnCompletion)

  /** 
   * @group Constructors (Primitives)
   */
  val isClosed: StatementIO[Boolean] =
    F.liftFC(IsClosed)

  /** 
   * @group Constructors (Primitives)
   */
  val isPoolable: StatementIO[Boolean] =
    F.liftFC(IsPoolable)

  /** 
   * @group Constructors (Primitives)
   */
  def isWrapperFor(a: Class[_]): StatementIO[Boolean] =
    F.liftFC(IsWrapperFor(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setCursorName(a: String): StatementIO[Unit] =
    F.liftFC(SetCursorName(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setEscapeProcessing(a: Boolean): StatementIO[Unit] =
    F.liftFC(SetEscapeProcessing(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setFetchDirection(a: Int): StatementIO[Unit] =
    F.liftFC(SetFetchDirection(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setFetchSize(a: Int): StatementIO[Unit] =
    F.liftFC(SetFetchSize(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setMaxFieldSize(a: Int): StatementIO[Unit] =
    F.liftFC(SetMaxFieldSize(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setMaxRows(a: Int): StatementIO[Unit] =
    F.liftFC(SetMaxRows(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setPoolable(a: Boolean): StatementIO[Unit] =
    F.liftFC(SetPoolable(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setQueryTimeout(a: Int): StatementIO[Unit] =
    F.liftFC(SetQueryTimeout(a))

  /** 
   * @group Constructors (Primitives)
   */
  def unwrap[T](a: Class[T]): StatementIO[T] =
    F.liftFC(Unwrap(a))

 /** 
  * Natural transformation from `StatementOp` to `Kleisli` for the given `M`, consuming a `java.sql.Statement`. 
  * @group Algebra
  */
  def kleisliTrans[M[_]: Monad: Catchable: Capture]: StatementOp ~> Kleisli[M, Statement, ?] =
    new (StatementOp ~> Kleisli[M, Statement, ?]) {
      def apply[A](op: StatementOp[A]): Kleisli[M, Statement, A] =
        op.defaultTransK[M]
    }

  /**
   * Syntax for `StatementIO`.
   * @group Algebra
   */
  implicit class StatementIOOps[A](ma: StatementIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Statement, A] =
      F.runFC[StatementOp, Kleisli[M, Statement, ?], A](ma)(kleisliTrans[M])
  }

}

