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
  sealed trait StatementOp[A]

  /** 
   * Module of constructors for `StatementOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `statement` module.
   * @group Algebra 
   */
  object StatementOp {
    
    // Lifting
    case class LiftBlobIO[A](s: Blob, action: BlobIO[A]) extends StatementOp[A]
    case class LiftCallableStatementIO[A](s: CallableStatement, action: CallableStatementIO[A]) extends StatementOp[A]
    case class LiftClobIO[A](s: Clob, action: ClobIO[A]) extends StatementOp[A]
    case class LiftConnectionIO[A](s: Connection, action: ConnectionIO[A]) extends StatementOp[A]
    case class LiftDatabaseMetaDataIO[A](s: DatabaseMetaData, action: DatabaseMetaDataIO[A]) extends StatementOp[A]
    case class LiftDriverIO[A](s: Driver, action: DriverIO[A]) extends StatementOp[A]
    case class LiftNClobIO[A](s: NClob, action: NClobIO[A]) extends StatementOp[A]
    case class LiftPreparedStatementIO[A](s: PreparedStatement, action: PreparedStatementIO[A]) extends StatementOp[A]
    case class LiftRefIO[A](s: Ref, action: RefIO[A]) extends StatementOp[A]
    case class LiftResultSetIO[A](s: ResultSet, action: ResultSetIO[A]) extends StatementOp[A]
    case class LiftSQLDataIO[A](s: SQLData, action: SQLDataIO[A]) extends StatementOp[A]
    case class LiftSQLInputIO[A](s: SQLInput, action: SQLInputIO[A]) extends StatementOp[A]
    case class LiftSQLOutputIO[A](s: SQLOutput, action: SQLOutputIO[A]) extends StatementOp[A]

    // Combinators
    case class Attempt[A](action: StatementIO[A]) extends StatementOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends StatementOp[A]

    // Primitive Operations
    case class  AddBatch(a: String) extends StatementOp[Unit]
    case object Cancel extends StatementOp[Unit]
    case object ClearBatch extends StatementOp[Unit]
    case object ClearWarnings extends StatementOp[Unit]
    case object Close extends StatementOp[Unit]
    case class  Execute(a: String, b: Int) extends StatementOp[Boolean]
    case class  Execute1(a: String) extends StatementOp[Boolean]
    case class  Execute2(a: String, b: Array[Int]) extends StatementOp[Boolean]
    case class  Execute3(a: String, b: Array[String]) extends StatementOp[Boolean]
    case object ExecuteBatch extends StatementOp[Array[Int]]
    case class  ExecuteQuery(a: String) extends StatementOp[ResultSet]
    case class  ExecuteUpdate(a: String, b: Int) extends StatementOp[Int]
    case class  ExecuteUpdate1(a: String) extends StatementOp[Int]
    case class  ExecuteUpdate2(a: String, b: Array[String]) extends StatementOp[Int]
    case class  ExecuteUpdate3(a: String, b: Array[Int]) extends StatementOp[Int]
    case object GetConnection extends StatementOp[Connection]
    case object GetFetchDirection extends StatementOp[Int]
    case object GetFetchSize extends StatementOp[Int]
    case object GetGeneratedKeys extends StatementOp[ResultSet]
    case object GetMaxFieldSize extends StatementOp[Int]
    case object GetMaxRows extends StatementOp[Int]
    case class  GetMoreResults(a: Int) extends StatementOp[Boolean]
    case object GetMoreResults1 extends StatementOp[Boolean]
    case object GetQueryTimeout extends StatementOp[Int]
    case object GetResultSet extends StatementOp[ResultSet]
    case object GetResultSetConcurrency extends StatementOp[Int]
    case object GetResultSetHoldability extends StatementOp[Int]
    case object GetResultSetType extends StatementOp[Int]
    case object GetUpdateCount extends StatementOp[Int]
    case object GetWarnings extends StatementOp[SQLWarning]
    case object IsClosed extends StatementOp[Boolean]
    case object IsPoolable extends StatementOp[Boolean]
    case class  IsWrapperFor(a: Class[_]) extends StatementOp[Boolean]
    case class  SetCursorName(a: String) extends StatementOp[Unit]
    case class  SetEscapeProcessing(a: Boolean) extends StatementOp[Unit]
    case class  SetFetchDirection(a: Int) extends StatementOp[Unit]
    case class  SetFetchSize(a: Int) extends StatementOp[Unit]
    case class  SetMaxFieldSize(a: Int) extends StatementOp[Unit]
    case class  SetMaxRows(a: Int) extends StatementOp[Unit]
    case class  SetPoolable(a: Boolean) extends StatementOp[Unit]
    case class  SetQueryTimeout(a: Int) extends StatementOp[Unit]
    case class  Unwrap[T](a: Class[T]) extends StatementOp[T]

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
  def execute(a: String, b: Int): StatementIO[Boolean] =
    F.liftFC(Execute(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String): StatementIO[Boolean] =
    F.liftFC(Execute1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Array[Int]): StatementIO[Boolean] =
    F.liftFC(Execute2(a, b))

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
  def executeUpdate(a: String): StatementIO[Int] =
    F.liftFC(ExecuteUpdate1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Array[String]): StatementIO[Int] =
    F.liftFC(ExecuteUpdate2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Array[Int]): StatementIO[Int] =
    F.liftFC(ExecuteUpdate3(a, b))

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
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: StatementOp ~> ({type l[a] = Kleisli[M, Statement, a]})#l =
   new (StatementOp ~> ({type l[a] = Kleisli[M, Statement, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: Statement => A): Kleisli[M, Statement, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: StatementOp[A]): Kleisli[M, Statement, A] = 
       op match {

        // Lifting
        case LiftBlobIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftCallableStatementIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftClobIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftConnectionIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftDatabaseMetaDataIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftDriverIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftNClobIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftPreparedStatementIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftRefIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftResultSetIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftSQLDataIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftSQLInputIO(s, k) => Kleisli(_ => k.transK[M].run(s))
        case LiftSQLOutputIO(s, k) => Kleisli(_ => k.transK[M].run(s))
  
        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.transK[M].attempt
  
        // Primitive Operations
        case AddBatch(a) => primitive(_.addBatch(a))
        case Cancel => primitive(_.cancel)
        case ClearBatch => primitive(_.clearBatch)
        case ClearWarnings => primitive(_.clearWarnings)
        case Close => primitive(_.close)
        case Execute(a, b) => primitive(_.execute(a, b))
        case Execute1(a) => primitive(_.execute(a))
        case Execute2(a, b) => primitive(_.execute(a, b))
        case Execute3(a, b) => primitive(_.execute(a, b))
        case ExecuteBatch => primitive(_.executeBatch)
        case ExecuteQuery(a) => primitive(_.executeQuery(a))
        case ExecuteUpdate(a, b) => primitive(_.executeUpdate(a, b))
        case ExecuteUpdate1(a) => primitive(_.executeUpdate(a))
        case ExecuteUpdate2(a, b) => primitive(_.executeUpdate(a, b))
        case ExecuteUpdate3(a, b) => primitive(_.executeUpdate(a, b))
        case GetConnection => primitive(_.getConnection)
        case GetFetchDirection => primitive(_.getFetchDirection)
        case GetFetchSize => primitive(_.getFetchSize)
        case GetGeneratedKeys => primitive(_.getGeneratedKeys)
        case GetMaxFieldSize => primitive(_.getMaxFieldSize)
        case GetMaxRows => primitive(_.getMaxRows)
        case GetMoreResults(a) => primitive(_.getMoreResults(a))
        case GetMoreResults1 => primitive(_.getMoreResults)
        case GetQueryTimeout => primitive(_.getQueryTimeout)
        case GetResultSet => primitive(_.getResultSet)
        case GetResultSetConcurrency => primitive(_.getResultSetConcurrency)
        case GetResultSetHoldability => primitive(_.getResultSetHoldability)
        case GetResultSetType => primitive(_.getResultSetType)
        case GetUpdateCount => primitive(_.getUpdateCount)
        case GetWarnings => primitive(_.getWarnings)
        case IsClosed => primitive(_.isClosed)
        case IsPoolable => primitive(_.isPoolable)
        case IsWrapperFor(a) => primitive(_.isWrapperFor(a))
        case SetCursorName(a) => primitive(_.setCursorName(a))
        case SetEscapeProcessing(a) => primitive(_.setEscapeProcessing(a))
        case SetFetchDirection(a) => primitive(_.setFetchDirection(a))
        case SetFetchSize(a) => primitive(_.setFetchSize(a))
        case SetMaxFieldSize(a) => primitive(_.setMaxFieldSize(a))
        case SetMaxRows(a) => primitive(_.setMaxRows(a))
        case SetPoolable(a) => primitive(_.setPoolable(a))
        case SetQueryTimeout(a) => primitive(_.setQueryTimeout(a))
        case Unwrap(a) => primitive(_.unwrap(a))
  
      }
  
    }

  /**
   * Syntax for `StatementIO`.
   * @group Algebra
   */
  implicit class StatementIOOps[A](ma: StatementIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Statement, A] =
      F.runFC[StatementOp,({type l[a]=Kleisli[M,Statement,a]})#l,A](ma)(kleisliTrans[M])
  }

}

