// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.deriving.Mirror
import doobie.util.shapeless.OrElse
import scala.util.NotGiven

trait MkReadPlatform:

  // Derivation for product types (i.e. case class)
  implicit def derived[P <: Product, A](
      implicit
      m: Mirror.ProductOf[P],
      i: A =:= m.MirroredElemTypes,
      r: Read[A] `OrElse` Derived[MkRead[A]],
      isNotCaseObj: NotGiven[m.MirroredElemTypes =:= EmptyTuple]
  ): Derived[MkRead[P]] = {
    val _ = isNotCaseObj
    val read = r.fold(identity, _.instance).map(a => m.fromProduct(i(a)))
    new Derived(new MkRead(read))
  }

  // Derivation base case for tuple (1-element)
  implicit def productBase[H](
      implicit H: Read[H] `OrElse` Derived[MkRead[H]]
  ): Derived[MkRead[H *: EmptyTuple]] = {
    val headInstance = H.fold(identity, _.instance)
    new Derived(
      new MkRead(
        Read.Transform(
          headInstance,
          h => h *: EmptyTuple
        )
      ))
  }

  // Derivation inductive case for tuples
  implicit def product[H, T <: Tuple](
      implicit
      H: Read[H] `OrElse` Derived[MkRead[H]],
      T: Read[T] `OrElse` Derived[MkRead[T]]
  ): Derived[MkRead[H *: T]] = {
    val headInstance = H.fold(identity, _.instance)
    val tailInstance = T.fold(identity, _.instance)

    new Derived(
      new MkRead(
        Read.Composite(
          headInstance,
          tailInstance,
          (h, t) => h *: t
        )
      )
    )

  }
