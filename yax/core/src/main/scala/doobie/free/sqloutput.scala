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
import java.io.Reader
import java.lang.Object
import java.lang.String
import java.math.BigDecimal
import java.net.URL
import java.sql.Blob
import java.sql.CallableStatement
import java.sql.Clob
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.Date
import java.sql.Driver
import java.sql.NClob
import java.sql.PreparedStatement
import java.sql.Ref
import java.sql.ResultSet
import java.sql.RowId
import java.sql.SQLData
import java.sql.SQLInput
import java.sql.SQLOutput
import java.sql.SQLType
import java.sql.SQLXML
import java.sql.Statement
import java.sql.Struct
import java.sql.Time
import java.sql.Timestamp
import java.sql.{ Array => SqlArray }

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

object sqloutput {

  // Algebra of operations for SQLOutput. Each accepts a visitor as an alternatie to pattern-matching.
  sealed trait SQLOutputOp[A] {
    def visit[F[_]](v: SQLOutputOp.Visitor[F]): F[A]
  }

  // Free monad over SQLOutputOp.
  type SQLOutputIO[A] = FF[SQLOutputOp, A]

  // Module of instances and constructors of SQLOutputOp.
  object SQLOutputOp {

    // Given a SQLOutput we can embed a SQLOutputIO program in any algebra that understands embedding.
    implicit val SQLOutputOpEmbeddable: Embeddable[SQLOutputOp, SQLOutput] =
      new Embeddable[SQLOutputOp, SQLOutput] {
        def embed[A](j: SQLOutput, fa: FF[SQLOutputOp, A]) = Embedded.SQLOutput(j, fa)
      }

    // Interface for a natural tansformation SQLOutputOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (SQLOutputOp ~> F) {
      final def apply[A](fa: SQLOutputOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: SQLOutput => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def delay[A](a: () => A): F[A]
      def attempt[A](fa: SQLOutputIO[A]): F[Throwable \/ A]

      // SQLOutput
      def writeArray(a: SqlArray): F[Unit]
      def writeAsciiStream(a: InputStream): F[Unit]
      def writeBigDecimal(a: BigDecimal): F[Unit]
      def writeBinaryStream(a: InputStream): F[Unit]
      def writeBlob(a: Blob): F[Unit]
      def writeBoolean(a: Boolean): F[Unit]
      def writeByte(a: Byte): F[Unit]
      def writeBytes(a: Array[Byte]): F[Unit]
      def writeCharacterStream(a: Reader): F[Unit]
      def writeClob(a: Clob): F[Unit]
      def writeDate(a: Date): F[Unit]
      def writeDouble(a: Double): F[Unit]
      def writeFloat(a: Float): F[Unit]
      def writeInt(a: Int): F[Unit]
      def writeLong(a: Long): F[Unit]
      def writeNClob(a: NClob): F[Unit]
      def writeNString(a: String): F[Unit]
      def writeObject(a: AnyRef, b: SQLType): F[Unit]
      def writeObject(a: SQLData): F[Unit]
      def writeRef(a: Ref): F[Unit]
      def writeRowId(a: RowId): F[Unit]
      def writeSQLXML(a: SQLXML): F[Unit]
      def writeShort(a: Short): F[Unit]
      def writeString(a: String): F[Unit]
      def writeStruct(a: Struct): F[Unit]
      def writeTime(a: Time): F[Unit]
      def writeTimestamp(a: Timestamp): F[Unit]
      def writeURL(a: URL): F[Unit]

    }

    // Common operations for all algebras.
    case class Raw[A](f: SQLOutput => A) extends SQLOutputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    case class Embed[A](e: Embedded[A]) extends SQLOutputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    case class  Delay[A](a: () => A) extends SQLOutputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    }
    case class  Attempt[A](fa: SQLOutputIO[A]) extends SQLOutputOp[Throwable \/ A] {
      def visit[F[_]](v: Visitor[F]) = v.attempt(fa)
    }

