// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.{ HList, HNil, ::, Generic, Lazy, <:!< }
import shapeless.labelled.{ field, FieldType }

trait ReadPlatform extends LowerPriorityRead { this: Read.type =>

  implicit def recordRead[K <: Symbol, H, T <: HList](
    implicit H: Lazy[Read[H]],
              T: Lazy[Read[T]]
  ): Read[FieldType[K, H] :: T] =
    new Read[FieldType[K, H] :: T](
      H.value.gets ++ T.value.gets,
      (rs, n) => field[K](H.value.unsafeGet(rs, n)) :: T.value.unsafeGet(rs, n + H.value.length)
    )

}

trait LowerPriorityRead extends EvenLower { this: Read.type =>

  implicit def product[H, T <: HList](
    implicit H: Lazy[Read[H]],
              T: Lazy[Read[T]]
  ): Read[H :: T] =
    new Read[H :: T](
      H.value.gets ++ T.value.gets,
      (rs, n) => H.value.unsafeGet(rs, n) :: T.value.unsafeGet(rs, n + H.value.length)
    )

  implicit def emptyProduct: Read[HNil] =
    new Read[HNil](Nil, (_, _) => HNil)

  implicit def generic[F, G](implicit gen: Generic.Aux[F, G], G: Lazy[Read[G]]): Read[F] =
    new Read[F](G.value.gets, (rs, n) => gen.from(G.value.unsafeGet(rs, n)))

}

trait EvenLower {

  implicit val ohnil: Read[Option[HNil]] =
    new Read[Option[HNil]](Nil, (_, _) => Some(HNil))

  implicit def ohcons1[H, T <: HList](
    implicit H: Lazy[Read[Option[H]]],
              T: Lazy[Read[Option[T]]],
              N: H <:!< Option[α] forSome { type α }
  ): Read[Option[H :: T]] = {
    void(N)
    new Read[Option[H :: T]](
      H.value.gets ++ T.value.gets,
      (rs, n) =>
        for {
          h <- H.value.unsafeGet(rs, n)
          t <- T.value.unsafeGet(rs, n + H.value.length)
        } yield h :: t
    )
  }

  implicit def ohcons2[H, T <: HList](
    implicit H: Lazy[Read[Option[H]]],
              T: Lazy[Read[Option[T]]]
  ): Read[Option[Option[H] :: T]] =
    new Read[Option[Option[H] :: T]](
      H.value.gets ++ T.value.gets,
      (rs, n) => T.value.unsafeGet(rs, n + H.value.length).map(H.value.unsafeGet(rs, n) :: _)
    )

  implicit def ogeneric[A, Repr <: HList](
    implicit G: Generic.Aux[A, Repr],
              B: Lazy[Read[Option[Repr]]]
  ): Read[Option[A]] =
    new Read[Option[A]](B.value.gets, B.value.unsafeGet(_, _).map(G.from))

}

