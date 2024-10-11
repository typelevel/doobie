// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.labelled.FieldType
import shapeless.{Generic, HList, IsTuple, Lazy, OrElse}
import shapeless.{::, HNil}

trait ReadPlatform extends LowerPriority1ReadPlatform {

  // Derivation for product types (i.e. case class)
  implicit def genericTuple[A, Repr <: HList](implicit
      gen: Generic.Aux[A, Repr],
      G: Lazy[Read[Repr]],
      isTuple: IsTuple[A]
  ): Read[A] = {
    val _ = isTuple
    implicit val r: Lazy[Read[Repr] OrElse MkRead[Repr]] = G.map(OrElse.primary(_))
    MkRead.generic[A, Repr].instance
  }

  @deprecated("Use Read.derived instead to derive instances explicitly", "1.0.0-RC6")
  def generic[T, Repr](
      implicit
      gen: Generic.Aux[T, Repr],
      G: Lazy[MkRead[Repr]]
  ): MkRead[T] =
    MkRead.generic[T, Repr]

  implicit def recordBase[K <: Symbol, H](
      implicit H: Read[H]
  ): Read[FieldType[K, H] :: HNil] = MkRead.recordBase[K, H].instance

  implicit def productBase[H](
      implicit H: Read[H]
  ): Read[H :: HNil] = MkRead.productBase[H].instance
}

trait LowerPriority1ReadPlatform extends LowerPriority2ReadPlatform {
  implicit def product[H, T <: HList](
      implicit
      H: Read[H],
      T: Read[T]
  ): Read[H :: T] = MkRead.product[H, T].instance

  implicit def record[K <: Symbol, H, T <: HList](
      implicit
      H: Read[H],
      T: Read[T]
  ): Read[FieldType[K, H] :: T] = MkRead.record[K, H, T].instance
}

trait LowerPriority2ReadPlatform {
  implicit def fromDerived[A](implicit ev: MkRead[A]): Read[A] = ev.instance
}
