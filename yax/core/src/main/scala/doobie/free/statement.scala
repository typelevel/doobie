package doobie.free

#+scalaz
import doobie.util.capture.Capture
import scalaz.{ Catchable, Free => FF, Monad, ~>, \/ }
#-scalaz
#+cats
import cats.{ Monad, ~> }
import cats.free.{ Free => FF }
import scala.util.{ Either => \/ }
import fs2.util.{ Catchable, Suspendable }
#-cats

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

object statement {

  // Algebra of operations for Statement. Each accepts a visitor as an alternatie to pattern-matching.
  sealed trait StatementOp[A] {
    def visit[F[_]](v: StatementOp.Visitor[F]): F[A]
  }

  // Free monad over StatementOp.
  type StatementIO[A] = FF[StatementOp, A]

  // Module of instances and constructors of StatementOp.
  object StatementOp {

    // Given a Statement we can embed a StatementIO program in any algebra that understands embedding.
    implicit val StatementOpEmbeddable: Embeddable[StatementOp, Statement] =
      new Embeddable[StatementOp, Statement] {
        def embed[A](j: Statement, fa: FF[StatementOp, A]) = Embedded.Statement(j, fa)
      }

    // Interface for a natural tansformation StatementOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (StatementOp ~> F) {
      final def apply[A](fa: StatementOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: Statement => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def delay[A](a: () => A): F[A]
      def attempt[A](fa: StatementIO[A]): F[Throwable \/ A]

      // Statement
      def addBatch(a: String): F[Unit]
      def cancel: F[Unit]
      def clearBatch: F[Unit]
      def clearWarnings: F[Unit]
      def close: F[Unit]
      def closeOnCompletion: F[Unit]
      def execute(a: String): F[Boolean]
      def execute(a: String, b: Array[Int]): F[Boolean]
      def execute(a: String, b: Array[String]): F[Boolean]
      def execute(a: String, b: Int): F[Boolean]
      def executeBatch: F[Array[Int]]
      def executeLargeBatch: F[Array[Long]]
      def executeLargeUpdate(a: String): F[Long]
      def executeLargeUpdate(a: String, b: Array[Int]): F[Long]
      def executeLargeUpdate(a: String, b: Array[String]): F[Long]
      def executeLargeUpdate(a: String, b: Int): F[Long]
      def executeQuery(a: String): F[ResultSet]
      def executeUpdate(a: String): F[Int]
      def executeUpdate(a: String, b: Array[Int]): F[Int]
      def executeUpdate(a: String, b: Array[String]): F[Int]
      def executeUpdate(a: String, b: Int): F[Int]
      def getConnection: F[Connection]
      def getFetchDirection: F[Int]
      def getFetchSize: F[Int]
      def getGeneratedKeys: F[ResultSet]
      def getLargeMaxRows: F[Long]
      def getLargeUpdateCount: F[Long]
      def getMaxFieldSize: F[Int]
      def getMaxRows: F[Int]
      def getMoreResults: F[Boolean]
      def getMoreResults(a: Int): F[Boolean]
      def getQueryTimeout: F[Int]
      def getResultSet: F[ResultSet]
      def getResultSetConcurrency: F[Int]
      def getResultSetHoldability: F[Int]
      def getResultSetType: F[Int]
      def getUpdateCount: F[Int]
      def getWarnings: F[SQLWarning]
      def isCloseOnCompletion: F[Boolean]
      def isClosed: F[Boolean]
      def isPoolable: F[Boolean]
      def isWrapperFor(a: Class[_]): F[Boolean]
      def setCursorName(a: String): F[Unit]
      def setEscapeProcessing(a: Boolean): F[Unit]
      def setFetchDirection(a: Int): F[Unit]
      def setFetchSize(a: Int): F[Unit]
      def setLargeMaxRows(a: Long): F[Unit]
      def setMaxFieldSize(a: Int): F[Unit]
      def setMaxRows(a: Int): F[Unit]
      def setPoolable(a: Boolean): F[Unit]
      def setQueryTimeout(a: Int): F[Unit]
      def unwrap[T](a: Class[T]): F[T]

    }

