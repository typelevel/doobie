// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.deriving.Mirror

trait ReadPlatform:

  // Trivial Read for EmptyTuple
  given Read[EmptyTuple] =
    new Read[EmptyTuple](Nil, (_, _) => EmptyTuple, (_, _) => Some(EmptyTuple))

  // Read for head and tail.
  given [H, T <: Tuple](using H: => Read[H], T: => Read[T]): Read[H *: T] =
    new Read[H *: T](
      H.gets ++ T.gets,
      (rs, n) => H.unsafeGet(rs, n) *: T.unsafeGet(rs, n + H.length),
      (rs, n) =>
        for {
          h <- H.unsafeGetOption(rs, n)
          t <- T.unsafeGetOption(rs, n + H.length)
        } yield h *: t
    )

  // Generic Read for products.
  given [P <: Product, A](
    using m: Mirror.ProductOf[P],
          i: A =:= m.MirroredElemTypes,
          w: Read[A]
  ): Read[P] =
    w.map(a => m.fromProduct(i(a)))

