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

import org.postgresql.copy.{ CopyIn => PGCopyIn }

@silent("deprecated")
object copyin { module =>

  // Algebra of operations for PGCopyIn. Each accepts a visitor as an alternative to pattern-matching.
  sealed trait CopyInOp[A] {
    def visit[F[_]](v: CopyInOp.Visitor[F]): F[A]
  }

  // Free monad over CopyInOp.
  type CopyInIO[A] = FF[CopyInOp, A]

  // Module of instances and constructors of CopyInOp.
  object CopyInOp {

    // Given a PGCopyIn we can embed a CopyInIO program in any algebra that understands embedding.
    implicit val CopyInOpEmbeddable: Embeddable[CopyInOp, PGCopyIn] =
      new Embeddable[CopyInOp, PGCopyIn] {
        def embed[A](j: PGCopyIn, fa: FF[CopyInOp, A]) = Embedded.CopyIn(j, fa)
      }

    // Interface for a natural transformation CopyInOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (CopyInOp ~> F) {
      final def apply[A](fa: CopyInOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: PGCopyIn => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def raiseError[A](e: Throwable): F[A]
      def handleErrorWith[A](fa: CopyInIO[A])(f: Throwable => CopyInIO[A]): F[A]
      def monotonic: F[FiniteDuration]
      def realTime: F[FiniteDuration]
      def delay[A](thunk: => A): F[A]
      def suspend[A](hint: Sync.Type)(thunk: => A): F[A]
      def forceR[A, B](fa: CopyInIO[A])(fb: CopyInIO[B]): F[B]
      def canceled: F[Unit]
      def onCancel[A](fa: CopyInIO[A], fin: CopyInIO[Unit]): F[A]
      def cede: F[Unit]
      def sleep(time: FiniteDuration): F[Unit]
      def evalOn[A](fa: CopyInIO[A], ec: ExecutionContext): F[A]
      def executionContext: F[ExecutionContext]
      def async[A](k: (Either[Throwable, A] => Unit) => CopyInIO[Option[CopyInIO[Unit]]]): F[A]


      // PGCopyIn
      def cancelCopy: F[Unit]
      def endCopy: F[Long]
      def flushCopy: F[Unit]
      def getFieldCount: F[Int]
      def getFieldFormat(a: Int): F[Int]
      def getFormat: F[Int]
      def getHandledRowCount: F[Long]
      def isActive: F[Boolean]
      def writeToCopy(a: Array[Byte], b: Int, c: Int): F[Unit]

    }

