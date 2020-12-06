// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie

import cats.~>
import cats.implicits._
import cats.effect.kernel.{ Async, MonadCancel, Poll, Resource, Sync }
import cats.effect.std.Dispatcher
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import cats.Applicative

trait WeakAsync[F[_]] {
  def pure[A](x: A): F[A]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B]
  def raiseError[A](e: Throwable): F[A]
  def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A]
  def monotonic: F[FiniteDuration]
  def realTime: F[FiniteDuration]
  def suspend[A](hint: Sync.Type)(thunk: => A): F[A]
  def delay[A](thunk: => A): F[A] = suspend(Sync.Type.Delay)(thunk)
  def blocking[A](thunk: => A): F[A] = suspend(Sync.Type.Blocking)(thunk)
  def forceR[A, B](fa: F[A])(fb: F[B]): F[B]
  def uncancelable[A](body: Poll[F] => F[A]): F[A]
  def canceled: F[Unit]
  def onCancel[A](fa: F[A], fin: F[Unit]): F[A]
  def fromFuture[A](fut: F[Future[A]]): F[A]
}

trait WeakAsyncLowPriorityInstances {

  implicit def doobieSyncForWeakAsync[F[_]](implicit F: WeakAsync[F]): Sync[F] =
    new Sync[F] {
      override val applicative = new Applicative[F] {
        override def pure[A](x: A): F[A] = F.pure(x)
        override def ap[A, B](ff: F[A => B])(fa: F[A]): F[B] = F.flatMap(ff)(f => F.flatMap(fa)(a => F.pure(f(a))))
      }
      override def pure[A](x: A): F[A] = F.pure(x)
      override def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = F.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] = F.tailRecM(a)(f)
      override def raiseError[A](e: Throwable): F[A] = F.raiseError(e)
      override def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A] = F.handleErrorWith(fa)(f)
      override def monotonic: F[FiniteDuration] = F.monotonic
      override def realTime: F[FiniteDuration] = F.realTime
      override def suspend[A](hint: Sync.Type)(thunk: => A): F[A] = F.suspend(hint)(thunk)
    }
}

object WeakAsync extends WeakAsyncLowPriorityInstances {

  def apply[F[_]](implicit ev: WeakAsync[F]): WeakAsync[F] = ev

  implicit def doobieMonadCancelForWeakAsync[F[_]](implicit F: WeakAsync[F]): MonadCancel[F, Throwable] =
    new MonadCancel[F, Throwable] {
      override def pure[A](x: A): F[A] = F.pure(x)
      override def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = F.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] = F.tailRecM(a)(f)
      override def raiseError[A](e: Throwable): F[A] = F.raiseError(e)
      override def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A] = F.handleErrorWith(fa)(f)
      override def forceR[A, B](fa: F[A])(fb: F[B]): F[B] = F.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[F] => F[A]): F[A] = F.uncancelable(body)
      override def canceled: F[Unit] = F.canceled
      override def onCancel[A](fa: F[A], fin: F[Unit]): F[A] = F.onCancel(fa, fin)
    }
    

  implicit def doobieWeakAsyncForAsync[F[_]](implicit F: Async[F]): WeakAsync[F] = 
    new WeakAsync[F] {
      override def pure[A](x: A): F[A] = F.pure(x)
      override def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = F.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] = F.tailRecM(a)(f)
      override def raiseError[A](e: Throwable): F[A] = F.raiseError(e)
      override def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A] = F.handleErrorWith(fa)(f)
      override def monotonic: F[FiniteDuration] = F.monotonic
      override def realTime: F[FiniteDuration] = F.realTime
      override def suspend[A](hint: Sync.Type)(thunk: => A): F[A] = F.suspend(hint)(thunk)
      override def forceR[A, B](fa: F[A])(fb: F[B]): F[B] = F.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[F] => F[A]): F[A] = F.uncancelable(body)
      override def canceled: F[Unit] = F.canceled
      override def onCancel[A](fa: F[A], fin: F[Unit]): F[A] = F.onCancel(fa, fin)
      override def fromFuture[A](fut: F[Future[A]]): F[A] = F.fromFuture(fut)
    }

  /** Create a natural transformation for lifting an `Async` effect `F` into a `WeakAsync` effect `G`
    * `cats.effect.std.Dispatcher` the trasformation is based on is stateful and requires finalization.
    * Leaking it from it's resource scope will lead to erorrs at runtime. */
  def liftK[F[_], G[_]](implicit F: Async[F], G: WeakAsync[G]): Resource[F, F ~> G] =
    Dispatcher[F].map(dispatcher =>
      new(F ~> G) {
        def apply[T](fa: F[T]) = {
          G.delay(dispatcher.unsafeToFutureCancelable(fa)).flatMap { 
            case (running, cancel) =>
              G.onCancel(G.fromFuture(G.pure(running)), G.fromFuture(G.delay(cancel())))
          }
        }
      }
    )

}