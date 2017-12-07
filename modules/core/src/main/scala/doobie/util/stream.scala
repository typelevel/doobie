// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import fs2.Stream
import fs2.Stream.{ attemptEval, emits, empty, raiseError }

/** Additional functions for manipulating `Stream` values. */
object stream {

  /** Stream constructor for effectful source of chunks. */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def repeatEvalChunks[F[_], T](fa: F[Seq[T]]): Stream[F, T] =
    attemptEval(fa) flatMap {
      case Left(e)    => raiseError(e)
      case Right(seq) => if (seq.isEmpty) empty else (emits(seq) ++ repeatEvalChunks(fa))
    }

}