    // Common operations for all algebras.
    case class Raw[A](f: Statement => A) extends StatementOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    case class Embed[A](e: Embedded[A]) extends StatementOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    case class  Delay[A](a: () => A) extends StatementOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    }
    case class  Attempt[A](fa: StatementIO[A]) extends StatementOp[Throwable \/ A] {
      def visit[F[_]](v: Visitor[F]) = v.attempt(fa)
    }

    // Statement-specific operations.
    case class  AddBatch(a: String) extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.addBatch(a)
    }
    case object Cancel extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.cancel
    }
    case object ClearBatch extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.clearBatch
    }
    case object ClearWarnings extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.clearWarnings
    }
    case object Close extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.close
    }
    case object CloseOnCompletion extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.closeOnCompletion
    }
    case class  Execute(a: String) extends StatementOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.execute(a)
    }
    case class  Execute1(a: String, b: Array[Int]) extends StatementOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.execute(a, b)
    }
    case class  Execute2(a: String, b: Array[String]) extends StatementOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.execute(a, b)
    }
    case class  Execute3(a: String, b: Int) extends StatementOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.execute(a, b)
    }
    case object ExecuteBatch extends StatementOp[Array[Int]] {
      def visit[F[_]](v: Visitor[F]) = v.executeBatch
    }
    case object ExecuteLargeBatch extends StatementOp[Array[Long]] {
      def visit[F[_]](v: Visitor[F]) = v.executeLargeBatch
    }
    case class  ExecuteLargeUpdate(a: String) extends StatementOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.executeLargeUpdate(a)
    }
    case class  ExecuteLargeUpdate1(a: String, b: Array[Int]) extends StatementOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.executeLargeUpdate(a, b)
    }
    case class  ExecuteLargeUpdate2(a: String, b: Array[String]) extends StatementOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.executeLargeUpdate(a, b)
    }
    case class  ExecuteLargeUpdate3(a: String, b: Int) extends StatementOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.executeLargeUpdate(a, b)
    }
    case class  ExecuteQuery(a: String) extends StatementOp[ResultSet] {
      def visit[F[_]](v: Visitor[F]) = v.executeQuery(a)
    }
    case class  ExecuteUpdate(a: String) extends StatementOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.executeUpdate(a)
    }
    case class  ExecuteUpdate1(a: String, b: Array[Int]) extends StatementOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.executeUpdate(a, b)
    }
    case class  ExecuteUpdate2(a: String, b: Array[String]) extends StatementOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.executeUpdate(a, b)
    }
    case class  ExecuteUpdate3(a: String, b: Int) extends StatementOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.executeUpdate(a, b)
    }
    case object GetConnection extends StatementOp[Connection] {
      def visit[F[_]](v: Visitor[F]) = v.getConnection
    }
    case object GetFetchDirection extends StatementOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getFetchDirection
    }
    case object GetFetchSize extends StatementOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getFetchSize
    }
    case object GetGeneratedKeys extends StatementOp[ResultSet] {
      def visit[F[_]](v: Visitor[F]) = v.getGeneratedKeys
    }
    case object GetLargeMaxRows extends StatementOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.getLargeMaxRows
    }
    case object GetLargeUpdateCount extends StatementOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.getLargeUpdateCount
    }
    case object GetMaxFieldSize extends StatementOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getMaxFieldSize
    }
    case object GetMaxRows extends StatementOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getMaxRows
    }
    case object GetMoreResults extends StatementOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.getMoreResults
    }
    case class  GetMoreResults1(a: Int) extends StatementOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.getMoreResults(a)
    }
    case object GetQueryTimeout extends StatementOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getQueryTimeout
    }
    case object GetResultSet extends StatementOp[ResultSet] {
      def visit[F[_]](v: Visitor[F]) = v.getResultSet
    }
    case object GetResultSetConcurrency extends StatementOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getResultSetConcurrency
    }
    case object GetResultSetHoldability extends StatementOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getResultSetHoldability
    }
    case object GetResultSetType extends StatementOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getResultSetType
    }
    case object GetUpdateCount extends StatementOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getUpdateCount
    }
    case object GetWarnings extends StatementOp[SQLWarning] {
      def visit[F[_]](v: Visitor[F]) = v.getWarnings
    }
    case object IsCloseOnCompletion extends StatementOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.isCloseOnCompletion
    }
    case object IsClosed extends StatementOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.isClosed
    }
    case object IsPoolable extends StatementOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.isPoolable
    }
    case class  IsWrapperFor(a: Class[_]) extends StatementOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.isWrapperFor(a)
    }
    case class  SetCursorName(a: String) extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setCursorName(a)
    }
    case class  SetEscapeProcessing(a: Boolean) extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setEscapeProcessing(a)
    }
    case class  SetFetchDirection(a: Int) extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setFetchDirection(a)
    }
    case class  SetFetchSize(a: Int) extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setFetchSize(a)
    }
    case class  SetLargeMaxRows(a: Long) extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setLargeMaxRows(a)
    }
    case class  SetMaxFieldSize(a: Int) extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setMaxFieldSize(a)
    }
    case class  SetMaxRows(a: Int) extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setMaxRows(a)
    }
    case class  SetPoolable(a: Boolean) extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setPoolable(a)
    }
    case class  SetQueryTimeout(a: Int) extends StatementOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setQueryTimeout(a)
    }
    case class  Unwrap[T](a: Class[T]) extends StatementOp[T] {
      def visit[F[_]](v: Visitor[F]) = v.unwrap(a)
    }

  }
  import StatementOp._

  // Smart constructors for operations common to all algebras.
  val unit: StatementIO[Unit] = FF.pure[StatementOp, Unit](())
  def raw[A](f: Statement => A): StatementIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[StatementOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def lift[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[StatementOp, A] = embed(j, fa)
  def delay[A](a: => A): StatementIO[A] = FF.liftF(Delay(() => a))
  def attempt[A](fa: StatementIO[A]): StatementIO[Throwable \/ A] = FF.liftF[StatementOp, Throwable \/ A](Attempt(fa))

  // Smart constructors for Statement-specific operations.
  def addBatch(a: String): StatementIO[Unit] = FF.liftF(AddBatch(a))
  val cancel: StatementIO[Unit] = FF.liftF(Cancel)
  val clearBatch: StatementIO[Unit] = FF.liftF(ClearBatch)
  val clearWarnings: StatementIO[Unit] = FF.liftF(ClearWarnings)
  val close: StatementIO[Unit] = FF.liftF(Close)
  val closeOnCompletion: StatementIO[Unit] = FF.liftF(CloseOnCompletion)
  def execute(a: String): StatementIO[Boolean] = FF.liftF(Execute(a))
  def execute(a: String, b: Array[Int]): StatementIO[Boolean] = FF.liftF(Execute1(a, b))
  def execute(a: String, b: Array[String]): StatementIO[Boolean] = FF.liftF(Execute2(a, b))
  def execute(a: String, b: Int): StatementIO[Boolean] = FF.liftF(Execute3(a, b))
  val executeBatch: StatementIO[Array[Int]] = FF.liftF(ExecuteBatch)
  val executeLargeBatch: StatementIO[Array[Long]] = FF.liftF(ExecuteLargeBatch)
  def executeLargeUpdate(a: String): StatementIO[Long] = FF.liftF(ExecuteLargeUpdate(a))
  def executeLargeUpdate(a: String, b: Array[Int]): StatementIO[Long] = FF.liftF(ExecuteLargeUpdate1(a, b))
  def executeLargeUpdate(a: String, b: Array[String]): StatementIO[Long] = FF.liftF(ExecuteLargeUpdate2(a, b))
  def executeLargeUpdate(a: String, b: Int): StatementIO[Long] = FF.liftF(ExecuteLargeUpdate3(a, b))
  def executeQuery(a: String): StatementIO[ResultSet] = FF.liftF(ExecuteQuery(a))
  def executeUpdate(a: String): StatementIO[Int] = FF.liftF(ExecuteUpdate(a))
  def executeUpdate(a: String, b: Array[Int]): StatementIO[Int] = FF.liftF(ExecuteUpdate1(a, b))
  def executeUpdate(a: String, b: Array[String]): StatementIO[Int] = FF.liftF(ExecuteUpdate2(a, b))
  def executeUpdate(a: String, b: Int): StatementIO[Int] = FF.liftF(ExecuteUpdate3(a, b))
  val getConnection: StatementIO[Connection] = FF.liftF(GetConnection)
  val getFetchDirection: StatementIO[Int] = FF.liftF(GetFetchDirection)
  val getFetchSize: StatementIO[Int] = FF.liftF(GetFetchSize)
  val getGeneratedKeys: StatementIO[ResultSet] = FF.liftF(GetGeneratedKeys)
  val getLargeMaxRows: StatementIO[Long] = FF.liftF(GetLargeMaxRows)
  val getLargeUpdateCount: StatementIO[Long] = FF.liftF(GetLargeUpdateCount)
  val getMaxFieldSize: StatementIO[Int] = FF.liftF(GetMaxFieldSize)
  val getMaxRows: StatementIO[Int] = FF.liftF(GetMaxRows)
  val getMoreResults: StatementIO[Boolean] = FF.liftF(GetMoreResults)
  def getMoreResults(a: Int): StatementIO[Boolean] = FF.liftF(GetMoreResults1(a))
  val getQueryTimeout: StatementIO[Int] = FF.liftF(GetQueryTimeout)
  val getResultSet: StatementIO[ResultSet] = FF.liftF(GetResultSet)
  val getResultSetConcurrency: StatementIO[Int] = FF.liftF(GetResultSetConcurrency)
  val getResultSetHoldability: StatementIO[Int] = FF.liftF(GetResultSetHoldability)
  val getResultSetType: StatementIO[Int] = FF.liftF(GetResultSetType)
  val getUpdateCount: StatementIO[Int] = FF.liftF(GetUpdateCount)
  val getWarnings: StatementIO[SQLWarning] = FF.liftF(GetWarnings)
  val isCloseOnCompletion: StatementIO[Boolean] = FF.liftF(IsCloseOnCompletion)
  val isClosed: StatementIO[Boolean] = FF.liftF(IsClosed)
  val isPoolable: StatementIO[Boolean] = FF.liftF(IsPoolable)
  def isWrapperFor(a: Class[_]): StatementIO[Boolean] = FF.liftF(IsWrapperFor(a))
  def setCursorName(a: String): StatementIO[Unit] = FF.liftF(SetCursorName(a))
  def setEscapeProcessing(a: Boolean): StatementIO[Unit] = FF.liftF(SetEscapeProcessing(a))
  def setFetchDirection(a: Int): StatementIO[Unit] = FF.liftF(SetFetchDirection(a))
  def setFetchSize(a: Int): StatementIO[Unit] = FF.liftF(SetFetchSize(a))
  def setLargeMaxRows(a: Long): StatementIO[Unit] = FF.liftF(SetLargeMaxRows(a))
  def setMaxFieldSize(a: Int): StatementIO[Unit] = FF.liftF(SetMaxFieldSize(a))
  def setMaxRows(a: Int): StatementIO[Unit] = FF.liftF(SetMaxRows(a))
  def setPoolable(a: Boolean): StatementIO[Unit] = FF.liftF(SetPoolable(a))
  def setQueryTimeout(a: Int): StatementIO[Unit] = FF.liftF(SetQueryTimeout(a))
  def unwrap[T](a: Class[T]): StatementIO[T] = FF.liftF(Unwrap(a))

// StatementIO can capture side-effects, and can trap and raise exceptions.
#+scalaz
  implicit val CatchableStatementIO: Catchable[StatementIO] with Capture[StatementIO] =
    new Catchable[StatementIO] with Capture[StatementIO] {
      def attempt[A](f: StatementIO[A]): StatementIO[Throwable \/ A] = statement.attempt(f)
      def fail[A](err: Throwable): StatementIO[A] = delay(throw err)
      def apply[A](a: => A): StatementIO[A] = statement.delay(a)
    }
#-scalaz
#+fs2
  implicit val CatchableStatementIO: Suspendable[StatementIO] with Catchable[StatementIO] =
    new Suspendable[StatementIO] with Catchable[StatementIO] {
      def pure[A](a: A): StatementIO[A] = statement.delay(a)
      override def map[A, B](fa: StatementIO[A])(f: A => B): StatementIO[B] = fa.map(f)
      def flatMap[A, B](fa: StatementIO[A])(f: A => StatementIO[B]): StatementIO[B] = fa.flatMap(f)
      def suspend[A](fa: => StatementIO[A]): StatementIO[A] = FF.suspend(fa)
      override def delay[A](a: => A): StatementIO[A] = statement.delay(a)
      def attempt[A](f: StatementIO[A]): StatementIO[Throwable \/ A] = statement.attempt(f)
      def fail[A](err: Throwable): StatementIO[A] = delay(throw err)
    }
#-fs2

}

