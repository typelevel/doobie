// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.deriving.Mirror

trait ReadPlatform extends LowerPriority1ReadPlatform:
  // Generic Read for products.
  given derivedTuple[P <: Tuple, A](
      using
      m: Mirror.ProductOf[P],
      i: A =:= m.MirroredElemTypes,
      w: MkRead[A]
  ): MkRead[P] =
    MkRead.derived[P, A]

trait LowerPriority1ReadPlatform:
  implicit def fromDerived[A](implicit ev: MkRead[A]): Read[A] = ev.instance
