// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.free

import cats.~>
import cats.effect.{ Async, Cont, Fiber, Outcome, Poll, Sync }
import cats.effect.kernel.{ Deferred, Ref => CERef }
import cats.free.{ Free => FF } // alias because some algebras have an op called Free
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import com.github.ghik.silencer.silent

import java.io.InputStream
import java.io.OutputStream
import org.postgresql.largeobject.LargeObject

@silent("deprecated")
object largeobject { module =>

  // Algebra of operations for LargeObject. Each accepts a visitor as an alternative to pattern-matching.
  sealed trait LargeObjectOp[A] {
    def visit[F[_]](v: LargeObjectOp.Visitor[F]): F[A]
  }

  // Free monad over LargeObjectOp.
  type LargeObjectIO[A] = FF[LargeObjectOp, A]

  // Module of instances and constructors of LargeObjectOp.
  object LargeObjectOp {

    // Given a LargeObject we can embed a LargeObjectIO program in any algebra that understands embedding.
    implicit val LargeObjectOpEmbeddable: Embeddable[LargeObjectOp, LargeObject] =
      new Embeddable[LargeObjectOp, LargeObject] {
        def embed[A](j: LargeObject, fa: FF[LargeObjectOp, A]) = Embedded.LargeObject(j, fa)
      }

    // Interface for a natural transformation LargeObjectOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (LargeObjectOp ~> F) {
      final def apply[A](fa: LargeObjectOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: LargeObject => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def raiseError[A](e: Throwable): F[A]
      def handleErrorWith[A](fa: LargeObjectIO[A])(f: Throwable => LargeObjectIO[A]): F[A]
      def monotonic: F[FiniteDuration]
      def realTime: F[FiniteDuration]
      def delay[A](thunk: => A): F[A]
      def suspend[A](hint: Sync.Type)(thunk: => A): F[A]
      def forceR[A, B](fa: LargeObjectIO[A])(fb: LargeObjectIO[B]): F[B]
      def uncancelable[A](body: Poll[LargeObjectIO] => LargeObjectIO[A]): F[A]
      def poll[A](poll: Any, fa: LargeObjectIO[A]): F[A]
      def canceled: F[Unit]
      def onCancel[A](fa: LargeObjectIO[A], fin: LargeObjectIO[Unit]): F[A]
      def cede: F[Unit]
      def ref[A](a: A): F[CERef[LargeObjectIO, A]]
      def deferred[A]: F[Deferred[LargeObjectIO, A]]
      def sleep(time: FiniteDuration): F[Unit]
      def evalOn[A](fa: LargeObjectIO[A], ec: ExecutionContext): F[A]
      def executionContext: F[ExecutionContext]
      def async[A](k: (Either[Throwable, A] => Unit) => LargeObjectIO[Option[LargeObjectIO[Unit]]]): F[A]

      // LargeObject
      def close: F[Unit]
      def copy: F[LargeObject]
      def getInputStream: F[InputStream]
      def getInputStream(a: Long): F[InputStream]
      def getLongOID: F[Long]
      def getOID: F[Int]
      def getOutputStream: F[OutputStream]
      def read(a: Array[Byte], b: Int, c: Int): F[Int]
      def read(a: Int): F[Array[Byte]]
      def seek(a: Int): F[Unit]
      def seek(a: Int, b: Int): F[Unit]
      def seek64(a: Long, b: Int): F[Unit]
      def size: F[Int]
      def size64: F[Long]
      def tell: F[Int]
      def tell64: F[Long]
      def truncate(a: Int): F[Unit]
      def truncate64(a: Long): F[Unit]
      def write(a: Array[Byte]): F[Unit]
      def write(a: Array[Byte], b: Int, c: Int): F[Unit]

    }

