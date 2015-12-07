package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._
import doobie.free.kleislitrans._

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
import java.util.concurrent.Executor

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
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: ConnectionIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: Connection = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object connection {
  
  /** 
   * Sum type of primitive operations over a `java.sql.Connection`.
   * @group Algebra 
   */
  sealed trait ConnectionOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: Connection => A): Kleisli[M, Connection, A] = 
      Kleisli((s: Connection) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Connection, A]
  }

  /** 
   * Module of constructors for `ConnectionOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `connection` module.
   * @group Algebra 
   */
  object ConnectionOp {
    
    // This algebra has a default interpreter
    implicit val ConnectionKleisliTrans: KleisliTrans.Aux[ConnectionOp, Connection] =
      new KleisliTrans[ConnectionOp] {
        type J = Connection
        def interpK[M[_]: Monad: Catchable: Capture]: ConnectionOp ~> Kleisli[M, Connection, ?] =
          new (ConnectionOp ~> Kleisli[M, Connection, ?]) {
            def apply[A](op: ConnectionOp[A]): Kleisli[M, Connection, A] =
              op.defaultTransK[M]
          }
      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F.FreeC[Op, A], mod: KleisliTrans.Aux[Op, J]) extends ConnectionOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
    }

    // Combinators
    case class Attempt[A](action: ConnectionIO[A]) extends ConnectionOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, Connection, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends ConnectionOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: Connection => A) extends ConnectionOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case class  Abort(a: Executor) extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.abort(a))
    }
    case object ClearWarnings extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.clearWarnings())
    }
    case object Close extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.close())
    }
    case object Commit extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.commit())
    }
    case class  CreateArrayOf(a: String, b: Array[Object]) extends ConnectionOp[SqlArray] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.createArrayOf(a, b))
    }
    case object CreateBlob extends ConnectionOp[Blob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.createBlob())
    }
    case object CreateClob extends ConnectionOp[Clob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.createClob())
    }
    case object CreateNClob extends ConnectionOp[NClob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.createNClob())
    }
    case object CreateSQLXML extends ConnectionOp[SQLXML] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.createSQLXML())
    }
    case object CreateStatement extends ConnectionOp[Statement] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.createStatement())
    }
    case class  CreateStatement1(a: Int, b: Int) extends ConnectionOp[Statement] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.createStatement(a, b))
    }
    case class  CreateStatement2(a: Int, b: Int, c: Int) extends ConnectionOp[Statement] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.createStatement(a, b, c))
    }
    case class  CreateStruct(a: String, b: Array[Object]) extends ConnectionOp[Struct] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.createStruct(a, b))
    }
    case object GetAutoCommit extends ConnectionOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getAutoCommit())
    }
    case object GetCatalog extends ConnectionOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCatalog())
    }
    case object GetClientInfo extends ConnectionOp[Properties] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getClientInfo())
    }
    case class  GetClientInfo1(a: String) extends ConnectionOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getClientInfo(a))
    }
    case object GetHoldability extends ConnectionOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getHoldability())
    }
    case object GetMetaData extends ConnectionOp[DatabaseMetaData] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMetaData())
    }
    case object GetNetworkTimeout extends ConnectionOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getNetworkTimeout())
    }
    case object GetSchema extends ConnectionOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSchema())
    }
    case object GetTransactionIsolation extends ConnectionOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTransactionIsolation())
    }
    case object GetTypeMap extends ConnectionOp[Map[String, Class[_]]] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTypeMap())
    }
    case object GetWarnings extends ConnectionOp[SQLWarning] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getWarnings())
    }
    case object IsClosed extends ConnectionOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isClosed())
    }
    case object IsReadOnly extends ConnectionOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isReadOnly())
    }
    case class  IsValid(a: Int) extends ConnectionOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isValid(a))
    }
    case class  IsWrapperFor(a: Class[_]) extends ConnectionOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isWrapperFor(a))
    }
    case class  NativeSQL(a: String) extends ConnectionOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.nativeSQL(a))
    }
    case class  PrepareCall(a: String, b: Int, c: Int, d: Int) extends ConnectionOp[CallableStatement] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.prepareCall(a, b, c, d))
    }
    case class  PrepareCall1(a: String, b: Int, c: Int) extends ConnectionOp[CallableStatement] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.prepareCall(a, b, c))
    }
    case class  PrepareCall2(a: String) extends ConnectionOp[CallableStatement] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.prepareCall(a))
    }
    case class  PrepareStatement(a: String, b: Int, c: Int) extends ConnectionOp[PreparedStatement] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.prepareStatement(a, b, c))
    }
    case class  PrepareStatement1(a: String, b: Int, c: Int, d: Int) extends ConnectionOp[PreparedStatement] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.prepareStatement(a, b, c, d))
    }
    case class  PrepareStatement2(a: String) extends ConnectionOp[PreparedStatement] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.prepareStatement(a))
    }
    case class  PrepareStatement3(a: String, b: Array[String]) extends ConnectionOp[PreparedStatement] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.prepareStatement(a, b))
    }
    case class  PrepareStatement4(a: String, b: Int) extends ConnectionOp[PreparedStatement] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.prepareStatement(a, b))
    }
    case class  PrepareStatement5(a: String, b: Array[Int]) extends ConnectionOp[PreparedStatement] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.prepareStatement(a, b))
    }
    case class  ReleaseSavepoint(a: Savepoint) extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.releaseSavepoint(a))
    }
    case object Rollback extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.rollback())
    }
    case class  Rollback1(a: Savepoint) extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.rollback(a))
    }
    case class  SetAutoCommit(a: Boolean) extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setAutoCommit(a))
    }
    case class  SetCatalog(a: String) extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setCatalog(a))
    }
    case class  SetClientInfo(a: String, b: String) extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setClientInfo(a, b))
    }
    case class  SetClientInfo1(a: Properties) extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setClientInfo(a))
    }
    case class  SetHoldability(a: Int) extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setHoldability(a))
    }
    case class  SetNetworkTimeout(a: Executor, b: Int) extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setNetworkTimeout(a, b))
    }
    case class  SetReadOnly(a: Boolean) extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setReadOnly(a))
    }
    case object SetSavepoint extends ConnectionOp[Savepoint] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setSavepoint())
    }
    case class  SetSavepoint1(a: String) extends ConnectionOp[Savepoint] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setSavepoint(a))
    }
    case class  SetSchema(a: String) extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setSchema(a))
    }
    case class  SetTransactionIsolation(a: Int) extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTransactionIsolation(a))
    }
    case class  SetTypeMap(a: Map[String, Class[_]]) extends ConnectionOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.setTypeMap(a))
    }
    case class  Unwrap[T](a: Class[T]) extends ConnectionOp[T] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.unwrap(a))
    }

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
   * Capture instance for [[ConnectionIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureConnectionIO: Capture[ConnectionIO] =
    new Capture[ConnectionIO] {
      def apply[A](a: => A): ConnectionIO[A] = connection.delay(a)
    }

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F.FreeC[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): ConnectionIO[A] =
    F.liftFC(Lift(j, action, mod))

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
   * Backdoor for arbitrary computations on the underlying Connection.
   * @group Constructors (Lifting)
   */
  def raw[A](f: Connection => A): ConnectionIO[A] =
    F.liftFC(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  def abort(a: Executor): ConnectionIO[Unit] =
    F.liftFC(Abort(a))

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
  val getClientInfo: ConnectionIO[Properties] =
    F.liftFC(GetClientInfo)

  /** 
   * @group Constructors (Primitives)
   */
  def getClientInfo(a: String): ConnectionIO[String] =
    F.liftFC(GetClientInfo1(a))

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
  val getNetworkTimeout: ConnectionIO[Int] =
    F.liftFC(GetNetworkTimeout)

  /** 
   * @group Constructors (Primitives)
   */
  val getSchema: ConnectionIO[String] =
    F.liftFC(GetSchema)

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
  def prepareCall(a: String, b: Int, c: Int, d: Int): ConnectionIO[CallableStatement] =
    F.liftFC(PrepareCall(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareCall(a: String, b: Int, c: Int): ConnectionIO[CallableStatement] =
    F.liftFC(PrepareCall1(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareCall(a: String): ConnectionIO[CallableStatement] =
    F.liftFC(PrepareCall2(a))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareStatement(a: String, b: Int, c: Int): ConnectionIO[PreparedStatement] =
    F.liftFC(PrepareStatement(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareStatement(a: String, b: Int, c: Int, d: Int): ConnectionIO[PreparedStatement] =
    F.liftFC(PrepareStatement1(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareStatement(a: String): ConnectionIO[PreparedStatement] =
    F.liftFC(PrepareStatement2(a))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareStatement(a: String, b: Array[String]): ConnectionIO[PreparedStatement] =
    F.liftFC(PrepareStatement3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareStatement(a: String, b: Int): ConnectionIO[PreparedStatement] =
    F.liftFC(PrepareStatement4(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def prepareStatement(a: String, b: Array[Int]): ConnectionIO[PreparedStatement] =
    F.liftFC(PrepareStatement5(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def releaseSavepoint(a: Savepoint): ConnectionIO[Unit] =
    F.liftFC(ReleaseSavepoint(a))

  /** 
   * @group Constructors (Primitives)
   */
  val rollback: ConnectionIO[Unit] =
    F.liftFC(Rollback)

  /** 
   * @group Constructors (Primitives)
   */
  def rollback(a: Savepoint): ConnectionIO[Unit] =
    F.liftFC(Rollback1(a))

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
  def setNetworkTimeout(a: Executor, b: Int): ConnectionIO[Unit] =
    F.liftFC(SetNetworkTimeout(a, b))

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
  def setSchema(a: String): ConnectionIO[Unit] =
    F.liftFC(SetSchema(a))

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
  def interpK[M[_]: Monad: Catchable: Capture]: ConnectionOp ~> Kleisli[M, Connection, ?] =
   ConnectionOp.ConnectionKleisliTrans.interpK

 /** 
  * Natural transformation from `ConnectionIO` to `Kleisli` for the given `M`, consuming a `java.sql.Connection`. 
  * @group Algebra
  */
  def transK[M[_]: Monad: Catchable: Capture]: ConnectionIO ~> Kleisli[M, Connection, ?] =
   ConnectionOp.ConnectionKleisliTrans.transK

 /** 
  * Natural transformation from `ConnectionIO` to `M`, given a `java.sql.Connection`. 
  * @group Algebra
  */
 def trans[M[_]: Monad: Catchable: Capture](c: Connection): ConnectionIO ~> M =
   ConnectionOp.ConnectionKleisliTrans.trans[M](c)

  /**
   * Syntax for `ConnectionIO`.
   * @group Algebra
   */
  implicit class ConnectionIOOps[A](ma: ConnectionIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Connection, A] =
      ConnectionOp.ConnectionKleisliTrans.transK[M].apply(ma)
  }

}

