// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.deriving.Mirror
import doobie.util.shapeless.OrElse

trait MkReadPlatform:

  // Generic Read for products.
  given derived[P <: Product, A](
      using
      m: Mirror.ProductOf[P],
      i: A =:= m.MirroredElemTypes,
      r: MkRead[A]
  ): MkRead[P] = {
    val read = r.instance.map(a => m.fromProduct(i(a)))
    new MkRead(read)
  }

  // Derivation base case for product types (1-element)
  given productBase[H](
      using H: Read[H] `OrElse` MkRead[H]
  ): MkRead[H *: EmptyTuple] = {
    val headInstance = H.fold(identity, _.instance)
    new MkRead(
      Read.Composite(
        List(headInstance),
        _.head.asInstanceOf[H] *: EmptyTuple
      )
    )
  }

  // Read for head and tail.
  given product[H, T <: Tuple](
      using
      H: Read[H] `OrElse` MkRead[H],
      T: Read[T] `OrElse` MkRead[T]
  ): MkRead[H *: T] = {
    val headInstance = H.fold(identity, _.instance)
    val tailInstance = T.fold(identity, _.instance)

    new MkRead(
      Read.Composite(
        List(headInstance, tailInstance),
        list => list(0).asInstanceOf[H] *: list(1).asInstanceOf[T]
      )
    )

  }
