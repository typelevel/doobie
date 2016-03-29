package doobie.free

import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
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
 * `Free#foldMap`.
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
    
    // This algebra has a default interpreter
    implicit val StatementKleisliTrans: KleisliTrans.Aux[StatementOp, Statement] =
      new KleisliTrans[StatementOp] {
        type J = Statement
        def interpK[M[_]: Monad: Catchable: Capture]: StatementOp ~> Kleisli[M, Statement, ?] =
          new (StatementOp ~> Kleisli[M, Statement, ?]) {
            def apply[A](op: StatementOp[A]): Kleisli[M, Statement, A] =
              op.defaultTransK[M]
          }
      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F[Op, A], mod: KleisliTrans.Aux[Op, J]) extends StatementOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
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
    case class  Execute(a: String, b: Array[Int]) extends StatementOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.execute(a, b))
    }
    case class  Execute1(a: String, b: Int) extends StatementOp[Boolean] {
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
    case class  ExecuteUpdate(a: String) extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a))
    }
    case class  ExecuteUpdate1(a: String, b: Array[String]) extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate2(a: String, b: Array[Int]) extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
    }
    case class  ExecuteUpdate3(a: String, b: Int) extends StatementOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.executeUpdate(a, b))
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
  type StatementIO[A] = F[StatementOp, A]

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
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): StatementIO[A] =
    F.liftF(Lift(j, action, mod))

  /** 
   * Lift a StatementIO[A] into an exception-capturing StatementIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: StatementIO[A]): StatementIO[Throwable \/ A] =
    F.liftF[StatementOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): StatementIO[A] =
    F.liftF(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying Statement.
   * @group Constructors (Lifting)
   */
  def raw[A](f: Statement => A): StatementIO[A] =
    F.liftF(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  def addBatch(a: String): StatementIO[Unit] =
    F.liftF(AddBatch(a))

  /** 
   * @group Constructors (Primitives)
   */
  val cancel: StatementIO[Unit] =
    F.liftF(Cancel)

  /** 
   * @group Constructors (Primitives)
   */
  val clearBatch: StatementIO[Unit] =
    F.liftF(ClearBatch)

  /** 
   * @group Constructors (Primitives)
   */
  val clearWarnings: StatementIO[Unit] =
    F.liftF(ClearWarnings)

  /** 
   * @group Constructors (Primitives)
   */
  val close: StatementIO[Unit] =
    F.liftF(Close)

  /** 
   * @group Constructors (Primitives)
   */
  val closeOnCompletion: StatementIO[Unit] =
    F.liftF(CloseOnCompletion)

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Array[Int]): StatementIO[Boolean] =
    F.liftF(Execute(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Int): StatementIO[Boolean] =
    F.liftF(Execute1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String): StatementIO[Boolean] =
    F.liftF(Execute2(a))

  /** 
   * @group Constructors (Primitives)
   */
  def execute(a: String, b: Array[String]): StatementIO[Boolean] =
    F.liftF(Execute3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val executeBatch: StatementIO[Array[Int]] =
    F.liftF(ExecuteBatch)

  /** 
   * @group Constructors (Primitives)
   */
  def executeQuery(a: String): StatementIO[ResultSet] =
    F.liftF(ExecuteQuery(a))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String): StatementIO[Int] =
    F.liftF(ExecuteUpdate(a))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Array[String]): StatementIO[Int] =
    F.liftF(ExecuteUpdate1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Array[Int]): StatementIO[Int] =
    F.liftF(ExecuteUpdate2(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def executeUpdate(a: String, b: Int): StatementIO[Int] =
    F.liftF(ExecuteUpdate3(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val getConnection: StatementIO[Connection] =
    F.liftF(GetConnection)

  /** 
   * @group Constructors (Primitives)
   */
  val getFetchDirection: StatementIO[Int] =
    F.liftF(GetFetchDirection)

  /** 
   * @group Constructors (Primitives)
   */
  val getFetchSize: StatementIO[Int] =
    F.liftF(GetFetchSize)

  /** 
   * @group Constructors (Primitives)
   */
  val getGeneratedKeys: StatementIO[ResultSet] =
    F.liftF(GetGeneratedKeys)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxFieldSize: StatementIO[Int] =
    F.liftF(GetMaxFieldSize)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxRows: StatementIO[Int] =
    F.liftF(GetMaxRows)

  /** 
   * @group Constructors (Primitives)
   */
  def getMoreResults(a: Int): StatementIO[Boolean] =
    F.liftF(GetMoreResults(a))

  /** 
   * @group Constructors (Primitives)
   */
  val getMoreResults: StatementIO[Boolean] =
    F.liftF(GetMoreResults1)

  /** 
   * @group Constructors (Primitives)
   */
  val getQueryTimeout: StatementIO[Int] =
    F.liftF(GetQueryTimeout)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSet: StatementIO[ResultSet] =
    F.liftF(GetResultSet)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSetConcurrency: StatementIO[Int] =
    F.liftF(GetResultSetConcurrency)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSetHoldability: StatementIO[Int] =
    F.liftF(GetResultSetHoldability)

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSetType: StatementIO[Int] =
    F.liftF(GetResultSetType)

  /** 
   * @group Constructors (Primitives)
   */
  val getUpdateCount: StatementIO[Int] =
    F.liftF(GetUpdateCount)

  /** 
   * @group Constructors (Primitives)
   */
  val getWarnings: StatementIO[SQLWarning] =
    F.liftF(GetWarnings)

  /** 
   * @group Constructors (Primitives)
   */
  val isCloseOnCompletion: StatementIO[Boolean] =
    F.liftF(IsCloseOnCompletion)

  /** 
   * @group Constructors (Primitives)
   */
  val isClosed: StatementIO[Boolean] =
    F.liftF(IsClosed)

  /** 
   * @group Constructors (Primitives)
   */
  val isPoolable: StatementIO[Boolean] =
    F.liftF(IsPoolable)

  /** 
   * @group Constructors (Primitives)
   */
  def isWrapperFor(a: Class[_]): StatementIO[Boolean] =
    F.liftF(IsWrapperFor(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setCursorName(a: String): StatementIO[Unit] =
    F.liftF(SetCursorName(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setEscapeProcessing(a: Boolean): StatementIO[Unit] =
    F.liftF(SetEscapeProcessing(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setFetchDirection(a: Int): StatementIO[Unit] =
    F.liftF(SetFetchDirection(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setFetchSize(a: Int): StatementIO[Unit] =
    F.liftF(SetFetchSize(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setMaxFieldSize(a: Int): StatementIO[Unit] =
    F.liftF(SetMaxFieldSize(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setMaxRows(a: Int): StatementIO[Unit] =
    F.liftF(SetMaxRows(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setPoolable(a: Boolean): StatementIO[Unit] =
    F.liftF(SetPoolable(a))

  /** 
   * @group Constructors (Primitives)
   */
  def setQueryTimeout(a: Int): StatementIO[Unit] =
    F.liftF(SetQueryTimeout(a))

  /** 
   * @group Constructors (Primitives)
   */
  def unwrap[T](a: Class[T]): StatementIO[T] =
    F.liftF(Unwrap(a))

 /** 
  * Natural transformation from `StatementOp` to `Kleisli` for the given `M`, consuming a `java.sql.Statement`. 
  * @group Algebra
  */
  def interpK[M[_]: Monad: Catchable: Capture]: StatementOp ~> Kleisli[M, Statement, ?] =
   StatementOp.StatementKleisliTrans.interpK

 /** 
  * Natural transformation from `StatementIO` to `Kleisli` for the given `M`, consuming a `java.sql.Statement`. 
  * @group Algebra
  */
  def transK[M[_]: Monad: Catchable: Capture]: StatementIO ~> Kleisli[M, Statement, ?] =
   StatementOp.StatementKleisliTrans.transK

 /** 
  * Natural transformation from `StatementIO` to `M`, given a `java.sql.Statement`. 
  * @group Algebra
  */
 def trans[M[_]: Monad: Catchable: Capture](c: Statement): StatementIO ~> M =
   StatementOp.StatementKleisliTrans.trans[M](c)

  /**
   * Syntax for `StatementIO`.
   * @group Algebra
   */
  implicit class StatementIOOps[A](ma: StatementIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, Statement, A] =
      StatementOp.StatementKleisliTrans.transK[M].apply(ma)
  }

}

