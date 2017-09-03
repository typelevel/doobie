package doobie.free

import cats.~>
import cats.effect.Async
import cats.free.{ Free => FF } // alias because some algebras have an op called Free

import java.io.InputStream
import java.io.Reader
import java.lang.String
import java.math.BigDecimal
import java.net.URL
import java.sql.Blob
import java.sql.Clob
import java.sql.Date
import java.sql.NClob
import java.sql.Ref
import java.sql.RowId
import java.sql.SQLData
import java.sql.SQLOutput
import java.sql.SQLType
import java.sql.SQLXML
import java.sql.Struct
import java.sql.Time
import java.sql.Timestamp
import java.sql.{ Array => SqlArray }

@SuppressWarnings(Array("org.wartremover.warts.Overloading"))
object sqloutput { module =>

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
      def handleErrorWith[A](fa: SQLOutputIO[A], f: Throwable => SQLOutputIO[A]): F[A]
      def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A]

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
    final case class Raw[A](f: SQLOutput => A) extends SQLOutputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    final case class Embed[A](e: Embedded[A]) extends SQLOutputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    final case class Delay[A](a: () => A) extends SQLOutputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    }
    final case class HandleErrorWith[A](fa: SQLOutputIO[A], f: Throwable => SQLOutputIO[A]) extends SQLOutputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa, f)
    }
    final case class Async1[A](k: (Either[Throwable, A] => Unit) => Unit) extends SQLOutputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.async(k)
    }

    // SQLOutput-specific operations.
    final case class  WriteArray(a: SqlArray) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeArray(a)
    }
    final case class  WriteAsciiStream(a: InputStream) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeAsciiStream(a)
    }
    final case class  WriteBigDecimal(a: BigDecimal) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeBigDecimal(a)
    }
    final case class  WriteBinaryStream(a: InputStream) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeBinaryStream(a)
    }
    final case class  WriteBlob(a: Blob) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeBlob(a)
    }
    final case class  WriteBoolean(a: Boolean) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeBoolean(a)
    }
    final case class  WriteByte(a: Byte) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeByte(a)
    }
    final case class  WriteBytes(a: Array[Byte]) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeBytes(a)
    }
    final case class  WriteCharacterStream(a: Reader) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeCharacterStream(a)
    }
    final case class  WriteClob(a: Clob) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeClob(a)
    }
    final case class  WriteDate(a: Date) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeDate(a)
    }
    final case class  WriteDouble(a: Double) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeDouble(a)
    }
    final case class  WriteFloat(a: Float) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeFloat(a)
    }
    final case class  WriteInt(a: Int) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeInt(a)
    }
    final case class  WriteLong(a: Long) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeLong(a)
    }
    final case class  WriteNClob(a: NClob) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeNClob(a)
    }
    final case class  WriteNString(a: String) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeNString(a)
    }
    final case class  WriteObject(a: AnyRef, b: SQLType) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeObject(a, b)
    }
    final case class  WriteObject1(a: SQLData) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeObject(a)
    }
    final case class  WriteRef(a: Ref) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeRef(a)
    }
    final case class  WriteRowId(a: RowId) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeRowId(a)
    }
    final case class  WriteSQLXML(a: SQLXML) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeSQLXML(a)
    }
    final case class  WriteShort(a: Short) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeShort(a)
    }
    final case class  WriteString(a: String) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeString(a)
    }
    final case class  WriteStruct(a: Struct) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeStruct(a)
    }
    final case class  WriteTime(a: Time) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeTime(a)
    }
    final case class  WriteTimestamp(a: Timestamp) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeTimestamp(a)
    }
    final case class  WriteURL(a: URL) extends SQLOutputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeURL(a)
    }

  }
  import SQLOutputOp._

  // Smart constructors for operations common to all algebras.
  val unit: SQLOutputIO[Unit] = FF.pure[SQLOutputOp, Unit](())
  def raw[A](f: SQLOutput => A): SQLOutputIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[SQLOutputOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def delay[A](a: => A): SQLOutputIO[A] = FF.liftF(Delay(() => a))
  def handleErrorWith[A](fa: SQLOutputIO[A], f: Throwable => SQLOutputIO[A]): SQLOutputIO[A] = FF.liftF[SQLOutputOp, A](HandleErrorWith(fa, f))
  def raiseError[A](err: Throwable): SQLOutputIO[A] = delay(throw err)
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): SQLOutputIO[A] = FF.liftF[SQLOutputOp, A](Async1(k))

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

  // SQLOutputIO is an Async
  implicit val AsyncSQLOutputIO: Async[SQLOutputIO] =
    new Async[SQLOutputIO] {
      val M = FF.catsFreeMonadForFree[SQLOutputOp]
      def pure[A](x: A): SQLOutputIO[A] = M.pure(x)
      def handleErrorWith[A](fa: SQLOutputIO[A])(f: Throwable => SQLOutputIO[A]): SQLOutputIO[A] = module.handleErrorWith(fa, f)
      def raiseError[A](e: Throwable): SQLOutputIO[A] = module.raiseError(e)
      def async[A](k: (Either[Throwable,A] => Unit) => Unit): SQLOutputIO[A] = module.async(k)
      def flatMap[A, B](fa: SQLOutputIO[A])(f: A => SQLOutputIO[B]): SQLOutputIO[B] = M.flatMap(fa)(f)
      def tailRecM[A, B](a: A)(f: A => SQLOutputIO[Either[A, B]]): SQLOutputIO[B] = M.tailRecM(a)(f)
      def suspend[A](thunk: => SQLOutputIO[A]): SQLOutputIO[A] = M.flatten(module.delay(thunk))
    }

}

