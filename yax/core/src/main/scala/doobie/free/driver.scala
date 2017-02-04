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
import java.util.logging.Logger

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

object driver {

  // Algebra of operations for Driver. Each accepts a visitor as an alternatie to pattern-matching.
  sealed trait DriverOp[A] {
    def visit[F[_]](v: DriverOp.Visitor[F]): F[A]
  }

  // Free monad over DriverOp.
  type DriverIO[A] = FF[DriverOp, A]

  // Module of instances and constructors of DriverOp.
  object DriverOp {

    // Given a Driver we can embed a DriverIO program in any algebra that understands embedding.
    implicit val DriverOpEmbeddable: Embeddable[DriverOp, Driver] =
      new Embeddable[DriverOp, Driver] {
        def embed[A](j: Driver, fa: FF[DriverOp, A]) = Embedded.Driver(j, fa)
      }

    // Interface for a natural tansformation DriverOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (DriverOp ~> F) {
      final def apply[A](fa: DriverOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: Driver => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def delay[A](a: () => A): F[A]
      def attempt[A](fa: DriverIO[A]): F[Throwable \/ A]

      // Driver
      def acceptsURL(a: String): F[Boolean]
      def connect(a: String, b: Properties): F[Connection]
      def getMajorVersion: F[Int]
      def getMinorVersion: F[Int]
      def getParentLogger: F[Logger]
      def getPropertyInfo(a: String, b: Properties): F[Array[DriverPropertyInfo]]
      def jdbcCompliant: F[Boolean]

    }

    // Common operations for all algebras.
    case class Raw[A](f: Driver => A) extends DriverOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    case class Embed[A](e: Embedded[A]) extends DriverOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    case class  Delay[A](a: () => A) extends DriverOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    }
    case class  Attempt[A](fa: DriverIO[A]) extends DriverOp[Throwable \/ A] {
      def visit[F[_]](v: Visitor[F]) = v.attempt(fa)
    }

    // Driver-specific operations.
    case class  AcceptsURL(a: String) extends DriverOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.acceptsURL(a)
    }
    case class  Connect(a: String, b: Properties) extends DriverOp[Connection] {
      def visit[F[_]](v: Visitor[F]) = v.connect(a, b)
    }
    case object GetMajorVersion extends DriverOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getMajorVersion
    }
    case object GetMinorVersion extends DriverOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getMinorVersion
    }
    case object GetParentLogger extends DriverOp[Logger] {
      def visit[F[_]](v: Visitor[F]) = v.getParentLogger
    }
    case class  GetPropertyInfo(a: String, b: Properties) extends DriverOp[Array[DriverPropertyInfo]] {
      def visit[F[_]](v: Visitor[F]) = v.getPropertyInfo(a, b)
    }
    case object JdbcCompliant extends DriverOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.jdbcCompliant
    }

  }
  import DriverOp._

  // Smart constructors for operations common to all algebras.
  val unit: DriverIO[Unit] = FF.pure[DriverOp, Unit](())
  def raw[A](f: Driver => A): DriverIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[DriverOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def lift[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[DriverOp, A] = embed(j, fa)
  def delay[A](a: => A): DriverIO[A] = FF.liftF(Delay(() => a))
  def attempt[A](fa: DriverIO[A]): DriverIO[Throwable \/ A] = FF.liftF[DriverOp, Throwable \/ A](Attempt(fa))

  // Smart constructors for Driver-specific operations.
  def acceptsURL(a: String): DriverIO[Boolean] = FF.liftF(AcceptsURL(a))
  def connect(a: String, b: Properties): DriverIO[Connection] = FF.liftF(Connect(a, b))
  val getMajorVersion: DriverIO[Int] = FF.liftF(GetMajorVersion)
  val getMinorVersion: DriverIO[Int] = FF.liftF(GetMinorVersion)
  val getParentLogger: DriverIO[Logger] = FF.liftF(GetParentLogger)
  def getPropertyInfo(a: String, b: Properties): DriverIO[Array[DriverPropertyInfo]] = FF.liftF(GetPropertyInfo(a, b))
  val jdbcCompliant: DriverIO[Boolean] = FF.liftF(JdbcCompliant)

// DriverIO can capture side-effects, and can trap and raise exceptions.
#+scalaz
  implicit val CatchableDriverIO: Catchable[DriverIO] with Capture[DriverIO] =
    new Catchable[DriverIO] with Capture[DriverIO] {
      def attempt[A](f: DriverIO[A]): DriverIO[Throwable \/ A] = driver.attempt(f)
      def fail[A](err: Throwable): DriverIO[A] = delay(throw err)
      def apply[A](a: => A): DriverIO[A] = driver.delay(a)
    }
#-scalaz
#+fs2
  implicit val CatchableDriverIO: Suspendable[DriverIO] with Catchable[DriverIO] =
    new Suspendable[DriverIO] with Catchable[DriverIO] {
      def pure[A](a: A): DriverIO[A] = driver.delay(a)
      override def map[A, B](fa: DriverIO[A])(f: A => B): DriverIO[B] = fa.map(f)
      def flatMap[A, B](fa: DriverIO[A])(f: A => DriverIO[B]): DriverIO[B] = fa.flatMap(f)
      def suspend[A](fa: => DriverIO[A]): DriverIO[A] = FF.suspend(fa)
      override def delay[A](a: => A): DriverIO[A] = driver.delay(a)
      def attempt[A](f: DriverIO[A]): DriverIO[Throwable \/ A] = driver.attempt(f)
      def fail[A](err: Throwable): DriverIO[A] = delay(throw err)
    }
#-fs2

}