    // Common operations for all algebras.
    final case class Raw[A](f: PGCopyIn => A) extends CopyInOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    final case class Embed[A](e: Embedded[A]) extends CopyInOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    final case class RaiseError[A](e: Throwable) extends CopyInOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raiseError(e)
    }
    final case class HandleErrorWith[A](fa: CopyInIO[A], f: Throwable => CopyInIO[A]) extends CopyInOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa)(f)
    }
    case object Monotonic extends CopyInOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.monotonic
    }
    case object Realtime extends CopyInOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.realTime
    }
    case class Suspend[A](hint: Sync.Type, thunk: () => A) extends CopyInOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.suspend(hint)(thunk())
    }
    case class ForceR[A, B](fa: CopyInIO[A], fb: CopyInIO[B]) extends CopyInOp[B] {
      def visit[F[_]](v: Visitor[F]) = v.forceR(fa)(fb)
    }
    case object Canceled extends CopyInOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.canceled
    }
    case class OnCancel[A](fa: CopyInIO[A], fin: CopyInIO[Unit]) extends CopyInOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.onCancel(fa, fin)
    }
    case object Cede extends CopyInOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.cede
    }
    case class Sleep(time: FiniteDuration) extends CopyInOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.sleep(time)
    }
    case class EvalOn[A](fa: CopyInIO[A], ec: ExecutionContext) extends CopyInOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.evalOn(fa, ec)
    }
    case object ExecutionContext1 extends CopyInOp[ExecutionContext] {
      def visit[F[_]](v: Visitor[F]) = v.executionContext
    }
    case class Async1[A](k: (Either[Throwable, A] => Unit) => CopyInIO[Option[CopyInIO[Unit]]]) extends CopyInOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.async(k)
    }

    // PGCopyIn-specific operations.
    final case object CancelCopy extends CopyInOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.cancelCopy
    }
    final case object EndCopy extends CopyInOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.endCopy
    }
    final case object FlushCopy extends CopyInOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.flushCopy
    }
    final case object GetFieldCount extends CopyInOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getFieldCount
    }
    final case class  GetFieldFormat(a: Int) extends CopyInOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getFieldFormat(a)
    }
    final case object GetFormat extends CopyInOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getFormat
    }
    final case object GetHandledRowCount extends CopyInOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.getHandledRowCount
    }
    final case object IsActive extends CopyInOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.isActive
    }
    final case class  WriteToCopy(a: Array[Byte], b: Int, c: Int) extends CopyInOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeToCopy(a, b, c)
    }

  }
  import CopyInOp._

  // Smart constructors for operations common to all algebras.
  val unit: CopyInIO[Unit] = FF.pure[CopyInOp, Unit](())
  def pure[A](a: A): CopyInIO[A] = FF.pure[CopyInOp, A](a)
  def raw[A](f: PGCopyIn => A): CopyInIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[CopyInOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def raiseError[A](err: Throwable): CopyInIO[A] = FF.liftF[CopyInOp, A](RaiseError(err))
  def handleErrorWith[A](fa: CopyInIO[A])(f: Throwable => CopyInIO[A]): CopyInIO[A] = FF.liftF[CopyInOp, A](HandleErrorWith(fa, f))
  val monotonic = FF.liftF[CopyInOp, FiniteDuration](Monotonic)
  val realtime = FF.liftF[CopyInOp, FiniteDuration](Realtime)
  def delay[A](thunk: => A) = FF.liftF[CopyInOp, A](Suspend(Sync.Type.Delay, () => thunk))
  def suspend[A](hint: Sync.Type)(thunk: => A) = FF.liftF[CopyInOp, A](Suspend(hint, () => thunk))
  def forceR[A, B](fa: CopyInIO[A])(fb: CopyInIO[B]) = FF.liftF[CopyInOp, B](ForceR(fa, fb))
  val canceled = FF.liftF[CopyInOp, Unit](Canceled)
  def onCancel[A](fa: CopyInIO[A], fin: CopyInIO[Unit]) = FF.liftF[CopyInOp, A](OnCancel(fa, fin))
  val cede = FF.liftF[CopyInOp, Unit](Cede)
  def sleep(time: FiniteDuration) = FF.liftF[CopyInOp, Unit](Sleep(time))
  def evalOn[A](fa: CopyInIO[A], ec: ExecutionContext) = FF.liftF[CopyInOp, A](EvalOn(fa, ec))
  val executionContext = FF.liftF[CopyInOp, ExecutionContext](ExecutionContext1)
  def async[A](k: (Either[Throwable, A] => Unit) => CopyInIO[Option[CopyInIO[Unit]]]) = FF.liftF[CopyInOp, A](Async1(k))

  // Smart constructors for CopyIn-specific operations.
  val cancelCopy: CopyInIO[Unit] = FF.liftF(CancelCopy)
  val endCopy: CopyInIO[Long] = FF.liftF(EndCopy)
  val flushCopy: CopyInIO[Unit] = FF.liftF(FlushCopy)
  val getFieldCount: CopyInIO[Int] = FF.liftF(GetFieldCount)
  def getFieldFormat(a: Int): CopyInIO[Int] = FF.liftF(GetFieldFormat(a))
  val getFormat: CopyInIO[Int] = FF.liftF(GetFormat)
  val getHandledRowCount: CopyInIO[Long] = FF.liftF(GetHandledRowCount)
  val isActive: CopyInIO[Boolean] = FF.liftF(IsActive)
  def writeToCopy(a: Array[Byte], b: Int, c: Int): CopyInIO[Unit] = FF.liftF(WriteToCopy(a, b, c))

  // CopyInIO is an Async
  implicit val AsyncCopyInIO: Async[CopyInIO] =
    new Async[CopyInIO] {
      val asyncM = FF.catsFreeMonadForFree[CopyInOp]
      override def pure[A](x: A): CopyInIO[A] = asyncM.pure(x)
      override def flatMap[A, B](fa: CopyInIO[A])(f: A => CopyInIO[B]): CopyInIO[B] = asyncM.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => CopyInIO[Either[A, B]]): CopyInIO[B] = asyncM.tailRecM(a)(f)
      override def raiseError[A](e: Throwable): CopyInIO[A] = module.raiseError(e)
      override def handleErrorWith[A](fa: CopyInIO[A])(f: Throwable => CopyInIO[A]): CopyInIO[A] = module.handleErrorWith(fa)(f)
      override def monotonic: CopyInIO[FiniteDuration] = module.monotonic
      override def realTime: CopyInIO[FiniteDuration] = module.realtime
      override def suspend[A](hint: Sync.Type)(thunk: => A): CopyInIO[A] = module.suspend(hint)(thunk)
      override def forceR[A, B](fa: CopyInIO[A])(fb: CopyInIO[B]): CopyInIO[B] = module.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[CopyInIO] => CopyInIO[A]): CopyInIO[A] = module.raiseError(new Exception("Unimplemented"))
      override def canceled: CopyInIO[Unit] = module.canceled
      override def onCancel[A](fa: CopyInIO[A], fin: CopyInIO[Unit]): CopyInIO[A] = module.onCancel(fa, fin)
      override def start[A](fa: CopyInIO[A]): CopyInIO[Fiber[CopyInIO, Throwable, A]] = module.raiseError(new Exception("Unimplemented"))
      override def cede: CopyInIO[Unit] = module.cede
      override def racePair[A, B](fa: CopyInIO[A], fb: CopyInIO[B]): CopyInIO[Either[(Outcome[CopyInIO, Throwable, A], Fiber[CopyInIO, Throwable, B]), (Fiber[CopyInIO, Throwable, A], Outcome[CopyInIO, Throwable, B])]] = module.raiseError(new Exception("Unimplemented"))
      override def ref[A](a: A): CopyInIO[CERef[CopyInIO, A]] = module.raiseError(new Exception("Unimplemented"))
      override def deferred[A]: CopyInIO[Deferred[CopyInIO, A]] = module.raiseError(new Exception("Unimplemented"))
      override def sleep(time: FiniteDuration): CopyInIO[Unit] = module.sleep(time)
      override def evalOn[A](fa: CopyInIO[A], ec: ExecutionContext): CopyInIO[A] = module.evalOn(fa, ec)
      override def executionContext: CopyInIO[ExecutionContext] = module.executionContext
      override def async[A](k: (Either[Throwable, A] => Unit) => CopyInIO[Option[CopyInIO[Unit]]]) = module.async(k)
      override def cont[A](body: Cont[CopyInIO, A]): CopyInIO[A] = Async.defaultCont(body)(this)
    }

}

