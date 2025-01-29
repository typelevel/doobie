// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.{HList, HNil, ::, Generic, Lazy, OrElse}
import shapeless.labelled.FieldType

trait MkReadPlatform extends LowerPriorityMkRead {

  // Derivation base case for product types (1-element)
  implicit def productBase[H](
      implicit H: Read[H] OrElse Derived[MkRead[H]]
  ): Derived[MkRead[H :: HNil]] = {
    val headInstance = H.fold(identity, _.instance)

    new Derived(
      new MkRead(
        headInstance.map(_ :: HNil)
      )
    )
  }

  // Derivation base case for shapeless record (1-element)
  implicit def recordBase[K <: Symbol, H](
      implicit H: Read[H] OrElse Derived[MkRead[H]]
  ): Derived[MkRead[FieldType[K, H] :: HNil]] = {
    val headInstance = H.fold(identity, _.instance)

    new Derived(
      new MkRead(
        new Read.Transform[FieldType[K, H] :: HNil, H](
          headInstance,
          h => shapeless.labelled.field[K].apply(h) :: HNil
        )
      )
    )
  }
}

trait LowerPriorityMkRead {

  // Derivation inductive case for product types
  implicit def product[H, T <: HList](
      implicit
      H: Read[H] OrElse Derived[MkRead[H]],
      T: Read[T] OrElse Derived[MkRead[T]]
  ): Derived[MkRead[H :: T]] = {
    val headInstance = H.fold(identity, _.instance)
    val tailInstance = T.fold(identity, _.instance)

    new Derived(
      new MkRead(
        new Read.Composite[H :: T, H, T](
          headInstance,
          tailInstance,
          (h, t) => h :: t
        )
      )
    )
  }

  // Derivation inductive case for shapeless records
  implicit def record[K <: Symbol, H, T <: HList](
      implicit
      H: Read[H] OrElse Derived[MkRead[H]],
      T: Read[T] OrElse Derived[MkRead[T]]
  ): Derived[MkRead[FieldType[K, H] :: T]] = {
    val headInstance = H.fold(identity, _.instance)
    val tailInstance = T.fold(identity, _.instance)

    new Derived(
      new MkRead(
        new Read.Composite[FieldType[K, H] :: T, H, T](
          headInstance,
          tailInstance,
          (h, t) => shapeless.labelled.field[K].apply(h) :: t
        )
      )
    )
  }

  // Derivation for product types (i.e. case class)
  implicit def genericRead[T, Repr](
      implicit
      gen: Generic.Aux[T, Repr],
      hlistRead: Lazy[Read[Repr] OrElse Derived[MkRead[Repr]]]
  ): Derived[MkRead[T]] = {
    val hlistInstance: Read[Repr] = hlistRead.value.fold(identity, _.instance)
    new Derived(new MkRead(hlistInstance.map(gen.from)))
  }

}
