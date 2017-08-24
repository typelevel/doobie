// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.free

import cats.~>
import cats.effect.Async
import cats.free.{ Free => FF } // alias because some algebras have an op called Free

import java.io.InputStream
import java.io.Reader
import java.lang.Class
import java.lang.String
import java.math.BigDecimal
import java.net.URL
import java.sql.Blob
import java.sql.Clob
import java.sql.Date
import java.sql.NClob
import java.sql.Ref
import java.sql.RowId
import java.sql.SQLInput
import java.sql.SQLXML
import java.sql.Time
import java.sql.Timestamp
import java.sql.{ Array => SqlArray }

object sqlinput { module =>

  // Algebra of operations for SQLInput. Each accepts a visitor as an alternatie to pattern-matching.
  sealed trait SQLInputOp[A] {
    def visit[F[_]](v: SQLInputOp.Visitor[F]): F[A]
  }

  // Free monad over SQLInputOp.
  type SQLInputIO[A] = FF[SQLInputOp, A]

  // Module of instances and constructors of SQLInputOp.
  object SQLInputOp {

    // Given a SQLInput we can embed a SQLInputIO program in any algebra that understands embedding.
    implicit val SQLInputOpEmbeddable: Embeddable[SQLInputOp, SQLInput] =
      new Embeddable[SQLInputOp, SQLInput] {
        def embed[A](j: SQLInput, fa: FF[SQLInputOp, A]) = Embedded.SQLInput(j, fa)
      }

    // Interface for a natural tansformation SQLInputOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (SQLInputOp ~> F) {
      final def apply[A](fa: SQLInputOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: SQLInput => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def delay[A](a: () => A): F[A]
      def handleErrorWith[A](fa: SQLInputIO[A], f: Throwable => SQLInputIO[A]): F[A]
      def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A]

      // SQLInput
      def readArray: F[SqlArray]
      def readAsciiStream: F[InputStream]
      def readBigDecimal: F[BigDecimal]
      def readBinaryStream: F[InputStream]
      def readBlob: F[Blob]
      def readBoolean: F[Boolean]
      def readByte: F[Byte]
      def readBytes: F[Array[Byte]]
      def readCharacterStream: F[Reader]
      def readClob: F[Clob]
      def readDate: F[Date]
      def readDouble: F[Double]
      def readFloat: F[Float]
      def readInt: F[Int]
      def readLong: F[Long]
      def readNClob: F[NClob]
      def readNString: F[String]
      def readObject: F[AnyRef]
      def readObject[T](a: Class[T]): F[T]
      def readRef: F[Ref]
      def readRowId: F[RowId]
      def readSQLXML: F[SQLXML]
      def readShort: F[Short]
      def readString: F[String]
      def readTime: F[Time]
      def readTimestamp: F[Timestamp]
      def readURL: F[URL]
      def wasNull: F[Boolean]

    }

