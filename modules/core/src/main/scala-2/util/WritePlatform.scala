// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.{ HList, HNil, ::, Generic, Lazy}
import shapeless.labelled.{ FieldType }

trait WritePlatform extends LowerPriorityWrite {

  implicit def recordWrite[K <: Symbol, H, T <: HList](
    implicit H: Lazy[Write[H]],
              T: Lazy[Write[T]]
  ): Write[FieldType[K, H] :: T] = {
    new Write(
      H.value.puts ++ T.value.puts,
      { case h :: t => H.value.toList(h) ++ T.value.toList(t) },
      { case (ps, n, h :: t) => H.value.unsafeSet(ps, n, h); T.value.unsafeSet(ps, n + H.value.length, t) },
      { case (rs, n, h :: t) => H.value.unsafeUpdate(rs, n, h); T.value.unsafeUpdate(rs, n + H.value.length, t) }
    )
  }

}

trait LowerPriorityWrite {

  implicit def product[H, T <: HList](
    implicit H: Lazy[Write[H]],
              T: Lazy[Write[T]]
  ): Write[H :: T] =
    new Write(
      H.value.puts ++ T.value.puts,
      { case h :: t => H.value.toList(h) ++ T.value.toList(t) },
      { case (ps, n, h :: t) => H.value.unsafeSet(ps, n, h); T.value.unsafeSet(ps, n + H.value.length, t) },
      { case (rs, n, h :: t) => H.value.unsafeUpdate(rs, n, h); T.value.unsafeUpdate(rs, n + H.value.length, t) }
    )

  implicit def emptyProduct: Write[HNil] =
    new Write[HNil](Nil, _ => Nil, (_, _, _) => (), (_, _, _) => ())

  implicit def generic[B, A](implicit gen: Generic.Aux[B, A], A: Lazy[Write[A]]): Write[B] =
    new Write[B](
      A.value.puts,
      b => A.value.toList(gen.to(b)),
      (ps, n, b) => A.value.unsafeSet(ps, n, gen.to(b)),
      (rs, n, b) => A.value.unsafeUpdate(rs, n, gen.to(b))
    )

}

