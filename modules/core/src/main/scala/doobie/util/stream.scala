// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.Applicative
import fs2.{ Stream, Sink }
import fs2.Stream.{ attemptEval, fail, emits, empty }

/** Additional functions for manipulating `Stream` values. */
object stream {

  /** Generalized `sink` constructor. */
  def sink[F[_]: Applicative, A](f: A => F[Unit]): Sink[F, A] =
    _.flatMap(a => Stream.eval(f(a)))

  /** Stream constructor for effectful source of chunks. */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def repeatEvalChunks[F[_], T](fa: F[Seq[T]]): Stream[F, T] =
    attemptEval(fa) flatMap {
      case Left(e)    => fail(e)
      case Right(seq) => if (seq.isEmpty) empty else (emits(seq) ++ repeatEvalChunks(fa))
    }

}
