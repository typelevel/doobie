// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.deriving.Mirror
import scala.compiletime.constValue
import scala.reflect.Enum

trait GetPlatform {
  private def of[A](name: String, cases: List[A], labels: List[String]): Get[A] =
    Get[String].temap { caseName =>
      labels.indexOf(caseName) match {
        case -1 => Left(s"enum $name does not contain case: $caseName")
        case i  => Right(cases(i))
      }
    }

  inline final def deriveEnumString[A <: Enum](using mirror: Mirror.SumOf[A]): Get[A] =
    of(
      constValue[mirror.MirroredLabel],
      summonSingletonCases[mirror.MirroredElemTypes, A](constValue[mirror.MirroredLabel]),
      summonLabels[mirror.MirroredElemLabels]
    )
}
