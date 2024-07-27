// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.deriving.Mirror

trait ReadPlatform:
  // Generic Read for products.
  given derivedTuple[P <: Tuple, A](
      using
      m: Mirror.ProductOf[P],
      i: A =:= m.MirroredElemTypes,
      w: MkRead[A]
  ): MkRead[P] =
    MkRead.derived[P, A]

  // Generic Read for option of products.
  given derivedOptionTuple[P <: Tuple, A](
      using
      m: Mirror.ProductOf[P],
      i: A =:= m.MirroredElemTypes,
      w: MkRead[Option[A]]
  ): MkRead[Option[P]] =
    MkRead.derivedOption[P, A]
