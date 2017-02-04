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

object nclob {

  // Algebra of operations for NClob. Each accepts a visitor as an alternatie to pattern-matching.
  sealed trait NClobOp[A] {
    def visit[F[_]](v: NClobOp.Visitor[F]): F[A]
  }

  // Free monad over NClobOp.
  type NClobIO[A] = FF[NClobOp, A]

  // Module of instances and constructors of NClobOp.
  object NClobOp {

    // Given a NClob we can embed a NClobIO program in any algebra that understands embedding.
    implicit val NClobOpEmbeddable: Embeddable[NClobOp, NClob] =
      new Embeddable[NClobOp, NClob] {
        def embed[A](j: NClob, fa: FF[NClobOp, A]) = Embedded.NClob(j, fa)
      }

    // Interface for a natural tansformation NClobOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (NClobOp ~> F) {
      final def apply[A](fa: NClobOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: NClob => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def delay[A](a: () => A): F[A]
      def attempt[A](fa: NClobIO[A]): F[Throwable \/ A]

      // NClob
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
    case class Raw[A](f: NClob => A) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    case class Embed[A](e: Embedded[A]) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    case class  Delay[A](a: () => A) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    }
    case class  Attempt[A](fa: NClobIO[A]) extends NClobOp[Throwable \/ A] {
      def visit[F[_]](v: Visitor[F]) = v.attempt(fa)
    }

    // NClob-specific operations.
    case object Free extends NClobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.free
    }
    case object GetAsciiStream extends NClobOp[InputStream] {
      def visit[F[_]](v: Visitor[F]) = v.getAsciiStream
    }
    case object GetCharacterStream extends NClobOp[Reader] {
      def visit[F[_]](v: Visitor[F]) = v.getCharacterStream
    }
    case class  GetCharacterStream1(a: Long, b: Long) extends NClobOp[Reader] {
      def visit[F[_]](v: Visitor[F]) = v.getCharacterStream(a, b)
    }
    case class  GetSubString(a: Long, b: Int) extends NClobOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.getSubString(a, b)
    }
    case object Length extends NClobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.length
    }
    case class  Position(a: Clob, b: Long) extends NClobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.position(a, b)
    }
    case class  Position1(a: String, b: Long) extends NClobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.position(a, b)
    }
    case class  SetAsciiStream(a: Long) extends NClobOp[OutputStream] {
      def visit[F[_]](v: Visitor[F]) = v.setAsciiStream(a)
    }
    case class  SetCharacterStream(a: Long) extends NClobOp[Writer] {
      def visit[F[_]](v: Visitor[F]) = v.setCharacterStream(a)
    }
    case class  SetString(a: Long, b: String) extends NClobOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.setString(a, b)
    }
    case class  SetString1(a: Long, b: String, c: Int, d: Int) extends NClobOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.setString(a, b, c, d)
    }
    case class  Truncate(a: Long) extends NClobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.truncate(a)
    }

  }
  import NClobOp._

  // Smart constructors for operations common to all algebras.
  val unit: NClobIO[Unit] = FF.pure[NClobOp, Unit](())
  def raw[A](f: NClob => A): NClobIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[NClobOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def lift[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[NClobOp, A] = embed(j, fa)
  def delay[A](a: => A): NClobIO[A] = FF.liftF(Delay(() => a))
  def attempt[A](fa: NClobIO[A]): NClobIO[Throwable \/ A] = FF.liftF[NClobOp, Throwable \/ A](Attempt(fa))

  // Smart constructors for NClob-specific operations.
  val free: NClobIO[Unit] = FF.liftF(Free)
  val getAsciiStream: NClobIO[InputStream] = FF.liftF(GetAsciiStream)
  val getCharacterStream: NClobIO[Reader] = FF.liftF(GetCharacterStream)
  def getCharacterStream(a: Long, b: Long): NClobIO[Reader] = FF.liftF(GetCharacterStream1(a, b))
  def getSubString(a: Long, b: Int): NClobIO[String] = FF.liftF(GetSubString(a, b))
  val length: NClobIO[Long] = FF.liftF(Length)
  def position(a: Clob, b: Long): NClobIO[Long] = FF.liftF(Position(a, b))
  def position(a: String, b: Long): NClobIO[Long] = FF.liftF(Position1(a, b))
  def setAsciiStream(a: Long): NClobIO[OutputStream] = FF.liftF(SetAsciiStream(a))
  def setCharacterStream(a: Long): NClobIO[Writer] = FF.liftF(SetCharacterStream(a))
  def setString(a: Long, b: String): NClobIO[Int] = FF.liftF(SetString(a, b))
  def setString(a: Long, b: String, c: Int, d: Int): NClobIO[Int] = FF.liftF(SetString1(a, b, c, d))
  def truncate(a: Long): NClobIO[Unit] = FF.liftF(Truncate(a))

// NClobIO can capture side-effects, and can trap and raise exceptions.
#+scalaz
  implicit val CatchableNClobIO: Catchable[NClobIO] with Capture[NClobIO] =
    new Catchable[NClobIO] with Capture[NClobIO] {
      def attempt[A](f: NClobIO[A]): NClobIO[Throwable \/ A] = nclob.attempt(f)
      def fail[A](err: Throwable): NClobIO[A] = delay(throw err)
      def apply[A](a: => A): NClobIO[A] = nclob.delay(a)
    }
#-scalaz
#+fs2
  implicit val CatchableNClobIO: Suspendable[NClobIO] with Catchable[NClobIO] =
    new Suspendable[NClobIO] with Catchable[NClobIO] {
      def pure[A](a: A): NClobIO[A] = nclob.delay(a)
      override def map[A, B](fa: NClobIO[A])(f: A => B): NClobIO[B] = fa.map(f)
      def flatMap[A, B](fa: NClobIO[A])(f: A => NClobIO[B]): NClobIO[B] = fa.flatMap(f)
      def suspend[A](fa: => NClobIO[A]): NClobIO[A] = FF.suspend(fa)
      override def delay[A](a: => A): NClobIO[A] = nclob.delay(a)
      def attempt[A](f: NClobIO[A]): NClobIO[Throwable \/ A] = nclob.attempt(f)
      def fail[A](err: Throwable): NClobIO[A] = delay(throw err)
    }
#-fs2

}

