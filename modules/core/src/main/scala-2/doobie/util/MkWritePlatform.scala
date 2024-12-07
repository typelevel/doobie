// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.{::, Generic, HList, HNil, Lazy, OrElse}
import shapeless.labelled.FieldType

trait MkWritePlatform extends LowerPriorityMkWrite {

  // Derivation base case for product types (1-element)
  implicit def productBase[H](
      implicit H: Write[H] OrElse Derived[MkWrite[H]]
  ): Derived[MkWrite[H :: HNil]] = {
    val head = H.fold(identity, _.instance)

    new Derived(
      new MkWrite[H :: HNil](
        new Write.Composite(List(head), { case h :: HNil => List(h) })
      )
    )
  }

  // Derivation base case for shapelss record (1-element)
  implicit def recordBase[K <: Symbol, H](
      implicit H: Write[H] OrElse Derived[MkWrite[H]]
  ): Derived[MkWrite[FieldType[K, H] :: HNil]] = {
    val head = H.fold(identity, _.instance)

    new Derived(
      new MkWrite(
        new Write.Composite(List(head), { case h :: HNil => List(h) })
      )
    )
  }

}

trait LowerPriorityMkWrite {

  // Derivation inductive case for product types
  implicit def product[H, T <: HList](
      implicit
      H: Write[H] OrElse Derived[MkWrite[H]],
      T: Write[T] OrElse Derived[MkWrite[T]]
  ): Derived[MkWrite[H :: T]] = {
    val head = H.fold(identity, _.instance)
    val tail = T.fold(identity, _.instance)

    new Derived(
      new MkWrite[H :: T](
        new Write.Composite(
          List(head, tail),
          { case h :: t => List(h, t) }
        )
      )
    )
  }

  // Derivation inductive case for shapeless records
  implicit def record[K <: Symbol, H, T <: HList](
      implicit
      H: Write[H] OrElse Derived[MkWrite[H]],
      T: Write[T] OrElse Derived[MkWrite[T]]
  ): Derived[MkWrite[FieldType[K, H] :: T]] = {
    val head = H.fold(identity, _.instance)
    val tail = T.fold(identity, _.instance)

    new Derived(
      new MkWrite(
        new Write.Composite(
          List(head, tail),
          {
            case h :: t => List(h, t)
          }
        )
      )
    )
  }

  // Derivation for product types (i.e. case class)
  implicit def genericWrite[A, Repr <: HList](
      implicit
      gen: Generic.Aux[A, Repr],
      hlistWrite: Lazy[Write[Repr] OrElse Derived[MkWrite[Repr]]]
  ): Derived[MkWrite[A]] = {
    val g = hlistWrite.value.fold(identity, _.instance)

    new Derived(
      new MkWrite[A](
        new Write.Composite(List(g), a => List(gen.to(a)))
      )
    )
  }

}