    // SQLOutput-specific operations.
    case class  WriteArray(a: SqlArray) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeArray(a)
    }
    case class  WriteAsciiStream(a: InputStream) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeAsciiStream(a)
    }
    case class  WriteBigDecimal(a: BigDecimal) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeBigDecimal(a)
    }
    case class  WriteBinaryStream(a: InputStream) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeBinaryStream(a)
    }
    case class  WriteBlob(a: Blob) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeBlob(a)
    }
    case class  WriteBoolean(a: Boolean) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeBoolean(a)
    }
    case class  WriteByte(a: Byte) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeByte(a)
    }
    case class  WriteBytes(a: Array[Byte]) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeBytes(a)
    }
    case class  WriteCharacterStream(a: Reader) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeCharacterStream(a)
    }
    case class  WriteClob(a: Clob) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeClob(a)
    }
    case class  WriteDate(a: Date) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeDate(a)
    }
    case class  WriteDouble(a: Double) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeDouble(a)
    }
    case class  WriteFloat(a: Float) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeFloat(a)
    }
    case class  WriteInt(a: Int) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeInt(a)
    }
    case class  WriteLong(a: Long) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeLong(a)
    }
    case class  WriteNClob(a: NClob) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeNClob(a)
    }
    case class  WriteNString(a: String) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeNString(a)
    }
    case class  WriteObject(a: AnyRef, b: SQLType) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeObject(a, b)
    }
    case class  WriteObject1(a: SQLData) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeObject(a)
    }
    case class  WriteRef(a: Ref) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeRef(a)
    }
    case class  WriteRowId(a: RowId) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeRowId(a)
    }
    case class  WriteSQLXML(a: SQLXML) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeSQLXML(a)
    }
    case class  WriteShort(a: Short) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeShort(a)
    }
    case class  WriteString(a: String) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeString(a)
    }
    case class  WriteStruct(a: Struct) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeStruct(a)
    }
    case class  WriteTime(a: Time) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeTime(a)
    }
    case class  WriteTimestamp(a: Timestamp) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeTimestamp(a)
    }
    case class  WriteURL(a: URL) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeURL(a)
    }

  }
  import SQLOutputOp._

  // Smart constructors for operations common to all algebras.
  val unit: SQLOutputIO[Unit] = FF.pure[SQLOutputOp, Unit](())
  def raw[A](f: SQLOutput => A): SQLOutputIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[SQLOutputOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def lift[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[SQLOutputOp, A] = embed(j, fa)
  def delay[A](a: => A): SQLOutputIO[A] = FF.liftF(Delay(() => a))
  def attempt[A](fa: SQLOutputIO[A]): SQLOutputIO[Throwable \/ A] = FF.liftF[SQLOutputOp, Throwable \/ A](Attempt(fa))

  // Smart constructors for SQLOutput-specific operations.
  def writeArray(a: SqlArray): SQLOutputIO[Unit] = FF.liftF(WriteArray(a))
  def writeAsciiStream(a: InputStream): SQLOutputIO[Unit] = FF.liftF(WriteAsciiStream(a))
  def writeBigDecimal(a: BigDecimal): SQLOutputIO[Unit] = FF.liftF(WriteBigDecimal(a))
  def writeBinaryStream(a: InputStream): SQLOutputIO[Unit] = FF.liftF(WriteBinaryStream(a))
  def writeBlob(a: Blob): SQLOutputIO[Unit] = FF.liftF(WriteBlob(a))
  def writeBoolean(a: Boolean): SQLOutputIO[Unit] = FF.liftF(WriteBoolean(a))
  def writeByte(a: Byte): SQLOutputIO[Unit] = FF.liftF(WriteByte(a))
  def writeBytes(a: Array[Byte]): SQLOutputIO[Unit] = FF.liftF(WriteBytes(a))
  def writeCharacterStream(a: Reader): SQLOutputIO[Unit] = FF.liftF(WriteCharacterStream(a))
  def writeClob(a: Clob): SQLOutputIO[Unit] = FF.liftF(WriteClob(a))
  def writeDate(a: Date): SQLOutputIO[Unit] = FF.liftF(WriteDate(a))
  def writeDouble(a: Double): SQLOutputIO[Unit] = FF.liftF(WriteDouble(a))
  def writeFloat(a: Float): SQLOutputIO[Unit] = FF.liftF(WriteFloat(a))
  def writeInt(a: Int): SQLOutputIO[Unit] = FF.liftF(WriteInt(a))
  def writeLong(a: Long): SQLOutputIO[Unit] = FF.liftF(WriteLong(a))
  def writeNClob(a: NClob): SQLOutputIO[Unit] = FF.liftF(WriteNClob(a))
  def writeNString(a: String): SQLOutputIO[Unit] = FF.liftF(WriteNString(a))
  def writeObject(a: AnyRef, b: SQLType): SQLOutputIO[Unit] = FF.liftF(WriteObject(a, b))
  def writeObject(a: SQLData): SQLOutputIO[Unit] = FF.liftF(WriteObject1(a))
  def writeRef(a: Ref): SQLOutputIO[Unit] = FF.liftF(WriteRef(a))
  def writeRowId(a: RowId): SQLOutputIO[Unit] = FF.liftF(WriteRowId(a))
  def writeSQLXML(a: SQLXML): SQLOutputIO[Unit] = FF.liftF(WriteSQLXML(a))
  def writeShort(a: Short): SQLOutputIO[Unit] = FF.liftF(WriteShort(a))
  def writeString(a: String): SQLOutputIO[Unit] = FF.liftF(WriteString(a))
  def writeStruct(a: Struct): SQLOutputIO[Unit] = FF.liftF(WriteStruct(a))
  def writeTime(a: Time): SQLOutputIO[Unit] = FF.liftF(WriteTime(a))
  def writeTimestamp(a: Timestamp): SQLOutputIO[Unit] = FF.liftF(WriteTimestamp(a))
  def writeURL(a: URL): SQLOutputIO[Unit] = FF.liftF(WriteURL(a))

// SQLOutputIO can capture side-effects, and can trap and raise exceptions.
#+scalaz
  implicit val CatchableSQLOutputIO: Catchable[SQLOutputIO] with Capture[SQLOutputIO] =
    new Catchable[SQLOutputIO] with Capture[SQLOutputIO] {
      def attempt[A](f: SQLOutputIO[A]): SQLOutputIO[Throwable \/ A] = sqloutput.attempt(f)
      def fail[A](err: Throwable): SQLOutputIO[A] = delay(throw err)
      def apply[A](a: => A): SQLOutputIO[A] = sqloutput.delay(a)
    }
#-scalaz
#+fs2
  implicit val CatchableSQLOutputIO: Suspendable[SQLOutputIO] with Catchable[SQLOutputIO] =
    new Suspendable[SQLOutputIO] with Catchable[SQLOutputIO] {
      def pure[A](a: A): SQLOutputIO[A] = sqloutput.delay(a)
      override def map[A, B](fa: SQLOutputIO[A])(f: A => B): SQLOutputIO[B] = fa.map(f)
      def flatMap[A, B](fa: SQLOutputIO[A])(f: A => SQLOutputIO[B]): SQLOutputIO[B] = fa.flatMap(f)
      def suspend[A](fa: => SQLOutputIO[A]): SQLOutputIO[A] = FF.suspend(fa)
      override def delay[A](a: => A): SQLOutputIO[A] = sqloutput.delay(a)
      def attempt[A](f: SQLOutputIO[A]): SQLOutputIO[Throwable \/ A] = sqloutput.attempt(f)
      def fail[A](err: Throwable): SQLOutputIO[A] = delay(throw err)
    }
#-fs2

}

