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

object blob {

  // Algebra of operations for Blob. Each accepts a visitor as an alternatie to pattern-matching.
  sealed trait BlobOp[A] {
    def visit[F[_]](v: BlobOp.Visitor[F]): F[A]
  }

  // Free monad over BlobOp.
  type BlobIO[A] = FF[BlobOp, A]

  // Module of instances and constructors of BlobOp.
  object BlobOp {

    // Given a Blob we can embed a BlobIO program in any algebra that understands embedding.
    implicit val BlobOpEmbeddable: Embeddable[BlobOp, Blob] =
      new Embeddable[BlobOp, Blob] {
        def embed[A](j: Blob, fa: FF[BlobOp, A]) = Embedded.Blob(j, fa)
      }

    // Interface for a natural tansformation BlobOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (BlobOp ~> F) {
      final def apply[A](fa: BlobOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: Blob => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def delay[A](a: () => A): F[A]
      def attempt[A](fa: BlobIO[A]): F[Throwable \/ A]

      // Blob
      def free: F[Unit]
      def getBinaryStream: F[InputStream]
      def getBinaryStream(a: Long, b: Long): F[InputStream]
      def getBytes(a: Long, b: Int): F[Array[Byte]]
      def length: F[Long]
      def position(a: Array[Byte], b: Long): F[Long]
      def position(a: Blob, b: Long): F[Long]
      def setBinaryStream(a: Long): F[OutputStream]
      def setBytes(a: Long, b: Array[Byte]): F[Int]
      def setBytes(a: Long, b: Array[Byte], c: Int, d: Int): F[Int]
      def truncate(a: Long): F[Unit]

    }

    // Common operations for all algebras.
    case class Raw[A](f: Blob => A) extends BlobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    case class Embed[A](e: Embedded[A]) extends BlobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    case class  Delay[A](a: () => A) extends BlobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    }
    case class  Attempt[A](fa: BlobIO[A]) extends BlobOp[Throwable \/ A] {
      def visit[F[_]](v: Visitor[F]) = v.attempt(fa)
    }

    // Blob-specific operations.
    case object Free extends BlobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.free
    }
    case object GetBinaryStream extends BlobOp[InputStream] {
      def visit[F[_]](v: Visitor[F]) = v.getBinaryStream
    }
    case class  GetBinaryStream1(a: Long, b: Long) extends BlobOp[InputStream] {
      def visit[F[_]](v: Visitor[F]) = v.getBinaryStream(a, b)
    }
    case class  GetBytes(a: Long, b: Int) extends BlobOp[Array[Byte]] {
      def visit[F[_]](v: Visitor[F]) = v.getBytes(a, b)
    }
    case object Length extends BlobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.length
    }
    case class  Position(a: Array[Byte], b: Long) extends BlobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.position(a, b)
    }
    case class  Position1(a: Blob, b: Long) extends BlobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.position(a, b)
    }
    case class  SetBinaryStream(a: Long) extends BlobOp[OutputStream] {
      def visit[F[_]](v: Visitor[F]) = v.setBinaryStream(a)
    }
    case class  SetBytes(a: Long, b: Array[Byte]) extends BlobOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.setBytes(a, b)
    }
    case class  SetBytes1(a: Long, b: Array[Byte], c: Int, d: Int) extends BlobOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.setBytes(a, b, c, d)
    }
    case class  Truncate(a: Long) extends BlobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.truncate(a)
    }

  }
  import BlobOp._

  // Smart constructors for operations common to all algebras.
  val unit: BlobIO[Unit] = FF.pure[BlobOp, Unit](())
  def raw[A](f: Blob => A): BlobIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[BlobOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def lift[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[BlobOp, A] = embed(j, fa)
  def delay[A](a: => A): BlobIO[A] = FF.liftF(Delay(() => a))
  def attempt[A](fa: BlobIO[A]): BlobIO[Throwable \/ A] = FF.liftF[BlobOp, Throwable \/ A](Attempt(fa))

  // Smart constructors for Blob-specific operations.
  val free: BlobIO[Unit] = FF.liftF(Free)
  val getBinaryStream: BlobIO[InputStream] = FF.liftF(GetBinaryStream)
  def getBinaryStream(a: Long, b: Long): BlobIO[InputStream] = FF.liftF(GetBinaryStream1(a, b))
  def getBytes(a: Long, b: Int): BlobIO[Array[Byte]] = FF.liftF(GetBytes(a, b))
  val length: BlobIO[Long] = FF.liftF(Length)
  def position(a: Array[Byte], b: Long): BlobIO[Long] = FF.liftF(Position(a, b))
  def position(a: Blob, b: Long): BlobIO[Long] = FF.liftF(Position1(a, b))
  def setBinaryStream(a: Long): BlobIO[OutputStream] = FF.liftF(SetBinaryStream(a))
  def setBytes(a: Long, b: Array[Byte]): BlobIO[Int] = FF.liftF(SetBytes(a, b))
  def setBytes(a: Long, b: Array[Byte], c: Int, d: Int): BlobIO[Int] = FF.liftF(SetBytes1(a, b, c, d))
  def truncate(a: Long): BlobIO[Unit] = FF.liftF(Truncate(a))

// BlobIO can capture side-effects, and can trap and raise exceptions.
#+scalaz
  implicit val CatchableBlobIO: Catchable[BlobIO] with Capture[BlobIO] =
    new Catchable[BlobIO] with Capture[BlobIO] {
      def attempt[A](f: BlobIO[A]): BlobIO[Throwable \/ A] = blob.attempt(f)
      def fail[A](err: Throwable): BlobIO[A] = delay(throw err)
      def apply[A](a: => A): BlobIO[A] = blob.delay(a)
    }
#-scalaz
#+fs2
  implicit val CatchableBlobIO: Suspendable[BlobIO] with Catchable[BlobIO] =
    new Suspendable[BlobIO] with Catchable[BlobIO] {
      def pure[A](a: A): BlobIO[A] = blob.delay(a)
      override def map[A, B](fa: BlobIO[A])(f: A => B): BlobIO[B] = fa.map(f)
      def flatMap[A, B](fa: BlobIO[A])(f: A => BlobIO[B]): BlobIO[B] = fa.flatMap(f)
      def suspend[A](fa: => BlobIO[A]): BlobIO[A] = FF.suspend(fa)
      override def delay[A](a: => A): BlobIO[A] = blob.delay(a)
      def attempt[A](f: BlobIO[A]): BlobIO[Throwable \/ A] = blob.attempt(f)
      def fail[A](err: Throwable): BlobIO[A] = delay(throw err)
    }
#-fs2

}

