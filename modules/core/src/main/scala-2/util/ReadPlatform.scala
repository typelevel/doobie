// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.{ HList, HNil, ::, Generic, Lazy}
import shapeless.labelled.{ field, FieldType }

trait ReadPlatform extends LowerPriorityRead { this: Read.type =>

  implicit def recordRead[K <: Symbol, H, T <: HList](
    implicit H: Lazy[Read[H]],
              T: Lazy[Read[T]]
  ): Read[FieldType[K, H] :: T] =
    new Read[FieldType[K, H] :: T](
      H.value.gets ++ T.value.gets,
      (rs, n) => field[K](H.value.unsafeGet(rs, n)) :: T.value.unsafeGet(rs, n + H.value.length),
      (rs, n) =>
        for {
          h <- H.value.unsafeGetOption(rs, n)
          t <- T.value.unsafeGetOption(rs, n + H.value.length)
        } yield field[K](h) :: t
    )
}

trait LowerPriorityRead { this: Read.type =>

  implicit def product[H, T <: HList](implicit H: Lazy[Read[H]], T: Lazy[Read[T]]): Read[H :: T] =
    new Read[H :: T](
      H.value.gets ++ T.value.gets,
      (rs, n) => H.value.unsafeGet(rs, n) :: T.value.unsafeGet(rs, n + H.value.length),
      (rs, n) =>
        for {
          h <- H.value.unsafeGetOption(rs, n)
          t <- T.value.unsafeGetOption(rs, n + H.value.length)
        } yield h :: t
    )

  implicit val emptyProduct: Read[HNil] =
    new Read[HNil](Nil, (_, _) => HNil, (_, _) => Option(HNil))

  implicit def generic[F, G](implicit gen: Generic.Aux[F, G], G: Lazy[Read[G]]): Read[F] =
    new Read[F](
      G.value.gets,
      (rs, n) => gen.from(G.value.unsafeGet(rs, n)),
      (rs, n) => G.value.unsafeGetOption(rs, n).map(gen.from),
    )
}
