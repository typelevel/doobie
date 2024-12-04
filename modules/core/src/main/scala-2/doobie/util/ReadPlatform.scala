// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.{Generic, HList, IsTuple, Lazy}

trait ReadPlatform {

  // Derivation for product types (i.e. case class)
  implicit def genericTuple[A, Repr <: HList](implicit
      gen: Generic.Aux[A, Repr],
      G: Lazy[MkRead[Repr]],
      isTuple: IsTuple[A]
  ): MkRead[A] = {
    val _ = isTuple
    MkRead.generic[A, Repr]
  }

  // Derivation for optional of product types (i.e. case class)
  implicit def ogenericTuple[A, Repr <: HList](
      implicit
      G: Generic.Aux[A, Repr],
      B: Lazy[MkRead[Option[Repr]]],
      isTuple: IsTuple[A]
  ): MkRead[Option[A]] = {
    val _ = isTuple
    MkRead.ogeneric[A, Repr]
  }

  @deprecated("Use Read.derived instead to derive instances explicitly", "1.0.0-RC6")
  def generic[T, Repr](implicit gen: Generic.Aux[T, Repr], G: Lazy[MkRead[Repr]]): MkRead[T] =
    MkRead.generic[T, Repr]
}
