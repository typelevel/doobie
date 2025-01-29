// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.deriving.Mirror
import doobie.util.shapeless.OrElse
import scala.util.NotGiven

trait MkWritePlatform:

  // Derivation for product types (i.e. case class)
  implicit def derived[P <: Product, A](
      implicit
      m: Mirror.ProductOf[P],
      i: m.MirroredElemTypes =:= A,
      w: Write[A] `OrElse` Derived[MkWrite[A]],
      isNotCaseObj: NotGiven[m.MirroredElemTypes =:= EmptyTuple]
  ): Derived[MkWrite[P]] =
    val _ = isNotCaseObj
    val write: Write[P] = w.fold(identity, _.instance).contramap(p => i(Tuple.fromProductTyped(p)))
    new Derived(
      new MkWrite(write)
    )

  // Derivation base case for tuple (1-element)
  implicit def productBase[H](
      implicit H: Write[H] `OrElse` Derived[MkWrite[H]]
  ): Derived[MkWrite[H *: EmptyTuple]] = {
    val headInstance = H.fold(identity, _.instance)
    new Derived(
      new MkWrite(Write.Composite(
        List(headInstance),
        { case h *: EmptyTuple => List(h) }
      ))
    )
  }

  // Derivation inductive case for tuples
  implicit def product[H, T <: Tuple](
      implicit
      H: Write[H] `OrElse` Derived[MkWrite[H]],
      T: Write[T] `OrElse` Derived[MkWrite[T]]
  ): Derived[MkWrite[H *: T]] = {
    val headWrite = H.fold(identity, _.instance)
    val tailWrite = T.fold(identity, _.instance)

    new Derived(
      new MkWrite(Write.Composite(
        List(headWrite, tailWrite),
        { case h *: t => List(h, t) }
      ))
    )
  }
