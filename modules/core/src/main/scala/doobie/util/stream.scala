// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import fs2.Stream
import cats.~>
import cats.effect.unsafe.UnsafeRun
import doobie.ConnectionIO
import doobie.FC

/** Additional functions for manipulating `Stream` values. */
object stream {

  /** Stream constructor for effectful source of chunks. */
  def repeatEvalChunks[F[_], T](fa: F[Seq[T]]): Stream[F, T] =
    Stream.repeatEval(fa).takeWhile(_.nonEmpty).flatMap(Stream.emits(_))

  /** Lift an effect into ConnectionIO */
  def toConnectionIO[F[_]](implicit F: UnsafeRun[F]) = λ[F ~> ConnectionIO] { fa =>
    FC.delay(F.unsafeRunFutureCancelable(fa)).flatMap { case (running, cancel) =>
      FC.onCancel(FC.fromFuture(FC.pure(running)), FC.fromFuture(FC.delay(cancel())))
    }
  }
}