    // Common operations for all algebras.
    case class Raw[A](f: SQLInput => A) extends SQLInputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    case class Embed[A](e: Embedded[A]) extends SQLInputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    case class Delay[A](a: () => A) extends SQLInputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    }
    case class HandleErrorWith[A](fa: SQLInputIO[A], f: Throwable => SQLInputIO[A]) extends SQLInputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa, f)
    }
    case class Async1[A](k: (Either[Throwable, A] => Unit) => Unit) extends SQLInputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.async(k)
    }

    // SQLInput-specific operations.
    case object ReadArray extends SQLInputOp[SqlArray] {
      def visit[F[_]](v: Visitor[F]) = v.readArray
    }
    case object ReadAsciiStream extends SQLInputOp[InputStream] {
      def visit[F[_]](v: Visitor[F]) = v.readAsciiStream
    }
    case object ReadBigDecimal extends SQLInputOp[BigDecimal] {
      def visit[F[_]](v: Visitor[F]) = v.readBigDecimal
    }
    case object ReadBinaryStream extends SQLInputOp[InputStream] {
      def visit[F[_]](v: Visitor[F]) = v.readBinaryStream
    }
    case object ReadBlob extends SQLInputOp[Blob] {
      def visit[F[_]](v: Visitor[F]) = v.readBlob
    }
    case object ReadBoolean extends SQLInputOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.readBoolean
    }
    case object ReadByte extends SQLInputOp[Byte] {
      def visit[F[_]](v: Visitor[F]) = v.readByte
    }
    case object ReadBytes extends SQLInputOp[Array[Byte]] {
      def visit[F[_]](v: Visitor[F]) = v.readBytes
    }
    case object ReadCharacterStream extends SQLInputOp[Reader] {
      def visit[F[_]](v: Visitor[F]) = v.readCharacterStream
    }
    case object ReadClob extends SQLInputOp[Clob] {
      def visit[F[_]](v: Visitor[F]) = v.readClob
    }
    case object ReadDate extends SQLInputOp[Date] {
      def visit[F[_]](v: Visitor[F]) = v.readDate
    }
    case object ReadDouble extends SQLInputOp[Double] {
      def visit[F[_]](v: Visitor[F]) = v.readDouble
    }
    case object ReadFloat extends SQLInputOp[Float] {
      def visit[F[_]](v: Visitor[F]) = v.readFloat
    }
    case object ReadInt extends SQLInputOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.readInt
    }
    case object ReadLong extends SQLInputOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.readLong
    }
    case object ReadNClob extends SQLInputOp[NClob] {
      def visit[F[_]](v: Visitor[F]) = v.readNClob
    }
    case object ReadNString extends SQLInputOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.readNString
    }
    case object ReadObject extends SQLInputOp[AnyRef] {
      def visit[F[_]](v: Visitor[F]) = v.readObject
    }
    case class  ReadObject1[T](a: Class[T]) extends SQLInputOp[T] {
      def visit[F[_]](v: Visitor[F]) = v.readObject(a)
    }
    case object ReadRef extends SQLInputOp[Ref] {
      def visit[F[_]](v: Visitor[F]) = v.readRef
    }
    case object ReadRowId extends SQLInputOp[RowId] {
      def visit[F[_]](v: Visitor[F]) = v.readRowId
    }
    case object ReadSQLXML extends SQLInputOp[SQLXML] {
      def visit[F[_]](v: Visitor[F]) = v.readSQLXML
    }
    case object ReadShort extends SQLInputOp[Short] {
      def visit[F[_]](v: Visitor[F]) = v.readShort
    }
    case object ReadString extends SQLInputOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.readString
    }
    case object ReadTime extends SQLInputOp[Time] {
      def visit[F[_]](v: Visitor[F]) = v.readTime
    }
    case object ReadTimestamp extends SQLInputOp[Timestamp] {
      def visit[F[_]](v: Visitor[F]) = v.readTimestamp
    }
    case object ReadURL extends SQLInputOp[URL] {
      def visit[F[_]](v: Visitor[F]) = v.readURL
    }
    case object WasNull extends SQLInputOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.wasNull
    }

  }
  import SQLInputOp._

  // Smart constructors for operations common to all algebras.
  val unit: SQLInputIO[Unit] = FF.pure[SQLInputOp, Unit](())
  def raw[A](f: SQLInput => A): SQLInputIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[SQLInputOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def delay[A](a: => A): SQLInputIO[A] = FF.liftF(Delay(() => a))
  def handleErrorWith[A](fa: SQLInputIO[A], f: Throwable => SQLInputIO[A]): SQLInputIO[A] = FF.liftF[SQLInputOp, A](HandleErrorWith(fa, f))
  def raiseError[A](err: Throwable): SQLInputIO[A] = delay(throw err)
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): SQLInputIO[A] = FF.liftF[SQLInputOp, A](Async1(k))

  // Smart constructors for SQLInput-specific operations.
  val readArray: SQLInputIO[SqlArray] = FF.liftF(ReadArray)
  val readAsciiStream: SQLInputIO[InputStream] = FF.liftF(ReadAsciiStream)
  val readBigDecimal: SQLInputIO[BigDecimal] = FF.liftF(ReadBigDecimal)
  val readBinaryStream: SQLInputIO[InputStream] = FF.liftF(ReadBinaryStream)
  val readBlob: SQLInputIO[Blob] = FF.liftF(ReadBlob)
  val readBoolean: SQLInputIO[Boolean] = FF.liftF(ReadBoolean)
  val readByte: SQLInputIO[Byte] = FF.liftF(ReadByte)
  val readBytes: SQLInputIO[Array[Byte]] = FF.liftF(ReadBytes)
  val readCharacterStream: SQLInputIO[Reader] = FF.liftF(ReadCharacterStream)
  val readClob: SQLInputIO[Clob] = FF.liftF(ReadClob)
  val readDate: SQLInputIO[Date] = FF.liftF(ReadDate)
  val readDouble: SQLInputIO[Double] = FF.liftF(ReadDouble)
  val readFloat: SQLInputIO[Float] = FF.liftF(ReadFloat)
  val readInt: SQLInputIO[Int] = FF.liftF(ReadInt)
  val readLong: SQLInputIO[Long] = FF.liftF(ReadLong)
  val readNClob: SQLInputIO[NClob] = FF.liftF(ReadNClob)
  val readNString: SQLInputIO[String] = FF.liftF(ReadNString)
  val readObject: SQLInputIO[AnyRef] = FF.liftF(ReadObject)
  def readObject[T](a: Class[T]): SQLInputIO[T] = FF.liftF(ReadObject1(a))
  val readRef: SQLInputIO[Ref] = FF.liftF(ReadRef)
  val readRowId: SQLInputIO[RowId] = FF.liftF(ReadRowId)
  val readSQLXML: SQLInputIO[SQLXML] = FF.liftF(ReadSQLXML)
  val readShort: SQLInputIO[Short] = FF.liftF(ReadShort)
  val readString: SQLInputIO[String] = FF.liftF(ReadString)
  val readTime: SQLInputIO[Time] = FF.liftF(ReadTime)
  val readTimestamp: SQLInputIO[Timestamp] = FF.liftF(ReadTimestamp)
  val readURL: SQLInputIO[URL] = FF.liftF(ReadURL)
  val wasNull: SQLInputIO[Boolean] = FF.liftF(WasNull)

  // SQLInputIO is an Async
  implicit val AsyncSQLInputIO: Async[SQLInputIO] =
    new Async[SQLInputIO] {
      val M = FF.catsFreeMonadForFree[SQLInputOp]
      def pure[A](x: A): SQLInputIO[A] = M.pure(x)
      def handleErrorWith[A](fa: SQLInputIO[A])(f: Throwable => SQLInputIO[A]): SQLInputIO[A] = module.handleErrorWith(fa, f)
      def raiseError[A](e: Throwable): SQLInputIO[A] = module.raiseError(e)
      def async[A](k: (Either[Throwable,A] => Unit) => Unit): SQLInputIO[A] = module.async(k)
      def flatMap[A, B](fa: SQLInputIO[A])(f: A => SQLInputIO[B]): SQLInputIO[B] = M.flatMap(fa)(f)
      def tailRecM[A, B](a: A)(f: A => SQLInputIO[Either[A, B]]): SQLInputIO[B] = M.tailRecM(a)(f)
      def suspend[A](thunk: => SQLInputIO[A]): SQLInputIO[A] = M.flatten(module.delay(thunk))
    }

}

