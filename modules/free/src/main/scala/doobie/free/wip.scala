// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.free

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext

import cats.~>
import cats.data.Kleisli
import cats.effect.{ Async, Fiber, Outcome, Sync, Poll, Cont }
import cats.free.{ Free => FF } // alias because some algebras have an op called Free
import cats.effect.kernel.{ Deferred, Ref }

object Wip { module =>

  sealed trait WipOp[A] {
    def visit[F[_]](v: WipOp.Visitor[F]): F[A]
  }

  type WipIO[A] = FF[WipOp, A]

  object WipOp {

    trait Visitor[F[_]] extends (WipOp ~> F) {
      final def apply[A](fa: WipOp[A]): F[A] = fa.visit(this)
      // Common
      override def raiseError[A](e: Throwable): F[A]
      override def handleErrorWith[A](fa: WipIO[A])(f: Throwable => WipIO[A]): F[A]
      override def monotonic: F[FiniteDuration]
      override def realTime: F[FiniteDuration]
      override def suspend[A](hint: Sync.Type)(thunk: => A): F[A]
      override def forceR[A, B](fa: WipIO[A])(fb: WipIO[B]): F[B]
      override def uncancelable[G[_], A](body: Poll[G] => WipIO[A]): F[A]
      override def canceled: F[Unit]
      override def onCancel[A](fa: WipIO[A], fin: WipIO[Unit]): F[A]
      override def start[G[_], A](fa: WipIO[A]): F[Fiber[G, Throwable, A]]
      override def cede: F[Unit]
      override def racePair[G[_], A, B](fa: WipIO[A], fb: WipIO[B]): F[Either[(Outcome[G, Throwable, A], Fiber[G, Throwable, B]), (Fiber[G, Throwable, A], Outcome[G, Throwable, B])]]
      override def ref[G[_], A](a: A): F[Ref[G, A]]
      override def deferred[G[_], A]: F[Deferred[G, A]]
      override def sleep(time: FiniteDuration): F[Unit]
      override def evalOn[A](fa: WipIO[A], ec: ExecutionContext): F[A]
      override def executionContext: F[ExecutionContext]
      override def cont[G[_], A](body: Cont[G, A]): F[A]
    }

    final case class RaiseError[A](e: Throwable) extends WipOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raiseError(e)
    }

