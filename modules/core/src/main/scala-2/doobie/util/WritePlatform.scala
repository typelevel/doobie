// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.{Generic, HList, IsTuple, Lazy}

trait WritePlatform {

  implicit def genericTuple[A, Repr](
      implicit
      gen: Generic.Aux[A, Repr],
      A: Lazy[MkWrite[Repr]],
      isTuple: IsTuple[A]
  ): MkWrite[A] = {
    val _ = isTuple
    MkWrite.generic[A, Repr]
  }

  implicit def ogenericTuple[A, Repr <: HList](
      implicit
      G: Generic.Aux[A, Repr],
      A: Lazy[MkWrite[Option[Repr]]],
      isTuple: IsTuple[A]
  ): MkWrite[Option[A]] = {
    val _ = isTuple
    MkWrite.ogeneric[A, Repr]
  }

  @deprecated("Use Write.derived instead to derive instances explicitly", "1.0.0-RC6")
  def generic[T, Repr](implicit gen: Generic.Aux[T, Repr], A: Lazy[MkWrite[Repr]]): MkWrite[T] =
    MkWrite.generic[T, Repr]
}
