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
  given derived[P <: Product, A](
    using m: Mirror.ProductOf[P],
          i: m.MirroredElemTypes =:= A,
          w: Write[A]
  ): Write[P] =
    w.contramap(p => i(Tuple.fromProductTyped(p)))

  // Trivial write for option of empty tuple.
  given woe: Write[Option[EmptyTuple]] =
    new Write[Option[EmptyTuple]](Nil, _ => Nil, (_, _, _) => (), (_, _, _) => ())

  // Trivial write for option of Unit.
  given wou: Write[Option[Unit]] =
    new Write[Option[Unit]](Nil, _ => Nil, (_, _, _) => (), (_, _, _) => ())

  // Write[Option[H]], Write[Option[T]] implies Write[Option[H *: T]]
  given cons1[H, T <: Tuple](
    using H: => Write[Option[H]],
          T: => Write[Option[T]],
          // N: H <:!< Option[_],
  ): Write[Option[H *: T]] =

    def split[A](i: Option[H *: T])(f: (Option[H], Option[T]) => A): A =
      i.fold(f(None, None)) { case h *: t => f(Some(h), Some(t)) }

    new Write(
      H.puts ++ T.puts,
      split(_) { (h, t) => H.toList(h) ++ T.toList(t) },
      (ps, n, i) => split(i) { (h, t) => H.unsafeSet(ps, n, h); T.unsafeSet(ps, n + H.length, t) },
      (rs, n, i) => split(i) { (h, t) => H.unsafeUpdate(rs, n, h); T.unsafeUpdate(rs, n + H.length, t) }
    )

  // Write[Option[H]], Write[Option[T]] implies Write[Option[Option[H] *: T]]
  given cons2[H, T <: Tuple](
    using H: => Write[Option[H]],
          T: => Write[Option[T]]
  ): Write[Option[Option[H] *: T]] =

    def split[A](i: Option[Option[H] *: T])(f: (Option[H], Option[T]) => A): A =
      i.fold(f(None, None)) { case oh *: t => f(oh, Some(t)) }

    new Write(
      H.puts ++ T.puts,
      split(_) { (h, t) => H.toList(h) ++ T.toList(t) },
      (ps, n, i) => split(i) { (h, t) => H.unsafeSet(ps, n, h); T.unsafeSet(ps, n + H.length, t) },
      (rs, n, i) => split(i) { (h, t) => H.unsafeUpdate(rs, n, h); T.unsafeUpdate(rs, n + H.length, t) }
    )

  // Generic write for options of products.
  given [P <: Product, A](
    using m: Mirror.ProductOf[P],
          i: m.MirroredElemTypes =:= A,
          w: Write[Option[A]]
  ): Write[Option[P]] =
    w.contramap(op => op.map(p => i(Tuple.fromProductTyped(p))))
