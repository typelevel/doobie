// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.*
import shapeless.labelled.FieldType

trait WritePlatform extends LowerPriority1WritePlatform {

  implicit def genericTuple[A, Repr <: HList](
      implicit
      gen: Generic.Aux[A, Repr],
      G: Lazy[Write[Repr]],
      isTuple: IsTuple[A]
  ): Write[A] = {
    val _ = isTuple
    implicit val hlistWrite: Lazy[Write[Repr] OrElse Derived[MkWrite[Repr]]] = G.map(OrElse.primary(_))
    MkWrite.genericWrite[A, Repr].instance
  }

  @deprecated("Write.generic has been renamed to Write.derived to align with Scala 3 derivation", "1.0.0-RC6")
  def generic[T, Repr <: HList](implicit
      gen: Generic.Aux[T, Repr],
      A: Write[Repr] OrElse Derived[MkWrite[Repr]]
  ): Write[T] = {
    implicit val hlistWrite: Lazy[Write[Repr] OrElse Derived[MkWrite[Repr]]] = A
    MkWrite.genericWrite[T, Repr].instance
  }

  implicit def recordBase[K <: Symbol, H](
      implicit H: Write[H]
  ): Write[FieldType[K, H] :: HNil] = MkWrite.recordBase[K, H].instance

  implicit def productBase[H](
      implicit H: Write[H]
  ): Write[H :: HNil] = MkWrite.productBase[H].instance

}

trait LowerPriority1WritePlatform extends LowestPriorityWrite {

  implicit def product[H, T <: HList](
      implicit
      H: Write[H],
      T: Write[T]
  ): Write[H :: T] = MkWrite.product[H, T].instance

  implicit def record[K <: Symbol, H, T <: HList](
      implicit
      H: Write[H],
      T: Write[T]
  ): Write[FieldType[K, H] :: T] = MkWrite.record[K, H, T].instance

}
