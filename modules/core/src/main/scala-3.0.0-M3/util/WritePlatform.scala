// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.deriving.Mirror

trait WritePlatform:

  // Trivial write for empty tuple.
  given Write[EmptyTuple] =
    new Write(Nil, _ => Nil, (_, _, _) => (),(_, _, _) => ())

  // Inductive write for writable head and tail.
  given [H, T <: Tuple](using H: => Write[H], T: => Write[T]): Write[H *: T] =
    new Write(
      H.puts ++ T.puts,
      { case h *: t => H.toList(h) ++ T.toList(t) },
      { case (ps, n, h *: t) => H.unsafeSet(ps, n, h); T.unsafeSet(ps, n + H.length, t) },
      { case (rs, n, h *: t) => H.unsafeUpdate(rs, n, h); T.unsafeUpdate(rs, n + H.length, t) }
    )

  // Generic write for products.
  given [P <: Product, A](
    using m: Mirror.ProductOf[P],
          i: m.MirroredElemTypes =:= A,
          w: Write[A]
  ): Write[P] =
    w.contramap(p => i(Tuple.fromProductTyped(p)))
