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
import java.sql.SQLXML
import java.sql.Savepoint
import java.sql.Statement
import java.sql.Struct
import java.sql.{ Array => SqlArray }
import java.util.Map
import java.util.Properties

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
 * Algebra and free monad for primitive operations over a `java.sql.Connection`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `ConnectionIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `ConnectionOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, Connection, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `liftK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: ConnectionIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: Connection = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.liftK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object connection {
  
  /** 
   * Sum type of primitive operations over a `java.sql.Connection`.
   * @group Algebra 
   */
  sealed trait ConnectionOp[A]

  /** 
   * Module of constructors for `ConnectionOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `connection` module.
   * @group Algebra 
   */
  object ConnectionOp {
    
    // Lifting
    case class LiftBlobIO[A](s: Blob, action: BlobIO[A]) extends ConnectionOp[A]
    case class LiftCallableStatementIO[A](s: CallableStatement, action: CallableStatementIO[A]) extends ConnectionOp[A]
    case class LiftClobIO[A](s: Clob, action: ClobIO[A]) extends ConnectionOp[A]
    case class LiftDatabaseMetaDataIO[A](s: DatabaseMetaData, action: DatabaseMetaDataIO[A]) extends ConnectionOp[A]
    case class LiftDriverIO[A](s: Driver, action: DriverIO[A]) extends ConnectionOp[A]
    case class LiftNClobIO[A](s: NClob, action: NClobIO[A]) extends ConnectionOp[A]
    case class LiftPreparedStatementIO[A](s: PreparedStatement, action: PreparedStatementIO[A]) extends ConnectionOp[A]
    case class LiftRefIO[A](s: Ref, action: RefIO[A]) extends ConnectionOp[A]
    case class LiftResultSetIO[A](s: ResultSet, action: ResultSetIO[A]) extends ConnectionOp[A]
    case class LiftSQLDataIO[A](s: SQLData, action: SQLDataIO[A]) extends ConnectionOp[A]
    case class LiftSQLInputIO[A](s: SQLInput, action: SQLInputIO[A]) extends ConnectionOp[A]
    case class LiftSQLOutputIO[A](s: SQLOutput, action: SQLOutputIO[A]) extends ConnectionOp[A]
    case class LiftStatementIO[A](s: Statement, action: StatementIO[A]) extends ConnectionOp[A]

    // Combinators
    case class Attempt[A](action: ConnectionIO[A]) extends ConnectionOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends ConnectionOp[A]

    // Primitive Operations
    case object ClearWarnings extends ConnectionOp[Unit]
    case object Close extends ConnectionOp[Unit]
    case object Commit extends ConnectionOp[Unit]
    case class  CreateArrayOf(a: String, b: Array[Object]) extends ConnectionOp[SqlArray]
    case object CreateBlob extends ConnectionOp[Blob]
    case object CreateClob extends ConnectionOp[Clob]
    case object CreateNClob extends ConnectionOp[NClob]
    case object CreateSQLXML extends ConnectionOp[SQLXML]
    case object CreateStatement extends ConnectionOp[Statement]
    case class  CreateStatement1(a: Int, b: Int) extends ConnectionOp[Statement]
    case class  CreateStatement2(a: Int, b: Int, c: Int) extends ConnectionOp[Statement]
    case class  CreateStruct(a: String, b: Array[Object]) extends ConnectionOp[Struct]
    case object GetAutoCommit extends ConnectionOp[Boolean]
    case object GetCatalog extends ConnectionOp[String]
    case class  GetClientInfo(a: String) extends ConnectionOp[String]
    case object GetClientInfo1 extends ConnectionOp[Properties]
    case object GetHoldability extends ConnectionOp[Int]
    case object GetMetaData extends ConnectionOp[DatabaseMetaData]
    case object GetTransactionIsolation extends ConnectionOp[Int]
    case object GetTypeMap extends ConnectionOp[Map[String, Class[_]]]
    case object GetWarnings extends ConnectionOp[SQLWarning]
    case object IsClosed extends ConnectionOp[Boolean]
    case object IsReadOnly extends ConnectionOp[Boolean]
    case class  IsValid(a: Int) extends ConnectionOp[Boolean]
    case class  IsWrapperFor(a: Class[_]) extends ConnectionOp[Boolean]
    case class  NativeSQL(a: String) extends ConnectionOp[String]
    case class  PrepareCall(a: String, b: Int, c: Int) extends ConnectionOp[CallableStatement]
    case class  PrepareCall1(a: String) extends ConnectionOp[CallableStatement]
    case class  PrepareCall2(a: String, b: Int, c: Int, d: Int) extends ConnectionOp[CallableStatement]
    case class  PrepareStatement(a: String, b: Int, c: Int) extends ConnectionOp[PreparedStatement]
    case class  PrepareStatement1(a: String) extends ConnectionOp[PreparedStatement]
    case class  PrepareStatement2(a: String, b: Int, c: Int, d: Int) extends ConnectionOp[PreparedStatement]
    case class  PrepareStatement3(a: String, b: Int) extends ConnectionOp[PreparedStatement]
    case class  PrepareStatement4(a: String, b: Array[Int]) extends ConnectionOp[PreparedStatement]
    case class  PrepareStatement5(a: String, b: Array[String]) extends ConnectionOp[PreparedStatement]
    case class  ReleaseSavepoint(a: Savepoint) extends ConnectionOp[Unit]
    case class  Rollback(a: Savepoint) extends ConnectionOp[Unit]
    case object Rollback1 extends ConnectionOp[Unit]
    case class  SetAutoCommit(a: Boolean) extends ConnectionOp[Unit]
    case class  SetCatalog(a: String) extends ConnectionOp[Unit]
    case class  SetClientInfo(a: String, b: String) extends ConnectionOp[Unit]
    case class  SetClientInfo1(a: Properties) extends ConnectionOp[Unit]
    case class  SetHoldability(a: Int) extends ConnectionOp[Unit]
    case class  SetReadOnly(a: Boolean) extends ConnectionOp[Unit]
    case object SetSavepoint extends ConnectionOp[Savepoint]
    case class  SetSavepoint1(a: String) extends ConnectionOp[Savepoint]
    case class  SetTransactionIsolation(a: Int) extends ConnectionOp[Unit]
    case class  SetTypeMap(a: Map[String, Class[_]]) extends ConnectionOp[Unit]
    case class  Unwrap[T](a: Class[T]) extends ConnectionOp[T]

  }
  import ConnectionOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[ConnectionOp]]; abstractly, a computation that consumes 
   * a `java.sql.Connection` and produces a value of type `A`. 
   * @group Algebra 
   */
  type ConnectionIO[A] = F.FreeC[ConnectionOp, A]

  /**
   * Monad instance for [[ConnectionIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadConnectionIO: Monad[ConnectionIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[ConnectionOp, α]})#λ]

  /**
   * Catchable instance for [[ConnectionIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableConnectionIO: Catchable[ConnectionIO] =
    new Catchable[ConnectionIO] {
      def attempt[A](f: ConnectionIO[A]): ConnectionIO[Throwable \/ A] = connection.attempt(f)
      def fail[A](err: Throwable): ConnectionIO[A] = connection.delay(throw err)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftBlob[A](s: Blob, k: BlobIO[A]): ConnectionIO[A] =
    F.liftFC(LiftBlobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftCallableStatement[A](s: CallableStatement, k: CallableStatementIO[A]): ConnectionIO[A] =
    F.liftFC(LiftCallableStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftClob[A](s: Clob, k: ClobIO[A]): ConnectionIO[A] =
    F.liftFC(LiftClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDatabaseMetaData[A](s: DatabaseMetaData, k: DatabaseMetaDataIO[A]): ConnectionIO[A] =
    F.liftFC(LiftDatabaseMetaDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDriver[A](s: Driver, k: DriverIO[A]): ConnectionIO[A] =
    F.liftFC(LiftDriverIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftNClob[A](s: NClob, k: NClobIO[A]): ConnectionIO[A] =
    F.liftFC(LiftNClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftPreparedStatement[A](s: PreparedStatement, k: PreparedStatementIO[A]): ConnectionIO[A] =
    F.liftFC(LiftPreparedStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftRef[A](s: Ref, k: RefIO[A]): ConnectionIO[A] =
    F.liftFC(LiftRefIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftResultSet[A](s: ResultSet, k: ResultSetIO[A]): ConnectionIO[A] =
    F.liftFC(LiftResultSetIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLData[A](s: SQLData, k: SQLDataIO[A]): ConnectionIO[A] =
    F.liftFC(LiftSQLDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLInput[A](s: SQLInput, k: SQLInputIO[A]): ConnectionIO[A] =
    F.liftFC(LiftSQLInputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLOutput[A](s: SQLOutput, k: SQLOutputIO[A]): ConnectionIO[A] =
    F.liftFC(LiftSQLOutputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftStatement[A](s: Statement, k: StatementIO[A]): ConnectionIO[A] =
    F.liftFC(LiftStatementIO(s, k))

  /** 
   * Lift a ConnectionIO[A] into an exception-capturing ConnectionIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: ConnectionIO[A]): ConnectionIO[Throwable \/ A] =
    F.liftFC[ConnectionOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): ConnectionIO[A] =
    F.liftFC(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  val clearWarnings: ConnectionIO[Unit] =
    F.liftFC(ClearWarnings)

  /** 
   * @group Constructors (Primitives)
   */
  val close: ConnectionIO[Unit] =
    F.liftFC(Close)

  /** 
   * @group Constructors (Primitives)
   */
  val commit: ConnectionIO[Unit] =
    F.liftFC(Commit)

  /** 
   * @group Constructors (Primitives)
   */
  def createArrayOf(a: String, b: Array[Object]): ConnectionIO[SqlArray] =
    F.liftFC(CreateArrayOf(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val createBlob: ConnectionIO[Blob] =
    F.liftFC(CreateBlob)

  /** 
   * @group Constructors (Primitives)
   */
  val createClob: ConnectionIO[Clob] =
    F.liftFC(CreateClob)

  /** 
   * @group Constructors (Primitives)
   */
  val createNClob: ConnectionIO[NClob] =
    F.liftFC(CreateNClob)

  /** 
   * @group Constructors (Primitives)
   */
  val createSQLXML: ConnectionIO[SQLXML] =
    F.liftFC(CreateSQLXML)

  /** 
   * @group Constructors (Primitives)
   */
  val createStatement: ConnectionIO[Statement] =
    F.liftFC(CreateStatement)

  /** 
   * @group Constructors (Primitives)
   */
  def createStatement(a: Int, b: Int): ConnectionIO[Statement] =
    F.liftFC(CreateStatement1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def createStatement(a: Int, b: Int, c: Int): ConnectionIO[Statement] =
    F.liftFC(CreateStatement2(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def createStruct(a: String, b: Array[Object]): ConnectionIO[Struct] =
    F.liftFC(CreateStruct(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val getAutoCommit: ConnectionIO[Boolean] =
    F.liftFC(GetAutoCommit)

  /** 
   * @group Constructors (Primitives)
   */
  val getCatalog: ConnectionIO[String] =
    F.liftFC(GetCatalog)

  /** 
   * @group Constructors (Primitives)
   */
  def getClientInfo(a: String): ConnectionIO[String] =
    F.liftFC(GetClientInfo(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getClientInfo: ConnectionIO[Properties] =
    F.liftFC(GetClientInfo1)

  /** 
   * @group Constructors (Primitives)
   */
  val getHoldability: ConnectionIO[Int] =
    F.liftFC(GetHoldability)

  /** 
   * @group Constructors (Primitives)
   */
  val getMetaData: ConnectionIO[DatabaseMetaData] =
    F.liftFC(GetMetaData)

  /** 
   * @group Constructors (Primitives)
   */
  val getTransactionIsolation: ConnectionIO[Int] =
    F.liftFC(GetTransactionIsolation)

  /** 
   * @group Constructors (Primitives)
   */
  val getTypeMap: ConnectionIO[Map[String, Class[_]]] =
    F.liftFC(GetTypeMap)

  /** 
   * @group Constructors (Primitives)
   */
  val getWarnings: ConnectionIO[SQLWarning] =
    F.liftFC(GetWarnings)

  /** 
   * @group Constructors (Primitives)
   */
  val isClosed: ConnectionIO[Boolean] =
    F.liftFC(IsClosed)

  /** 
   * @group Constructors (Primitives)
   */
  val isReadOnly: ConnectionIO[Boolean] =
    F.liftFC(IsReadOnly)

  /** 
   * @group Constructors (Primitives)
   */
  def isValid(a: Int): ConnectionIO[Boolean] =
    F.liftFC(IsValid(a))

  /** 
   * @group Constructors (Primitives)
   */
  def isWrapperFor(a: Class[_]): ConnectionIO[Boolean] =
    F.liftFC(IsWrapperFor(a))

  /** 
   * @group Constructors (Primitives)
   */
  def nativeSQL(a: String): ConnectionIO[String] =
    F.liftFC(NativeSQL(a))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareCall(a: String, b: Int, c: Int): ConnectionIO[CallableStatement] =
    F.liftFC(PrepareCall(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareCall(a: String): ConnectionIO[CallableStatement] =
    F.liftFC(PrepareCall1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareCall(a: String, b: Int, c: Int, d: Int): ConnectionIO[CallableStatement] =
    F.liftFC(PrepareCall2(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareStatement(a: String, b: Int, c: Int): ConnectionIO[PreparedStatement] =
    F.liftFC(PrepareStatement(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareStatement(a: String): ConnectionIO[PreparedStatement] =
    F.liftFC(PrepareStatement1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareStatement(a: String, b: Int, c: Int, d: Int): ConnectionIO[PreparedStatement] =
    F.liftFC(PrepareStatement2(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareStatement(a: String, b: Int): ConnectionIO[PreparedStatement] =
    F.liftFC(PrepareStatement3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareStatement(a: String, b: Array[Int]): ConnectionIO[PreparedStatement] =
    F.liftFC(PrepareStatement4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareStatement(a: String, b: Array[String]): ConnectionIO[PreparedStatement] =
    F.liftFC(PrepareStatement5(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def releaseSavepoint(a: Savepoint): ConnectionIO[Unit] =
    F.liftFC(ReleaseSavepoint(a))

  /** 
   * @group Constructors (Primitives)
   */
  def rollback(a: Savepoint): ConnectionIO[Unit] =
    F.liftFC(Rollback(a))

  /** 
   * @group Constructors (Primitives)
   */
  val rollback: ConnectionIO[Unit] =
    F.liftFC(Rollback1)

  /** 
   * @group Constructors (Primitives)
   */
  def setAutoCommit(a: Boolean): ConnectionIO[Unit] =
    F.liftFC(SetAutoCommit(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setCatalog(a: String): ConnectionIO[Unit] =
    F.liftFC(SetCatalog(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setClientInfo(a: String, b: String): ConnectionIO[Unit] =
    F.liftFC(SetClientInfo(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def setClientInfo(a: Properties): ConnectionIO[Unit] =
    F.liftFC(SetClientInfo1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setHoldability(a: Int): ConnectionIO[Unit] =
    F.liftFC(SetHoldability(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setReadOnly(a: Boolean): ConnectionIO[Unit] =
    F.liftFC(SetReadOnly(a))

  /** 
   * @group Constructors (Primitives)
   */
  val setSavepoint: ConnectionIO[Savepoint] =
    F.liftFC(SetSavepoint)

  /** 
   * @group Constructors (Primitives)
   */
  def setSavepoint(a: String): ConnectionIO[Savepoint] =
    F.liftFC(SetSavepoint1(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setTransactionIsolation(a: Int): ConnectionIO[Unit] =
    F.liftFC(SetTransactionIsolation(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setTypeMap(a: Map[String, Class[_]]): ConnectionIO[Unit] =
    F.liftFC(SetTypeMap(a))

  /** 
   * @group Constructors (Primitives)
   */
  def unwrap[T](a: Class[T]): ConnectionIO[T] =
    F.liftFC(Unwrap(a))

 /** 
  * Natural transformation from `ConnectionOp` to `Kleisli` for the given `M`, consuming a `java.sql.Connection`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: ConnectionOp ~> ({type l[a] = Kleisli[M, Connection, a]})#l =
   new (ConnectionOp ~> ({type l[a] = Kleisli[M, Connection, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: Connection => A): Kleisli[M, Connection, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: ConnectionOp[A]): Kleisli[M, Connection, A] = 
       op match {

        // Lifting
        case LiftBlobIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftCallableStatementIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftClobIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftDatabaseMetaDataIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftDriverIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftNClobIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftPreparedStatementIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftRefIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftResultSetIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftSQLDataIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftSQLInputIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftSQLOutputIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftStatementIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
  
        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.liftK[M].attempt
  
        // Primitive Operations
        case ClearWarnings => primitive(_.clearWarnings)
        case Close => primitive(_.close)
        case Commit => primitive(_.commit)
        case CreateArrayOf(a, b) => primitive(_.createArrayOf(a, b))
        case CreateBlob => primitive(_.createBlob)
        case CreateClob => primitive(_.createClob)
        case CreateNClob => primitive(_.createNClob)
        case CreateSQLXML => primitive(_.createSQLXML)
        case CreateStatement => primitive(_.createStatement)
        case CreateStatement1(a, b) => primitive(_.createStatement(a, b))
        case CreateStatement2(a, b, c) => primitive(_.createStatement(a, b, c))
        case CreateStruct(a, b) => primitive(_.createStruct(a, b))
        case GetAutoCommit => primitive(_.getAutoCommit)
        case GetCatalog => primitive(_.getCatalog)
        case GetClientInfo(a) => primitive(_.getClientInfo(a))
        case GetClientInfo1 => primitive(_.getClientInfo)
        case GetHoldability => primitive(_.getHoldability)
        case GetMetaData => primitive(_.getMetaData)
        case GetTransactionIsolation => primitive(_.getTransactionIsolation)
        case GetTypeMap => primitive(_.getTypeMap)
        case GetWarnings => primitive(_.getWarnings)
        case IsClosed => primitive(_.isClosed)
        case IsReadOnly => primitive(_.isReadOnly)
        case IsValid(a) => primitive(_.isValid(a))
        case IsWrapperFor(a) => primitive(_.isWrapperFor(a))
        case NativeSQL(a) => primitive(_.nativeSQL(a))
        case PrepareCall(a, b, c) => primitive(_.prepareCall(a, b, c))
        case PrepareCall1(a) => primitive(_.prepareCall(a))
        case PrepareCall2(a, b, c, d) => primitive(_.prepareCall(a, b, c, d))
        case PrepareStatement(a, b, c) => primitive(_.prepareStatement(a, b, c))
        case PrepareStatement1(a) => primitive(_.prepareStatement(a))
        case PrepareStatement2(a, b, c, d) => primitive(_.prepareStatement(a, b, c, d))
        case PrepareStatement3(a, b) => primitive(_.prepareStatement(a, b))
        case PrepareStatement4(a, b) => primitive(_.prepareStatement(a, b))
        case PrepareStatement5(a, b) => primitive(_.prepareStatement(a, b))
        case ReleaseSavepoint(a) => primitive(_.releaseSavepoint(a))
        case Rollback(a) => primitive(_.rollback(a))
        case Rollback1 => primitive(_.rollback)
        case SetAutoCommit(a) => primitive(_.setAutoCommit(a))
        case SetCatalog(a) => primitive(_.setCatalog(a))
        case SetClientInfo(a, b) => primitive(_.setClientInfo(a, b))
        case SetClientInfo1(a) => primitive(_.setClientInfo(a))
        case SetHoldability(a) => primitive(_.setHoldability(a))
        case SetReadOnly(a) => primitive(_.setReadOnly(a))
        case SetSavepoint => primitive(_.setSavepoint)
        case SetSavepoint1(a) => primitive(_.setSavepoint(a))
        case SetTransactionIsolation(a) => primitive(_.setTransactionIsolation(a))
        case SetTypeMap(a) => primitive(_.setTypeMap(a))
        case Unwrap(a) => primitive(_.unwrap(a))
  
      }
  
    }

  /**
   * Syntax for `ConnectionIO`.
   * @group Algebra
   */
  implicit class ConnectionIOOps[A](ma: ConnectionIO[A]) {
    def liftK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Connection, A] =
      F.runFC[ConnectionOp,({type l[a]=Kleisli[M,Connection,a]})#l,A](ma)(kleisliTrans[M])
  }

}

