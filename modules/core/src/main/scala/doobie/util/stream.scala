// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import fs2._

/** Additional functions for manipulating `Stream` values. */
object stream {

  /** Stream constructor for effectful source of chunks. */
  def repeatEvalChunks[F[_], T](fa: F[Seq[T]]): Stream[F, T] = {
    def go: Pull[F, T, Unit] =
      Pull.eval(fa).flatMap { seq =>
        if (seq.isEmpty) Pull.done
        else Pull.output(Chunk.seq(seq)) >> go
      }

    go.stream
  }
}
