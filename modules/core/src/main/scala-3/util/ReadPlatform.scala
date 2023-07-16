// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.deriving.Mirror

trait ReadPlatform:

  // Trivial Read for EmptyTuple
  given Read[EmptyTuple] =
    new Read[EmptyTuple](Nil, (_, _) => EmptyTuple)

  // Read for head and tail.
  given [H, T <: Tuple](using H: Read[H], T: Read[T]): Read[H *: T] =
    new Read[H *: T](
      H.gets ++ T.gets,
      (rs, n) => H.unsafeGet(rs, n) *: T.unsafeGet(rs, n + H.length)
    )

  // Generic Read for products.
  given derived [P <: Product, A](
    using m: Mirror.ProductOf[P],
          i: A =:= m.MirroredElemTypes,
          w: Read[A]
  ): Read[P] =
    w.map(a => m.fromProduct(i(a)))

  given roe: Read[Option[EmptyTuple]] =
    new Read[Option[EmptyTuple]](Nil, (_, _) => Some(EmptyTuple))

  given rou: Read[Option[Unit]] =
    new Read[Option[Unit]](Nil, (_, _) => Some(()))

  given cons1[H, T <: Tuple](
    using H: => Read[Option[H]],
          T: => Read[Option[T]],
  ): Read[Option[H *: T]] =
    new Read[Option[H *: T]](
      H.gets ++ T.gets,
      (rs, n) =>
        for {
          h <- H.unsafeGet(rs, n)
          t <- T.unsafeGet(rs, n + H.length)
        } yield h *: t
    )

  given cons2[H, T <: Tuple](
    using H: => Read[Option[H]],
          T: => Read[Option[T]]
  ): Read[Option[Option[H] *: T]] =
    new Read[Option[Option[H] *: T]](
      H.gets ++ T.gets,
      (rs, n) => T.unsafeGet(rs, n + H.length).map(H.unsafeGet(rs, n) *: _)
    )

  // Generic Read for option of products.
  given [P <: Product, A](
    using m: Mirror.ProductOf[P],
          i: A =:= m.MirroredElemTypes,
          w: Read[Option[A]]
  ): Read[Option[P]] =
    w.map(a => a.map(a => m.fromProduct(i(a))))
