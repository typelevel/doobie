// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.compiletime.constValue
import scala.deriving.Mirror
import scala.reflect.Enum

trait PutPlatform {
  inline final def deriveEnumString[A <: Enum](using mirror: Mirror.SumOf[A]): Put[A] =
    val _ = summonSingletonCases[mirror.MirroredElemTypes, A](constValue[mirror.MirroredLabel])
    val labels = summonLabels[mirror.MirroredElemLabels]

    Put[String].contramap(a => labels(mirror.ordinal(a)))
}
