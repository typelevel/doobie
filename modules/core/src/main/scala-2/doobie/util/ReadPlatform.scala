// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.{Generic, HList, IsTuple, Lazy}

trait ReadPlatform extends LowerPriorityReadPlatform {

  // Derivation for product types (i.e. case class)
  implicit def genericTuple[A, Repr <: HList](implicit
      gen: Generic.Aux[A, Repr],
      G: Lazy[Read[Repr]],
      isTuple: IsTuple[A]
  ): Read[A] = {
    val _ = isTuple
    implicit val r: Read[Repr] = G.value
    MkRead.generic[A, Repr].instance
  }

  @deprecated("Use Read.derived instead to derive instances explicitly", "1.0.0-RC6")
  def generic[T, Repr](
      implicit
      gen: Generic.Aux[T, Repr],
      G: Lazy[MkRead[Repr]]
  ): MkRead[T] =
    MkRead.generic[T, Repr]
}

trait LowerPriorityReadPlatform {
  implicit def fromDerived[A](implicit ev: MkRead[A]): Read[A] = ev.instance
}
