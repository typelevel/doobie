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
      A: Lazy[Write[Repr]],
      isTuple: IsTuple[A]
  ): Write[A] = {
    val _ = isTuple
    implicit val hlistWrite: Lazy[Write[Repr] OrElse MkWrite[Repr]] = A.map(OrElse.primary(_))
    MkWrite.generic[A, Repr].instance
  }

  @deprecated("Use Write.derived instead to derive instances explicitly", "1.0.0-RC6")
  def generic[T, Repr <: HList](implicit
      gen: Generic.Aux[T, Repr],
      A: Write[Repr] OrElse MkWrite[Repr]
  ): Write[T] = {
    implicit val hlistWrite: Lazy[Write[Repr] OrElse MkWrite[Repr]] = A
    MkWrite.generic[T, Repr].instance
  }

  implicit def recordBase[K <: Symbol, H](
      implicit H: Write[H]
  ): Write[FieldType[K, H] :: HNil] = MkWrite.recordBase[K, H].instance

  implicit def productBase[H](
      implicit H: Write[H]
  ): Write[H :: HNil] = MkWrite.productBase[H].instance

}

trait LowerPriority1WritePlatform extends LowerPriority2WritePlatform {

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

trait LowerPriority2WritePlatform {
  implicit def fromDerived[A](implicit ev: MkWrite[A]): Write[A] = ev.instance
}
