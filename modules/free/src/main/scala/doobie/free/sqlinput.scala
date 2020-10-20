// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.free

import cats.~>
import cats.effect.kernel.{ MonadCancel, Poll, Sync }
import cats.free.{ Free => FF } // alias because some algebras have an op called Free
import scala.concurrent.duration.FiniteDuration
import com.github.ghik.silencer.silent

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

@silent("deprecated")
object sqlinput { module =>

  // Algebra of operations for SQLInput. Each accepts a visitor as an alternative to pattern-matching.
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

    // Interface for a natural transformation SQLInputOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (SQLInputOp ~> F) {
      final def apply[A](fa: SQLInputOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: SQLInput => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def raiseError[A](e: Throwable): F[A]
      def handleErrorWith[A](fa: SQLInputIO[A])(f: Throwable => SQLInputIO[A]): F[A]
      def monotonic: F[FiniteDuration]
      def realTime: F[FiniteDuration]
      def delay[A](thunk: => A): F[A]
      def suspend[A](hint: Sync.Type)(thunk: => A): F[A]
      def forceR[A, B](fa: SQLInputIO[A])(fb: SQLInputIO[B]): F[B]
      def uncancelable[A](body: Poll[SQLInputIO] => SQLInputIO[A]): F[A]
      def poll[A](poll: Any, fa: SQLInputIO[A]): F[A]
      def canceled: F[Unit]
      def onCancel[A](fa: SQLInputIO[A], fin: SQLInputIO[Unit]): F[A]

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
    final case class Raw[A](f: SQLInput => A) extends SQLInputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    final case class Embed[A](e: Embedded[A]) extends SQLInputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    final case class RaiseError[A](e: Throwable) extends SQLInputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raiseError(e)
    }
    final case class HandleErrorWith[A](fa: SQLInputIO[A], f: Throwable => SQLInputIO[A]) extends SQLInputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa)(f)
    }
    case object Monotonic extends SQLInputOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.monotonic
    }
    case object Realtime extends SQLInputOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.realTime
    }
    case class Suspend[A](hint: Sync.Type, thunk: () => A) extends SQLInputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.suspend(hint)(thunk())
    }
    case class ForceR[A, B](fa: SQLInputIO[A], fb: SQLInputIO[B]) extends SQLInputOp[B] {
      def visit[F[_]](v: Visitor[F]) = v.forceR(fa)(fb)
    }
    case class Uncancelable[A](body: Poll[SQLInputIO] => SQLInputIO[A]) extends SQLInputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.uncancelable(body)
    }
    case class Poll1[A](poll: Any, fa: SQLInputIO[A]) extends SQLInputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.poll(poll, fa)
    }
    case object Canceled extends SQLInputOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.canceled
    }
    case class OnCancel[A](fa: SQLInputIO[A], fin: SQLInputIO[Unit]) extends SQLInputOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.onCancel(fa, fin)
    }

    // SQLInput-specific operations.
    final case object ReadArray extends SQLInputOp[SqlArray] {
      def visit[F[_]](v: Visitor[F]) = v.readArray
    }
    final case object ReadAsciiStream extends SQLInputOp[InputStream] {
      def visit[F[_]](v: Visitor[F]) = v.readAsciiStream
    }
    final case object ReadBigDecimal extends SQLInputOp[BigDecimal] {
      def visit[F[_]](v: Visitor[F]) = v.readBigDecimal
    }
    final case object ReadBinaryStream extends SQLInputOp[InputStream] {
      def visit[F[_]](v: Visitor[F]) = v.readBinaryStream
    }
    final case object ReadBlob extends SQLInputOp[Blob] {
      def visit[F[_]](v: Visitor[F]) = v.readBlob
    }
    final case object ReadBoolean extends SQLInputOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.readBoolean
    }
    final case object ReadByte extends SQLInputOp[Byte] {
      def visit[F[_]](v: Visitor[F]) = v.readByte
    }
    final case object ReadBytes extends SQLInputOp[Array[Byte]] {
      def visit[F[_]](v: Visitor[F]) = v.readBytes
    }
    final case object ReadCharacterStream extends SQLInputOp[Reader] {
      def visit[F[_]](v: Visitor[F]) = v.readCharacterStream
    }
    final case object ReadClob extends SQLInputOp[Clob] {
      def visit[F[_]](v: Visitor[F]) = v.readClob
    }
    final case object ReadDate extends SQLInputOp[Date] {
      def visit[F[_]](v: Visitor[F]) = v.readDate
    }
    final case object ReadDouble extends SQLInputOp[Double] {
      def visit[F[_]](v: Visitor[F]) = v.readDouble
    }
    final case object ReadFloat extends SQLInputOp[Float] {
      def visit[F[_]](v: Visitor[F]) = v.readFloat
    }
    final case object ReadInt extends SQLInputOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.readInt
    }
    final case object ReadLong extends SQLInputOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.readLong
    }
    final case object ReadNClob extends SQLInputOp[NClob] {
      def visit[F[_]](v: Visitor[F]) = v.readNClob
    }
    final case object ReadNString extends SQLInputOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.readNString
    }
    final case object ReadObject extends SQLInputOp[AnyRef] {
      def visit[F[_]](v: Visitor[F]) = v.readObject
    }
    final case class  ReadObject1[T](a: Class[T]) extends SQLInputOp[T] {
      def visit[F[_]](v: Visitor[F]) = v.readObject(a)
    }
    final case object ReadRef extends SQLInputOp[Ref] {
      def visit[F[_]](v: Visitor[F]) = v.readRef
    }
    final case object ReadRowId extends SQLInputOp[RowId] {
      def visit[F[_]](v: Visitor[F]) = v.readRowId
    }
    final case object ReadSQLXML extends SQLInputOp[SQLXML] {
      def visit[F[_]](v: Visitor[F]) = v.readSQLXML
    }
    final case object ReadShort extends SQLInputOp[Short] {
      def visit[F[_]](v: Visitor[F]) = v.readShort
    }
    final case object ReadString extends SQLInputOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.readString
    }
    final case object ReadTime extends SQLInputOp[Time] {
      def visit[F[_]](v: Visitor[F]) = v.readTime
    }
    final case object ReadTimestamp extends SQLInputOp[Timestamp] {
      def visit[F[_]](v: Visitor[F]) = v.readTimestamp
    }
    final case object ReadURL extends SQLInputOp[URL] {
      def visit[F[_]](v: Visitor[F]) = v.readURL
    }
    final case object WasNull extends SQLInputOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.wasNull
    }

  }
  import SQLInputOp._

  // Smart constructors for operations common to all algebras.
  val unit: SQLInputIO[Unit] = FF.pure[SQLInputOp, Unit](())
  def pure[A](a: A): SQLInputIO[A] = FF.pure[SQLInputOp, A](a)
  def raw[A](f: SQLInput => A): SQLInputIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[SQLInputOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def raiseError[A](err: Throwable): SQLInputIO[A] = FF.liftF[SQLInputOp, A](RaiseError(err))
  def handleErrorWith[A](fa: SQLInputIO[A])(f: Throwable => SQLInputIO[A]): SQLInputIO[A] = FF.liftF[SQLInputOp, A](HandleErrorWith(fa, f))
  val monotonic = FF.liftF[SQLInputOp, FiniteDuration](Monotonic)
  val realtime = FF.liftF[SQLInputOp, FiniteDuration](Realtime)
  def delay[A](thunk: => A) = FF.liftF[SQLInputOp, A](Suspend(Sync.Type.Delay, () => thunk))
  def suspend[A](hint: Sync.Type)(thunk: => A) = FF.liftF[SQLInputOp, A](Suspend(hint, () => thunk))
  def forceR[A, B](fa: SQLInputIO[A])(fb: SQLInputIO[B]) = FF.liftF[SQLInputOp, B](ForceR(fa, fb))
  def uncancelable[A](body: Poll[SQLInputIO] => SQLInputIO[A]) = FF.liftF[SQLInputOp, A](Uncancelable(body))
  def capturePoll[M[_]](mpoll: Poll[M]) = new Poll[SQLInputIO] {
    def apply[A](fa: SQLInputIO[A]) = FF.liftF[SQLInputOp, A](Poll1(mpoll, fa))
  }
  val canceled = FF.liftF[SQLInputOp, Unit](Canceled)
  def onCancel[A](fa: SQLInputIO[A], fin: SQLInputIO[Unit]) = FF.liftF[SQLInputOp, A](OnCancel(fa, fin))

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

  // Typeclass instances for SQLInputIO
  implicit val SyncMonadCancelSQLInputIO: Sync[SQLInputIO] with MonadCancel[SQLInputIO, Throwable] =
    new Sync[SQLInputIO] with MonadCancel[SQLInputIO, Throwable] {
      val monad = FF.catsFreeMonadForFree[SQLInputOp]
      override def pure[A](x: A): SQLInputIO[A] = monad.pure(x)
      override def flatMap[A, B](fa: SQLInputIO[A])(f: A => SQLInputIO[B]): SQLInputIO[B] = monad.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => SQLInputIO[Either[A, B]]): SQLInputIO[B] = monad.tailRecM(a)(f)
      override def raiseError[A](e: Throwable): SQLInputIO[A] = module.raiseError(e)
      override def handleErrorWith[A](fa: SQLInputIO[A])(f: Throwable => SQLInputIO[A]): SQLInputIO[A] = module.handleErrorWith(fa)(f)
      override def monotonic: SQLInputIO[FiniteDuration] = module.monotonic
      override def realTime: SQLInputIO[FiniteDuration] = module.realtime
      override def suspend[A](hint: Sync.Type)(thunk: => A): SQLInputIO[A] = module.suspend(hint)(thunk)
      override def forceR[A, B](fa: SQLInputIO[A])(fb: SQLInputIO[B]): SQLInputIO[B] = module.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[SQLInputIO] => SQLInputIO[A]): SQLInputIO[A] = module.uncancelable(body)
      override def canceled: SQLInputIO[Unit] = module.canceled
      override def onCancel[A](fa: SQLInputIO[A], fin: SQLInputIO[Unit]): SQLInputIO[A] = module.onCancel(fa, fin)
    }

}