    final case class HandleErrorWith[A](fa: WipIO[A], f: Throwable => WipIO[A]) extends WipOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa)(f)
    }

    case object Monotonic extends WipOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.monotonic
    }

    case object Realtime extends WipOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.realTime
    }

    case class Suspend[A](hint: Sync.Type, thunk: () => A) extends WipOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.suspend(hint)(thunk())
    }

    case class ForceR[A, B](fa: WipIO[A], fb: WipIO[B]) extends WipOp[B] {
      def visit[F[_]](v: Visitor[F]) = v.forceR(fa)(fb)
    }

    case class Uncacellable[G[_], A](body: Poll[G] => WipIO[A]) extends WipOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.uncancelable(body)
    }

    case object Canceled extends WipOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.canceled
    }

    case class OnCancel[A](fa: WipIO[A], fin: WipIO[Unit]) extends WipOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.onCancel(fa, fin)
    }

    case class Start[G[_], A](fa: WipIO[A]) extends WipOp[Fiber[G, Throwable, A]] {
      def visit[F[_]](v: Visitor[F]) = v.start(fa)
    }

    case object Cede extends WipOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.cede
    }

    case class RacePair[G[_], A, B](fa: WipIO[A], fb: WipIO[B]) extends WipOp[Either[(Outcome[G, Throwable, A], Fiber[G, Throwable, B]), (Fiber[G, Throwable, A], Outcome[G, Throwable, B])]] {
      def visit[F[_]](v: Visitor[F]) = v.racePair(fa, fb)
    }

    case class Ref1[G[_], A](a: A) extends WipOp[Ref[G, A]] {
      def visit[F[_]](v: Visitor[F]) = v.ref(a)
    }

    case class Deferred1[G[_], A]() extends WipOp[Deferred[G, A]] {
      def visit[F[_]](v: Visitor[F]) = v.deferred
    }

    case class Sleep(time: FiniteDuration) extends WipOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.sleep(time)
    }

    case class EvalOn[A](fa: WipIO[A], ec: ExecutionContext) extends WipOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.evalOn(fa, ec)
    }

    case object ExecutionContext1 extends WipOp[ExecutionContext] {
      def visit[F[_]](v: Visitor[F]) = v.executionContext
    }

    case class Cont1[G[_], A](body: Cont[G, A]) extends WipOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.cont(body)
    }
  }

  import WipOp._

  // Smart constructors for operations common to all algebras.
  val unit: WipIO[Unit] = FF.pure[WipOp, Unit](())
  def pure[A](a: A): WipIO[A] = FF.pure[WipOp, A](a)
  def raiseError[A](err: Throwable): WipIO[A] = FF.liftF[WipOp, A](RaiseError(err))
  def handleErrorWith[A](fa: WipIO[A])(f: Throwable => WipIO[A]): WipIO[A] = FF.liftF[WipOp, A](HandleErrorWith(fa, f))
  val monotonic = FF.liftF[WipOp, FiniteDuration](Monotonic)
  val realtime = FF.liftF[WipOp, FiniteDuration](Realtime)
  def suspend[A](hint: Sync.Type)(thunk: => A) = FF.liftF[WipOp, A](Suspend(hint, () => thunk))
  def forceR[A, B](fa: WipIO[A])(fb: WipIO[B]) = FF.liftF[WipOp, B](ForceR(fa, fb))
  def uncancelable[G[_], A](body: Poll[G] => WipIO[A]) = FF.liftF[WipOp, A](Uncacellable(body))
  val canceled = FF.liftF[WipOp, Unit](Canceled)
  def onCancel[A](fa: WipIO[A], fin: WipIO[Unit]) = FF.liftF[WipOp, A](OnCancel(fa, fin))
  def start[G[_], A](fa: WipIO[A]) = FF.liftF[WipOp, Fiber[G, Throwable, A]](Start(fa))
  val cede = FF.liftF[WipOp, Unit](Cede)
  def racePair[G[_], A, B](fa: WipIO[A], fb: WipIO[B]) = FF.liftF[WipOp, Either[(Outcome[G, Throwable, A], Fiber[G, Throwable, B]), (Fiber[G, Throwable, A], Outcome[G, Throwable, B])]](RacePair(fa, fb))
  def ref[G[_], A](a: A) = FF.liftF[WipOp, Ref[G, A]](Ref1(a))
  def deferred[G[_], A] = FF.liftF[WipOp, Deferred[G, A]](Deferred1())
  def sleep(time: FiniteDuration) = FF.liftF[WipOp, Unit](Sleep(time))
  def evalOn[A](fa: WipIO[A], ec: ExecutionContext) = FF.liftF[WipOp, A](EvalOn(fa, ec))
  val executionContext = FF.liftF[WipOp, ExecutionContext](ExecutionContext1)
  def cont[G[_], A](body: Cont[G, A]) = FF.liftF[WipOp, A](Cont1(body))

  implicit val AsyncWipIO = new Async[WipIO] {
    val asyncM = FF.catsFreeMonadForFree[WipOp]
    override def pure[A](x: A): WipIO[A] = asyncM.pure(x)
    override def flatMap[A, B](fa: WipIO[A])(f: A => WipIO[B]): WipIO[B] = asyncM.flatMap(fa)(f)
    override def tailRecG[A, B](a: A)(f: A => WipIO[Either[A, B]]): WipIO[B] = asyncM.tailRecM(a)(f)
    override def raiseError[A](e: Throwable): WipIO[A] = module.raiseError(e)
    override def handleErrorWith[A](fa: WipIO[A])(f: Throwable => WipIO[A]): WipIO[A] = module.handleErrorWith(fa)(f)
    override def monotonic: WipIO[FiniteDuration] = module.monotonic
    override def realTime: WipIO[FiniteDuration] = module.realtime
    override def suspend[A](hint: Sync.Type)(thunk: => A): WipIO[A] = module.suspend(hint)(thunk)
    override def forceR[A, B](fa: WipIO[A])(fb: WipIO[B]): WipIO[B] = module.forceR(fa)(fb)
    override def uncancelable[A](body: Poll[WipIO] => WipIO[A]): WipIO[A] = module.uncancelable(body)
    override def canceled: WipIO[Unit] = module.canceled
    override def onCancel[A](fa: WipIO[A], fin: WipIO[Unit]): WipIO[A] = module.onCancel(fa, fin)
    override def start[A](fa: WipIO[A]): WipIO[Fiber[WipIO, Throwable, A]] = module.start(fa)
    override def cede: WipIO[Unit] = module.cede
    override def racePair[A, B](fa: WipIO[A], fb: WipIO[B]): WipIO[Either[(Outcome[WipIO, Throwable, A], Fiber[WipIO, Throwable, B]), (Fiber[WipIO, Throwable, A], Outcome[WipIO, Throwable, B])]] = module.racePair(fa, fb)
    override def ref[A](a: A): WipIO[Ref[WipIO, A]] = module.ref(a)
    override def deferred[A]: WipIO[Deferred[WipIO, A]] = module.deferred
    override def sleep(time: FiniteDuration): WipIO[Unit] = module.sleep(time)
    override def evalOn[A](fa: WipIO[A], ec: ExecutionContext): WipIO[A] = module.evalOn(fa, ec)
    override def executionContext: WipIO[ExecutionContext] = module.executionContext
    override def cont[A](body: Cont[WipIO, A]): WipIO[A] = module.cont(body)
  }

  trait KleisliInterpeter[M[_]] { outer =>

    implicit val asyncM: Async[M]

    def raiseError[J, A](e: Throwable): Kleisli[M, J, A] = Kleisli(_ => asyncM.raiseError(e))
    def monotonic[J]: Kleisli[M, J, FiniteDuration] = Kleisli(_ => asyncM.monotonic)
    def realTime[J]: Kleisli[M, J, FiniteDuration] = Kleisli(_ => asyncM.realTime)
    def suspend[J, A](hint: Sync.Type)(thunk: => A): Kleisli[M, J, A] = Kleisli(_ => asyncM.suspend(hint)(thunk))
    def canceled[J]: Kleisli[M, J, Unit] = Kleisli(_ => asyncM.canceled)
    def cede[J]: Kleisli[M, J, Unit] = Kleisli(_ => asyncM.cede)
    def sleep[J](time: FiniteDuration): Kleisli[M, J, Unit] = Kleisli(_ => asyncM.sleep(time))
    def executionContext[J]: Kleisli[M, J, ExecutionContext] = Kleisli(_ => asyncM.executionContext)
    def ref[J, A](a: A): Kleisli[M, J, Ref[M, A]] = Kleisli(_ => asyncM.ref(a))
    def deferred[J, A]: Kleisli[M, J, Deferred[M, A]] = Kleisli(_ => asyncM.deferred)
    def cont[J, A](body: Cont[M, A]): Kleisli[M, J, A] = Kleisli(_ => asyncM.cont(body))

    type Wip = Unit

    lazy val WipInterpeter: WipOp ~> Kleisli[M, Wip, *] = new WipInterpeter { }

    trait WipInterpeter extends WipOp.Visitor[Kleisli[M, Wip, *]] {
      override def raiseError[A](e: Throwable): Kleisli[M, Wip, A] = outer.raiseError(e)
      override def handleErrorWith[A](fa: WipIO[A])(f: Throwable => WipIO[A]): Kleisli[M, Wip, A] = Kleisli (j =>
        asyncM.handleErrorWith(fa.foldMap(this).run(j))(f.andThen(_.foldMap(this).run(j)))
      )
      override def monotonic: Kleisli[M, Wip, FiniteDuration] = outer.monotonic
      override def realTime: Kleisli[M, Wip, FiniteDuration] = outer.realTime
      override def suspend[A](hint: Sync.Type)(thunk: => A): Kleisli[M, Wip, A] = outer.suspend(hint)(thunk)
      override def forceR[A, B](fa: WipIO[A])(fb: WipIO[B]): Kleisli[M, Wip, B] = Kleisli (j =>
        asyncM.forceR(fa.foldMap(this).run(j))(fb.foldMap(this).run(j))
      )
      override def uncancelable[A](body: Poll[M] => WipIO[A]): Kleisli[M, Wip, A] = Kleisli (j =>
         asyncM.uncancelable(body.andThen(_.foldMap(this).run(j)))
      )
      override def canceled: Kleisli[M, Wip, Unit] = outer.canceled
      override def onCancel[A](fa: WipIO[A], fin: WipIO[Unit]): Kleisli[M, Wip, A] = Kleisli (j =>
        asyncM.onCancel(fa.foldMap(this).run(j), fin.foldMap(this).run(j))
      )
      override def start[A](fa: WipIO[A]): Kleisli[M, Wip, Fiber[M, Throwable, A]] =
        Kleisli(j => asyncM.start(fa.foldMap(this).run(j)))
      override def cede: Kleisli[M, Wip, Unit] = outer.cede
      override def racePair[A, B](fa: WipIO[A], fb: WipIO[B]): Kleisli[M, Wip, Either[(Outcome[M, Throwable, A], Fiber[M, Throwable, B]), (Fiber[M, Throwable, A], Outcome[M, Throwable, B])]] = Kleisli(j =>
        asyncM.racePair(fa.foldMap(this).run(j), fb.foldMap(this).run(j))
      )
      override def ref[A](a: A): Kleisli[M, Wip, Ref[M, A]] = outer.ref(a)
      override def deferred[A]: Kleisli[M, Wip, Deferred[M, A]] = outer.deferred
      override def sleep(time: FiniteDuration): Kleisli[M, Wip, Unit] = outer.sleep(time)
      override def evalOn[A](fa: WipIO[A], ec: ExecutionContext): Kleisli[M, Wip, A] =
        Kleisli(j => asyncM.evalOn(fa.foldMap(this).run(j), ec))
      override def executionContext: Kleisli[M, Wip, ExecutionContext] = outer.executionContext
      override def cont[A](body: Cont[M, A]): Kleisli[M, Wip, A] = outer.cont(body)
    }

  }

}
