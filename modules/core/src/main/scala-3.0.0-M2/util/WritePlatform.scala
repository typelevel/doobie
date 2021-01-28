// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.deriving.Mirror

trait WritePlatform:

  // Trivial write for empty tuple.
  given Write[EmptyTuple] =
    new Write(
      puts = Nil,
      toList = _ => Nil,
      unsafeSet = (_, _, _) => (),
      unsafeUpdate = (_, _, _) => (),
      unsafeSetOption = (_, _, _) => (),
      unsafeUpdateOption = (_, _, _) => (),
    )

  // Inductive write for writable head and tail.
  given [H, T <: Tuple](using H: => Write[H], T: => Write[T]) as Write[H *: T] =
    new Write(
      puts = H.puts ++ T.puts,
      toList = { case h *: t => H.toList(h) ++ T.toList(t) },
      unsafeSet = { case (ps, n, h *: t) =>
        H.unsafeSet(ps, n, h)
        T.unsafeSet(ps, n + H.length, t)
      },
      unsafeUpdate = { case (rs, n, h *: t) =>
        H.unsafeUpdate(rs, n, h);
        T.unsafeUpdate(rs, n + H.length, t)
      },
      unsafeSetOption = { case (ps, n, oht) =>
        H.unsafeSetOption(ps, n, oht.map(_.head))
        T.unsafeSetOption(ps, n + H.length, oht.map(_.tail))
      },
      unsafeUpdateOption = { case (rs, n, oht) =>
        H.unsafeUpdateOption(rs, n, oht.map(_.head))
        T.unsafeUpdateOption(rs, n + H.length, oht.map(_.tail))
      },
    )

  // Generic write for products.
  given [P <: Product, A](
    using m: Mirror.ProductOf[P],
          i: m.MirroredElemTypes =:= A,
          w: Write[A]
  ) as Write[P] =
    w.contramap(p => i(Tuple.fromProductTyped(p)))