    // Common operations for all algebras.
final case class Raw[A](f: LargeObject => A) extends LargeObjectOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    final case class Embed[A](e: Embedded[A]) extends LargeObjectOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    final case class RaiseError[A](e: Throwable) extends LargeObjectOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raiseError(e)
    }
    final case class HandleErrorWith[A](fa: LargeObjectIO[A], f: Throwable => LargeObjectIO[A]) extends LargeObjectOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa)(f)
    }
    case object Monotonic extends LargeObjectOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.monotonic
    }
    case object Realtime extends LargeObjectOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.realTime
    }
    case class Suspend[A](hint: Sync.Type, thunk: () => A) extends LargeObjectOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.suspend(hint)(thunk())
    }
    case class ForceR[A, B](fa: LargeObjectIO[A], fb: LargeObjectIO[B]) extends LargeObjectOp[B] {
      def visit[F[_]](v: Visitor[F]) = v.forceR(fa)(fb)
    }
    case class Uncancelable[A](body: Poll[LargeObjectIO] => LargeObjectIO[A]) extends LargeObjectOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.uncancelable(body)
    }
    case class Poll1[A](poll: Any, fa: LargeObjectIO[A]) extends LargeObjectOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.poll(poll, fa)
    }    
    case object Canceled extends LargeObjectOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.canceled
    }
    case class OnCancel[A](fa: LargeObjectIO[A], fin: LargeObjectIO[Unit]) extends LargeObjectOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.onCancel(fa, fin)
    }
    case object Cede extends LargeObjectOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.cede
    }
    case class Ref1[A](a: A) extends LargeObjectOp[CERef[LargeObjectIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.ref(a)
    }
    case class Deferred1[A]() extends LargeObjectOp[Deferred[LargeObjectIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.deferred
    }
    case class Sleep(time: FiniteDuration) extends LargeObjectOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.sleep(time)
    }
    case class EvalOn[A](fa: LargeObjectIO[A], ec: ExecutionContext) extends LargeObjectOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.evalOn(fa, ec)
    }
    case object ExecutionContext1 extends LargeObjectOp[ExecutionContext] {
      def visit[F[_]](v: Visitor[F]) = v.executionContext
    }
    case class Async1[A](k: (Either[Throwable, A] => Unit) => LargeObjectIO[Option[LargeObjectIO[Unit]]]) extends LargeObjectOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.async(k)
    }

    // LargeObject-specific operations.
    final case object Close extends LargeObjectOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.close
    }
    final case object Copy extends LargeObjectOp[LargeObject] {
      def visit[F[_]](v: Visitor[F]) = v.copy
    }
    final case object GetInputStream extends LargeObjectOp[InputStream] {
      def visit[F[_]](v: Visitor[F]) = v.getInputStream
    }
    final case class  GetInputStream1(a: Long) extends LargeObjectOp[InputStream] {
      def visit[F[_]](v: Visitor[F]) = v.getInputStream(a)
    }
    final case object GetLongOID extends LargeObjectOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.getLongOID
    }
    final case object GetOID extends LargeObjectOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getOID
    }
    final case object GetOutputStream extends LargeObjectOp[OutputStream] {
      def visit[F[_]](v: Visitor[F]) = v.getOutputStream
    }
    final case class  Read(a: Array[Byte], b: Int, c: Int) extends LargeObjectOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.read(a, b, c)
    }
    final case class  Read1(a: Int) extends LargeObjectOp[Array[Byte]] {
      def visit[F[_]](v: Visitor[F]) = v.read(a)
    }
    final case class  Seek(a: Int) extends LargeObjectOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.seek(a)
    }
    final case class  Seek1(a: Int, b: Int) extends LargeObjectOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.seek(a, b)
    }
    final case class  Seek64(a: Long, b: Int) extends LargeObjectOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.seek64(a, b)
    }
    final case object Size extends LargeObjectOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.size
    }
    final case object Size64 extends LargeObjectOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.size64
    }
    final case object Tell extends LargeObjectOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.tell
    }
    final case object Tell64 extends LargeObjectOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.tell64
    }
    final case class  Truncate(a: Int) extends LargeObjectOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.truncate(a)
    }
    final case class  Truncate64(a: Long) extends LargeObjectOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.truncate64(a)
    }
    final case class  Write(a: Array[Byte]) extends LargeObjectOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.write(a)
    }
    final case class  Write1(a: Array[Byte], b: Int, c: Int) extends LargeObjectOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.write(a, b, c)
    }

  }
  import LargeObjectOp._

  // Smart constructors for operations common to all algebras.
  val unit: LargeObjectIO[Unit] = FF.pure[LargeObjectOp, Unit](())
  def pure[A](a: A): LargeObjectIO[A] = FF.pure[LargeObjectOp, A](a)
  def raw[A](f: LargeObject => A): LargeObjectIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[LargeObjectOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def raiseError[A](err: Throwable): LargeObjectIO[A] = FF.liftF[LargeObjectOp, A](RaiseError(err))
  def handleErrorWith[A](fa: LargeObjectIO[A])(f: Throwable => LargeObjectIO[A]): LargeObjectIO[A] = FF.liftF[LargeObjectOp, A](HandleErrorWith(fa, f))
  val monotonic = FF.liftF[LargeObjectOp, FiniteDuration](Monotonic)
  val realtime = FF.liftF[LargeObjectOp, FiniteDuration](Realtime)
  def delay[A](thunk: => A) = FF.liftF[LargeObjectOp, A](Suspend(Sync.Type.Delay, () => thunk))
  def suspend[A](hint: Sync.Type)(thunk: => A) = FF.liftF[LargeObjectOp, A](Suspend(hint, () => thunk))
  def forceR[A, B](fa: LargeObjectIO[A])(fb: LargeObjectIO[B]) = FF.liftF[LargeObjectOp, B](ForceR(fa, fb))
  def uncancelable[A](body: Poll[LargeObjectIO] => LargeObjectIO[A]) = FF.liftF[LargeObjectOp, A](Uncancelable(body))
  def capturePoll[M[_]](mpoll: Poll[M]): Poll[LargeObjectIO] = new Poll[LargeObjectIO] {
    def apply[A](fa: LargeObjectIO[A]) = FF.liftF[LargeObjectOp, A](Poll1(mpoll, fa))
  }
  val canceled = FF.liftF[LargeObjectOp, Unit](Canceled)
  def onCancel[A](fa: LargeObjectIO[A], fin: LargeObjectIO[Unit]) = FF.liftF[LargeObjectOp, A](OnCancel(fa, fin))
  val cede = FF.liftF[LargeObjectOp, Unit](Cede)
  def ref[A](a: A) = FF.liftF[LargeObjectOp, CERef[LargeObjectIO, A]](Ref1(a))
  def deferred[A] = FF.liftF[LargeObjectOp, Deferred[LargeObjectIO, A]](Deferred1())
  def sleep(time: FiniteDuration) = FF.liftF[LargeObjectOp, Unit](Sleep(time))
  def evalOn[A](fa: LargeObjectIO[A], ec: ExecutionContext) = FF.liftF[LargeObjectOp, A](EvalOn(fa, ec))
  val executionContext = FF.liftF[LargeObjectOp, ExecutionContext](ExecutionContext1)
  def async[A](k: (Either[Throwable, A] => Unit) => LargeObjectIO[Option[LargeObjectIO[Unit]]]) = FF.liftF[LargeObjectOp, A](Async1(k))

  // Smart constructors for LargeObject-specific operations.
  val close: LargeObjectIO[Unit] = FF.liftF(Close)
  val copy: LargeObjectIO[LargeObject] = FF.liftF(Copy)
  val getInputStream: LargeObjectIO[InputStream] = FF.liftF(GetInputStream)
  def getInputStream(a: Long): LargeObjectIO[InputStream] = FF.liftF(GetInputStream1(a))
  val getLongOID: LargeObjectIO[Long] = FF.liftF(GetLongOID)
  val getOID: LargeObjectIO[Int] = FF.liftF(GetOID)
  val getOutputStream: LargeObjectIO[OutputStream] = FF.liftF(GetOutputStream)
  def read(a: Array[Byte], b: Int, c: Int): LargeObjectIO[Int] = FF.liftF(Read(a, b, c))
  def read(a: Int): LargeObjectIO[Array[Byte]] = FF.liftF(Read1(a))
  def seek(a: Int): LargeObjectIO[Unit] = FF.liftF(Seek(a))
  def seek(a: Int, b: Int): LargeObjectIO[Unit] = FF.liftF(Seek1(a, b))
  def seek64(a: Long, b: Int): LargeObjectIO[Unit] = FF.liftF(Seek64(a, b))
  val size: LargeObjectIO[Int] = FF.liftF(Size)
  val size64: LargeObjectIO[Long] = FF.liftF(Size64)
  val tell: LargeObjectIO[Int] = FF.liftF(Tell)
  val tell64: LargeObjectIO[Long] = FF.liftF(Tell64)
  def truncate(a: Int): LargeObjectIO[Unit] = FF.liftF(Truncate(a))
  def truncate64(a: Long): LargeObjectIO[Unit] = FF.liftF(Truncate64(a))
  def write(a: Array[Byte]): LargeObjectIO[Unit] = FF.liftF(Write(a))
  def write(a: Array[Byte], b: Int, c: Int): LargeObjectIO[Unit] = FF.liftF(Write1(a, b, c))

  // LargeObjectIO is an Async
  implicit val AsyncLargeObjectIO: Async[LargeObjectIO] =
    new Async[LargeObjectIO] {
      val asyncM = FF.catsFreeMonadForFree[LargeObjectOp]
      override def pure[A](x: A): LargeObjectIO[A] = asyncM.pure(x)
      override def flatMap[A, B](fa: LargeObjectIO[A])(f: A => LargeObjectIO[B]): LargeObjectIO[B] = asyncM.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => LargeObjectIO[Either[A, B]]): LargeObjectIO[B] = asyncM.tailRecM(a)(f)
      override def raiseError[A](e: Throwable): LargeObjectIO[A] = module.raiseError(e)
      override def handleErrorWith[A](fa: LargeObjectIO[A])(f: Throwable => LargeObjectIO[A]): LargeObjectIO[A] = module.handleErrorWith(fa)(f)
      override def monotonic: LargeObjectIO[FiniteDuration] = module.monotonic
      override def realTime: LargeObjectIO[FiniteDuration] = module.realtime
      override def suspend[A](hint: Sync.Type)(thunk: => A): LargeObjectIO[A] = module.suspend(hint)(thunk)
      override def forceR[A, B](fa: LargeObjectIO[A])(fb: LargeObjectIO[B]): LargeObjectIO[B] = module.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[LargeObjectIO] => LargeObjectIO[A]): LargeObjectIO[A] = module.uncancelable(body)
      override def canceled: LargeObjectIO[Unit] = module.canceled
      override def onCancel[A](fa: LargeObjectIO[A], fin: LargeObjectIO[Unit]): LargeObjectIO[A] = module.onCancel(fa, fin)
      override def start[A](fa: LargeObjectIO[A]): LargeObjectIO[Fiber[LargeObjectIO, Throwable, A]] = module.raiseError(new Exception("Unimplemented"))
      override def cede: LargeObjectIO[Unit] = module.cede
      override def racePair[A, B](fa: LargeObjectIO[A], fb: LargeObjectIO[B]): LargeObjectIO[Either[(Outcome[LargeObjectIO, Throwable, A], Fiber[LargeObjectIO, Throwable, B]), (Fiber[LargeObjectIO, Throwable, A], Outcome[LargeObjectIO, Throwable, B])]] = module.raiseError(new Exception("Unimplemented"))
      override def ref[A](a: A): LargeObjectIO[CERef[LargeObjectIO, A]] = module.ref(a)
      override def deferred[A]: LargeObjectIO[Deferred[LargeObjectIO, A]] = module.deferred
      override def sleep(time: FiniteDuration): LargeObjectIO[Unit] = module.sleep(time)
      override def evalOn[A](fa: LargeObjectIO[A], ec: ExecutionContext): LargeObjectIO[A] = module.evalOn(fa, ec)
      override def executionContext: LargeObjectIO[ExecutionContext] = module.executionContext
      override def async[A](k: (Either[Throwable, A] => Unit) => LargeObjectIO[Option[LargeObjectIO[Unit]]]) = module.async(k)
      override def cont[A](body: Cont[LargeObjectIO, A]): LargeObjectIO[A] = Async.defaultCont(body)(this)
    }

}

