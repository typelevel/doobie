// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.deriving.Mirror

trait ReadPlatform extends LowestPriorityRead:
  trait Auto {
    implicit final inline def autoDerivedRead[A <: Product](using
        inline mirror: Mirror.ProductOf[A]
    ): Derived[Read[A]] =
      Derived(ReadDerivation.derived[A])
  }

  inline def derived[A <: Product](using
      inline mirror: Mirror.ProductOf[A]
  ): Read[A] =
    ReadDerivation.derived[A]

  given tupleBase[H](
      using H: Read[H]
  ): Read[H *: EmptyTuple] =
    H.map(h => h *: EmptyTuple)

  given tuple[H, T <: Tuple](
      using
      H: Read[H],
      T: Read[T]
  ): Read[H *: T] =
    Read.Composite(H, T, (h, t) => h *: t)

  given optionalFromRead[A](using read: Read[A]): Read[Option[A]] = read.toOpt

  given fromGet[A](using get: Get[A]): Read[A] = new Read.Single(get)

trait LowestPriorityRead {
  implicit def fromDerived[A](implicit ev: Derived[Read[A]]): Read[A] = ev.instance
}
