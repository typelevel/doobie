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

import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
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

object clob {

  // Algebra of operations for Clob. Each accepts a visitor as an alternatie to pattern-matching.
  sealed trait ClobOp[A] {
    def visit[F[_]](v: ClobOp.Visitor[F]): F[A]
  }

  // Free monad over ClobOp.
  type ClobIO[A] = FF[ClobOp, A]

  // Module of instances and constructors of ClobOp.
  object ClobOp {

    // Given a Clob we can embed a ClobIO program in any algebra that understands embedding.
    implicit val ClobOpEmbeddable: Embeddable[ClobOp, Clob] =
      new Embeddable[ClobOp, Clob] {
        def embed[A](j: Clob, fa: FF[ClobOp, A]) = Embedded.Clob(j, fa)
      }

    // Interface for a natural tansformation ClobOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (ClobOp ~> F) {
      final def apply[A](fa: ClobOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: Clob => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def delay[A](a: () => A): F[A]
      def attempt[A](fa: ClobIO[A]): F[Throwable \/ A]

      // Clob
      def free: F[Unit]
      def getAsciiStream: F[InputStream]
      def getCharacterStream: F[Reader]
      def getCharacterStream(a: Long, b: Long): F[Reader]
      def getSubString(a: Long, b: Int): F[String]
      def length: F[Long]
      def position(a: Clob, b: Long): F[Long]
      def position(a: String, b: Long): F[Long]
      def setAsciiStream(a: Long): F[OutputStream]
      def setCharacterStream(a: Long): F[Writer]
      def setString(a: Long, b: String): F[Int]
      def setString(a: Long, b: String, c: Int, d: Int): F[Int]
      def truncate(a: Long): F[Unit]

    }

    // Common operations for all algebras.
    case class Raw[A](f: Clob => A) extends ClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    case class Embed[A](e: Embedded[A]) extends ClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    case class  Delay[A](a: () => A) extends ClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    }
    case class  Attempt[A](fa: ClobIO[A]) extends ClobOp[Throwable \/ A] {
      def visit[F[_]](v: Visitor[F]) = v.attempt(fa)
    }

    // Clob-specific operations.
    case object Free extends ClobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.free
    }
    case object GetAsciiStream extends ClobOp[InputStream] {
      def visit[F[_]](v: Visitor[F]) = v.getAsciiStream
    }
    case object GetCharacterStream extends ClobOp[Reader] {
      def visit[F[_]](v: Visitor[F]) = v.getCharacterStream
    }
    case class  GetCharacterStream1(a: Long, b: Long) extends ClobOp[Reader] {
      def visit[F[_]](v: Visitor[F]) = v.getCharacterStream(a, b)
    }
    case class  GetSubString(a: Long, b: Int) extends ClobOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.getSubString(a, b)
    }
    case object Length extends ClobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.length
    }
    case class  Position(a: Clob, b: Long) extends ClobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.position(a, b)
    }
    case class  Position1(a: String, b: Long) extends ClobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.position(a, b)
    }
    case class  SetAsciiStream(a: Long) extends ClobOp[OutputStream] {
      def visit[F[_]](v: Visitor[F]) = v.setAsciiStream(a)
    }
    case class  SetCharacterStream(a: Long) extends ClobOp[Writer] {
      def visit[F[_]](v: Visitor[F]) = v.setCharacterStream(a)
    }
    case class  SetString(a: Long, b: String) extends ClobOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.setString(a, b)
    }
    case class  SetString1(a: Long, b: String, c: Int, d: Int) extends ClobOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.setString(a, b, c, d)
    }
    case class  Truncate(a: Long) extends ClobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.truncate(a)
    }

  }
  import ClobOp._

  // Smart constructors for operations common to all algebras.
  val unit: ClobIO[Unit] = FF.pure[ClobOp, Unit](())
  def raw[A](f: Clob => A): ClobIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[ClobOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def lift[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[ClobOp, A] = embed(j, fa)
  def delay[A](a: => A): ClobIO[A] = FF.liftF(Delay(() => a))
  def attempt[A](fa: ClobIO[A]): ClobIO[Throwable \/ A] = FF.liftF[ClobOp, Throwable \/ A](Attempt(fa))

  // Smart constructors for Clob-specific operations.
  val free: ClobIO[Unit] = FF.liftF(Free)
  val getAsciiStream: ClobIO[InputStream] = FF.liftF(GetAsciiStream)
  val getCharacterStream: ClobIO[Reader] = FF.liftF(GetCharacterStream)
  def getCharacterStream(a: Long, b: Long): ClobIO[Reader] = FF.liftF(GetCharacterStream1(a, b))
  def getSubString(a: Long, b: Int): ClobIO[String] = FF.liftF(GetSubString(a, b))
  val length: ClobIO[Long] = FF.liftF(Length)
  def position(a: Clob, b: Long): ClobIO[Long] = FF.liftF(Position(a, b))
  def position(a: String, b: Long): ClobIO[Long] = FF.liftF(Position1(a, b))
  def setAsciiStream(a: Long): ClobIO[OutputStream] = FF.liftF(SetAsciiStream(a))
  def setCharacterStream(a: Long): ClobIO[Writer] = FF.liftF(SetCharacterStream(a))
  def setString(a: Long, b: String): ClobIO[Int] = FF.liftF(SetString(a, b))
  def setString(a: Long, b: String, c: Int, d: Int): ClobIO[Int] = FF.liftF(SetString1(a, b, c, d))
  def truncate(a: Long): ClobIO[Unit] = FF.liftF(Truncate(a))

// ClobIO can capture side-effects, and can trap and raise exceptions.
#+scalaz
  implicit val CatchableClobIO: Catchable[ClobIO] with Capture[ClobIO] =
    new Catchable[ClobIO] with Capture[ClobIO] {
      def attempt[A](f: ClobIO[A]): ClobIO[Throwable \/ A] = clob.attempt(f)
      def fail[A](err: Throwable): ClobIO[A] = delay(throw err)
      def apply[A](a: => A): ClobIO[A] = clob.delay(a)
    }
#-scalaz
#+fs2
  implicit val CatchableClobIO: Suspendable[ClobIO] with Catchable[ClobIO] =
    new Suspendable[ClobIO] with Catchable[ClobIO] {
      def pure[A](a: A): ClobIO[A] = clob.delay(a)
      override def map[A, B](fa: ClobIO[A])(f: A => B): ClobIO[B] = fa.map(f)
      def flatMap[A, B](fa: ClobIO[A])(f: A => ClobIO[B]): ClobIO[B] = fa.flatMap(f)
      def suspend[A](fa: => ClobIO[A]): ClobIO[A] = FF.suspend(fa)
      override def delay[A](a: => A): ClobIO[A] = clob.delay(a)
      def attempt[A](f: ClobIO[A]): ClobIO[Throwable \/ A] = clob.attempt(f)
      def fail[A](err: Throwable): ClobIO[A] = delay(throw err)
    }
#-fs2

}

