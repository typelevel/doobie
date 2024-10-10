// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.{HList, HNil, ::, Generic, Lazy, OrElse}
import shapeless.labelled.{FieldType}

trait MkReadPlatform extends LowerPriorityMkRead {

  // Derivation base case for product types (1-element)
  implicit def productBase[H](
      implicit H: Read[H] OrElse MkRead[H]
  ): MkRead[H :: HNil] = {
    val headInstance = H.fold(identity, _.instance)

    new MkRead(
      Read.Composite(
        List(headInstance),
        _.head.asInstanceOf[H] :: HNil
      )
    )
  }

  // Derivation base case for shapeless record (1-element)
  implicit def recordBase[K <: Symbol, H](
      implicit H: Read[H] OrElse MkRead[H]
  ): MkRead[FieldType[K, H] :: HNil] = {
    val headInstance = H.fold(identity, _.instance)

    new MkRead(
      Read.Composite(
        List(headInstance),
        _.head.asInstanceOf[FieldType[K, H]] :: HNil
      )
    )
  }
}

trait LowerPriorityMkRead {

  // Derivation inductive case for product types
  implicit def product[H, T <: HList](
      implicit
      H: Read[H] OrElse MkRead[H],
      T: Read[T] OrElse MkRead[T]
  ): MkRead[H :: T] = {
    val headInstance = H.fold(identity, _.instance)
    val tailInstance = T.fold(identity, _.instance)

    new MkRead(
      Read.Composite(
        List(headInstance, tailInstance),
        list => list.head.asInstanceOf[H] :: list.tail.asInstanceOf[T]
      )
    )
  }

  // Derivation inductive case for shapeless records
  implicit def record[K <: Symbol, H, T <: HList](
      implicit
      H: Read[H] OrElse MkRead[H],
      T: Read[T] OrElse MkRead[T]
  ): MkRead[FieldType[K, H] :: T] = {
    val headInstance = H.fold(identity, _.instance)
    val tailInstance = T.fold(identity, _.instance)

    new MkRead(
      Read.Composite(
        List(headInstance, tailInstance),
        list => list.head.asInstanceOf[FieldType[K, H]] :: list.tail.asInstanceOf[T]
      )
    )
  }

  // FIXME:  remove lazyies
  // Derivation for product types (i.e. case class)
  implicit def generic[T, Repr](
      implicit
      gen: Generic.Aux[T, Repr],
      hlistRead: Lazy[Read[Repr] OrElse MkRead[Repr]]
  ): MkRead[T] = {
    val hlistInstance = hlistRead.value.fold(identity, _.instance)
    new MkRead(hlistInstance.map(gen.from))
  }

}
