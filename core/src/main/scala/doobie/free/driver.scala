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
import java.sql.DriverPropertyInfo
import java.sql.NClob
import java.sql.PreparedStatement
import java.sql.Ref
import java.sql.ResultSet
import java.sql.SQLData
import java.sql.SQLInput
import java.sql.SQLOutput
import java.sql.Statement
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
 * Algebra and free monad for primitive operations over a `java.sql.Driver`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `DriverIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `DriverOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, Driver, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `liftK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: DriverIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: Driver = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.liftK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object driver {
  
  /** 
   * Sum type of primitive operations over a `java.sql.Driver`.
   * @group Algebra 
   */
  sealed trait DriverOp[A]

  /** 
   * Module of constructors for `DriverOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `driver` module.
   * @group Algebra 
   */
  object DriverOp {
    
    // Lifting
    case class LiftBlobIO[A](s: Blob, action: BlobIO[A]) extends DriverOp[A]
    case class LiftCallableStatementIO[A](s: CallableStatement, action: CallableStatementIO[A]) extends DriverOp[A]
    case class LiftClobIO[A](s: Clob, action: ClobIO[A]) extends DriverOp[A]
    case class LiftConnectionIO[A](s: Connection, action: ConnectionIO[A]) extends DriverOp[A]
    case class LiftDatabaseMetaDataIO[A](s: DatabaseMetaData, action: DatabaseMetaDataIO[A]) extends DriverOp[A]
    case class LiftNClobIO[A](s: NClob, action: NClobIO[A]) extends DriverOp[A]
    case class LiftPreparedStatementIO[A](s: PreparedStatement, action: PreparedStatementIO[A]) extends DriverOp[A]
    case class LiftRefIO[A](s: Ref, action: RefIO[A]) extends DriverOp[A]
    case class LiftResultSetIO[A](s: ResultSet, action: ResultSetIO[A]) extends DriverOp[A]
    case class LiftSQLDataIO[A](s: SQLData, action: SQLDataIO[A]) extends DriverOp[A]
    case class LiftSQLInputIO[A](s: SQLInput, action: SQLInputIO[A]) extends DriverOp[A]
    case class LiftSQLOutputIO[A](s: SQLOutput, action: SQLOutputIO[A]) extends DriverOp[A]
    case class LiftStatementIO[A](s: Statement, action: StatementIO[A]) extends DriverOp[A]

    // Combinators
    case class Attempt[A](action: DriverIO[A]) extends DriverOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends DriverOp[A]

    // Primitive Operations
    case class  AcceptsURL(a: String) extends DriverOp[Boolean]
    case class  Connect(a: String, b: Properties) extends DriverOp[Connection]
    case object GetMajorVersion extends DriverOp[Int]
    case object GetMinorVersion extends DriverOp[Int]
    case class  GetPropertyInfo(a: String, b: Properties) extends DriverOp[Array[DriverPropertyInfo]]
    case object JdbcCompliant extends DriverOp[Boolean]

  }
  import DriverOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[DriverOp]]; abstractly, a computation that consumes 
   * a `java.sql.Driver` and produces a value of type `A`. 
   * @group Algebra 
   */
  type DriverIO[A] = F.FreeC[DriverOp, A]

  /**
   * Monad instance for [[DriverIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadDriverIO: Monad[DriverIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[DriverOp, α]})#λ]

  /**
   * Catchable instance for [[DriverIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableDriverIO: Catchable[DriverIO] =
    new Catchable[DriverIO] {
      def attempt[A](f: DriverIO[A]): DriverIO[Throwable \/ A] = driver.attempt(f)
      def fail[A](err: Throwable): DriverIO[A] = driver.delay(throw err)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftBlob[A](s: Blob, k: BlobIO[A]): DriverIO[A] =
    F.liftFC(LiftBlobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftCallableStatement[A](s: CallableStatement, k: CallableStatementIO[A]): DriverIO[A] =
    F.liftFC(LiftCallableStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftClob[A](s: Clob, k: ClobIO[A]): DriverIO[A] =
    F.liftFC(LiftClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftConnection[A](s: Connection, k: ConnectionIO[A]): DriverIO[A] =
    F.liftFC(LiftConnectionIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDatabaseMetaData[A](s: DatabaseMetaData, k: DatabaseMetaDataIO[A]): DriverIO[A] =
    F.liftFC(LiftDatabaseMetaDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftNClob[A](s: NClob, k: NClobIO[A]): DriverIO[A] =
    F.liftFC(LiftNClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftPreparedStatement[A](s: PreparedStatement, k: PreparedStatementIO[A]): DriverIO[A] =
    F.liftFC(LiftPreparedStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftRef[A](s: Ref, k: RefIO[A]): DriverIO[A] =
    F.liftFC(LiftRefIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftResultSet[A](s: ResultSet, k: ResultSetIO[A]): DriverIO[A] =
    F.liftFC(LiftResultSetIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLData[A](s: SQLData, k: SQLDataIO[A]): DriverIO[A] =
    F.liftFC(LiftSQLDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLInput[A](s: SQLInput, k: SQLInputIO[A]): DriverIO[A] =
    F.liftFC(LiftSQLInputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLOutput[A](s: SQLOutput, k: SQLOutputIO[A]): DriverIO[A] =
    F.liftFC(LiftSQLOutputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftStatement[A](s: Statement, k: StatementIO[A]): DriverIO[A] =
    F.liftFC(LiftStatementIO(s, k))

  /** 
   * Lift a DriverIO[A] into an exception-capturing DriverIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: DriverIO[A]): DriverIO[Throwable \/ A] =
    F.liftFC[DriverOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): DriverIO[A] =
    F.liftFC(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  def acceptsURL(a: String): DriverIO[Boolean] =
    F.liftFC(AcceptsURL(a))

  /** 
   * @group Constructors (Primitives)
   */
  def connect(a: String, b: Properties): DriverIO[Connection] =
    F.liftFC(Connect(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val getMajorVersion: DriverIO[Int] =
    F.liftFC(GetMajorVersion)

  /** 
   * @group Constructors (Primitives)
   */
  val getMinorVersion: DriverIO[Int] =
    F.liftFC(GetMinorVersion)

  /** 
   * @group Constructors (Primitives)
   */
  def getPropertyInfo(a: String, b: Properties): DriverIO[Array[DriverPropertyInfo]] =
    F.liftFC(GetPropertyInfo(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val jdbcCompliant: DriverIO[Boolean] =
    F.liftFC(JdbcCompliant)

 /** 
  * Natural transformation from `DriverOp` to `Kleisli` for the given `M`, consuming a `java.sql.Driver`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: DriverOp ~> ({type l[a] = Kleisli[M, Driver, a]})#l =
   new (DriverOp ~> ({type l[a] = Kleisli[M, Driver, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: Driver => A): Kleisli[M, Driver, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: DriverOp[A]): Kleisli[M, Driver, A] = 
       op match {

        // Lifting
        case LiftBlobIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftCallableStatementIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftClobIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftConnectionIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftDatabaseMetaDataIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
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
        case AcceptsURL(a) => primitive(_.acceptsURL(a))
        case Connect(a, b) => primitive(_.connect(a, b))
        case GetMajorVersion => primitive(_.getMajorVersion)
        case GetMinorVersion => primitive(_.getMinorVersion)
        case GetPropertyInfo(a, b) => primitive(_.getPropertyInfo(a, b))
        case JdbcCompliant => primitive(_.jdbcCompliant)
  
      }
  
    }

  /**
   * Syntax for `DriverIO`.
   * @group Algebra
   */
  implicit class DriverIOOps[A](ma: DriverIO[A]) {
    def liftK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Driver, A] =
      F.runFC[DriverOp,({type l[a]=Kleisli[M,Driver,a]})#l,A](ma)(kleisliTrans[M])
  }

}

