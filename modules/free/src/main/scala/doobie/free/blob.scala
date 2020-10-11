// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.free

import cats.~>
import cats.effect.{ Async, Cont, Fiber, Outcome, Poll, Sync }
import cats.effect.kernel.{ Deferred, Ref => CERef }
import cats.free.{ Free => FF } // alias because some algebras have an op called Free
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import com.github.ghik.silencer.silent

import java.io.InputStream
import java.io.OutputStream
import java.sql.Blob

@silent("deprecated")
object blob { module =>

  // Algebra of operations for Blob. Each accepts a visitor as an alternative to pattern-matching.
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

    // Interface for a natural transformation BlobOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (BlobOp ~> F) {
      final def apply[A](fa: BlobOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: Blob => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def raiseError[A](e: Throwable): F[A]
      def handleErrorWith[A](fa: BlobIO[A])(f: Throwable => BlobIO[A]): F[A]
      def monotonic: F[FiniteDuration]
      def realTime: F[FiniteDuration]
      def suspend[A](hint: Sync.Type)(thunk: => A): F[A]
      def forceR[A, B](fa: BlobIO[A])(fb: BlobIO[B]): F[B]
      def uncancelable[A](body: Poll[BlobIO] => BlobIO[A]): F[A]
      def poll[A](poll: Any, fa: BlobIO[A]): F[A]
      def canceled: F[Unit]
      def onCancel[A](fa: BlobIO[A], fin: BlobIO[Unit]): F[A]
      def cede: F[Unit]
      def ref[A](a: A): F[CERef[BlobIO, A]]
      def deferred[A]: F[Deferred[BlobIO, A]]
      def sleep(time: FiniteDuration): F[Unit]
      def evalOn[A](fa: BlobIO[A], ec: ExecutionContext): F[A]
      def executionContext: F[ExecutionContext]
      def async[A](k: (Either[Throwable, A] => Unit) => BlobIO[Option[BlobIO[Unit]]]): F[A]

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
    final case class Raw[A](f: Blob => A) extends BlobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    final case class Embed[A](e: Embedded[A]) extends BlobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    final case class RaiseError[A](e: Throwable) extends BlobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raiseError(e)
    }
    final case class HandleErrorWith[A](fa: BlobIO[A], f: Throwable => BlobIO[A]) extends BlobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa)(f)
    }
    case object Monotonic extends BlobOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.monotonic
    }
    case object Realtime extends BlobOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.realTime
    }
    case class Suspend[A](hint: Sync.Type, thunk: () => A) extends BlobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.suspend(hint)(thunk())
    }
    case class ForceR[A, B](fa: BlobIO[A], fb: BlobIO[B]) extends BlobOp[B] {
      def visit[F[_]](v: Visitor[F]) = v.forceR(fa)(fb)
    }
    case class Uncancelable[A](body: Poll[BlobIO] => BlobIO[A]) extends BlobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.uncancelable(body)
    }
    case class Poll1[A](poll: Any, fa: BlobIO[A]) extends BlobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.poll(poll, fa)
    }
    case object Canceled extends BlobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.canceled
    }
    case class OnCancel[A](fa: BlobIO[A], fin: BlobIO[Unit]) extends BlobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.onCancel(fa, fin)
    }
    case object Cede extends BlobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.cede
    }
    case class Ref1[A](a: A) extends BlobOp[CERef[BlobIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.ref(a)
    }
    case class Deferred1[A]() extends BlobOp[Deferred[BlobIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.deferred
    }
    case class Sleep(time: FiniteDuration) extends BlobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.sleep(time)
    }
    case class EvalOn[A](fa: BlobIO[A], ec: ExecutionContext) extends BlobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.evalOn(fa, ec)
    }
    case object ExecutionContext1 extends BlobOp[ExecutionContext] {
      def visit[F[_]](v: Visitor[F]) = v.executionContext
    }
    case class Async1[A](k: (Either[Throwable, A] => Unit) => BlobIO[Option[BlobIO[Unit]]]) extends BlobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.async(k)
    }

    // Blob-specific operations.
    final case object Free extends BlobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.free
    }
    final case object GetBinaryStream extends BlobOp[InputStream] {
      def visit[F[_]](v: Visitor[F]) = v.getBinaryStream
    }
    final case class  GetBinaryStream1(a: Long, b: Long) extends BlobOp[InputStream] {
      def visit[F[_]](v: Visitor[F]) = v.getBinaryStream(a, b)
    }
    final case class  GetBytes(a: Long, b: Int) extends BlobOp[Array[Byte]] {
      def visit[F[_]](v: Visitor[F]) = v.getBytes(a, b)
    }
    final case object Length extends BlobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.length
    }
    final case class  Position(a: Array[Byte], b: Long) extends BlobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.position(a, b)
    }
    final case class  Position1(a: Blob, b: Long) extends BlobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.position(a, b)
    }
    final case class  SetBinaryStream(a: Long) extends BlobOp[OutputStream] {
      def visit[F[_]](v: Visitor[F]) = v.setBinaryStream(a)
    }
    final case class  SetBytes(a: Long, b: Array[Byte]) extends BlobOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.setBytes(a, b)
    }
    final case class  SetBytes1(a: Long, b: Array[Byte], c: Int, d: Int) extends BlobOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.setBytes(a, b, c, d)
    }
    final case class  Truncate(a: Long) extends BlobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.truncate(a)
    }

  }
  import BlobOp._

  // Smart constructors for operations common to all algebras.
  val unit: BlobIO[Unit] = FF.pure[BlobOp, Unit](())
  def pure[A](a: A): BlobIO[A] = FF.pure[BlobOp, A](a)
  def raw[A](f: Blob => A): BlobIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[BlobOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def raiseError[A](err: Throwable): BlobIO[A] = FF.liftF[BlobOp, A](RaiseError(err))
  def handleErrorWith[A](fa: BlobIO[A])(f: Throwable => BlobIO[A]): BlobIO[A] = FF.liftF[BlobOp, A](HandleErrorWith(fa, f))
  val monotonic = FF.liftF[BlobOp, FiniteDuration](Monotonic)
  val realtime = FF.liftF[BlobOp, FiniteDuration](Realtime)
  def suspend[A](hint: Sync.Type)(thunk: => A) = FF.liftF[BlobOp, A](Suspend(hint, () => thunk))
  def forceR[A, B](fa: BlobIO[A])(fb: BlobIO[B]) = FF.liftF[BlobOp, B](ForceR(fa, fb))
  def uncancelable[A](body: Poll[BlobIO] => BlobIO[A]) = FF.liftF[BlobOp, A](Uncancelable(body))
  def capturePoll[M[_]](mpoll: Poll[M]): Poll[BlobIO] = new Poll[BlobIO] {
    def apply[A](fa: BlobIO[A]) = FF.liftF[BlobOp, A](Poll1(mpoll, fa))
  }
  val canceled = FF.liftF[BlobOp, Unit](Canceled)
  def onCancel[A](fa: BlobIO[A], fin: BlobIO[Unit]) = FF.liftF[BlobOp, A](OnCancel(fa, fin))
  val cede = FF.liftF[BlobOp, Unit](Cede)
  def ref[A](a: A) = FF.liftF[BlobOp, CERef[BlobIO, A]](Ref1(a))
  def deferred[A] = FF.liftF[BlobOp, Deferred[BlobIO, A]](Deferred1())
  def sleep(time: FiniteDuration) = FF.liftF[BlobOp, Unit](Sleep(time))
  def evalOn[A](fa: BlobIO[A], ec: ExecutionContext) = FF.liftF[BlobOp, A](EvalOn(fa, ec))
  val executionContext = FF.liftF[BlobOp, ExecutionContext](ExecutionContext1)
  def async[A](k: (Either[Throwable, A] => Unit) => BlobIO[Option[BlobIO[Unit]]]) = FF.liftF[BlobOp, A](Async1(k))

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

  // BlobIO is an Async
  implicit val AsyncBlobIO: Async[BlobIO] =
    new Async[BlobIO] {
      val asyncM = FF.catsFreeMonadForFree[BlobOp]
      override def pure[A](x: A): BlobIO[A] = asyncM.pure(x)
      override def flatMap[A, B](fa: BlobIO[A])(f: A => BlobIO[B]): BlobIO[B] = asyncM.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => BlobIO[Either[A, B]]): BlobIO[B] = asyncM.tailRecM(a)(f)
      override def raiseError[A](e: Throwable): BlobIO[A] = module.raiseError(e)
      override def handleErrorWith[A](fa: BlobIO[A])(f: Throwable => BlobIO[A]): BlobIO[A] = module.handleErrorWith(fa)(f)
      override def monotonic: BlobIO[FiniteDuration] = module.monotonic
      override def realTime: BlobIO[FiniteDuration] = module.realtime
      override def suspend[A](hint: Sync.Type)(thunk: => A): BlobIO[A] = module.suspend(hint)(thunk)
      override def forceR[A, B](fa: BlobIO[A])(fb: BlobIO[B]): BlobIO[B] = module.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[BlobIO] => BlobIO[A]): BlobIO[A] = module.uncancelable(body)
      override def canceled: BlobIO[Unit] = module.canceled
      override def onCancel[A](fa: BlobIO[A], fin: BlobIO[Unit]): BlobIO[A] = module.onCancel(fa, fin)
      override def start[A](fa: BlobIO[A]): BlobIO[Fiber[BlobIO, Throwable, A]] = module.raiseError(new Exception("Unimplemented"))
      override def cede: BlobIO[Unit] = module.cede
      override def racePair[A, B](fa: BlobIO[A], fb: BlobIO[B]): BlobIO[Either[(Outcome[BlobIO, Throwable, A], Fiber[BlobIO, Throwable, B]), (Fiber[BlobIO, Throwable, A], Outcome[BlobIO, Throwable, B])]] = module.raiseError(new Exception("Unimplemented"))
      override def ref[A](a: A): BlobIO[CERef[BlobIO, A]] = module.ref(a)
      override def deferred[A]: BlobIO[Deferred[BlobIO, A]] = module.deferred
      override def sleep(time: FiniteDuration): BlobIO[Unit] = module.sleep(time)
      override def evalOn[A](fa: BlobIO[A], ec: ExecutionContext): BlobIO[A] = module.evalOn(fa, ec)
      override def executionContext: BlobIO[ExecutionContext] = module.executionContext
      override def async[A](k: (Either[Throwable, A] => Unit) => BlobIO[Option[BlobIO[Unit]]]) = module.async(k)
      override def cont[A](body: Cont[BlobIO, A]): BlobIO[A] = Async.defaultCont(body)(this)
    }

}

