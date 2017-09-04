// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.enum

import doobie.util.invariant._

import java.sql.ParameterMetaData._

import cats.kernel.Eq
import cats.kernel.instances.int._

/** @group Implementation */
sealed abstract class ParameterMode(val toInt: Int)

/** @group Implementation */
object ParameterMode {

  /** @group Values */ case object ModeIn      extends ParameterMode(parameterModeIn)
  /** @group Values */ case object ModeOut     extends ParameterMode(parameterModeOut)
  /** @group Values */ case object ModeInOut   extends ParameterMode(parameterModeInOut)
  /** @group Values */ case object ModeUnknown extends ParameterMode(parameterModeUnknown)

  def fromInt(n:Int): Option[ParameterMode] =
    Some(n) collect {
      case ModeIn.toInt      => ModeIn
      case ModeOut.toInt     => ModeOut
      case ModeInOut.toInt   => ModeInOut
      case ModeUnknown.toInt => ModeUnknown
    }

  def unsafeFromInt(n: Int): ParameterMode =
    fromInt(n).getOrElse(throw InvalidOrdinal[ParameterMode](n))

  implicit val EqParameterMode: Eq[ParameterMode] =
    Eq.by(_.toInt)
}
