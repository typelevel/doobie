package doobie.enum

import doobie.util.invariant._

import java.sql.ParameterMetaData._

import scalaz.Equal
import scalaz.std.anyVal.intInstance

object parametermode {

  /** @group Implementation */
  sealed abstract class ParameterMode(val toInt: Int)

  /** @group Values */ case object ModeIn      extends ParameterMode(parameterModeIn)
  /** @group Values */ case object ModeOut     extends ParameterMode(parameterModeOut)
  /** @group Values */ case object ModeInOut   extends ParameterMode(parameterModeInOut)
  /** @group Values */ case object ModeUnknown extends ParameterMode(parameterModeUnknown)

  /** @group Implementation */
  object ParameterMode {

    def fromInt(n:Int): Option[ParameterMode] =
      Some(n) collect {
        case ModeIn.toInt      => ModeIn
        case ModeOut.toInt     => ModeOut
        case ModeInOut.toInt   => ModeInOut
        case ModeUnknown.toInt => ModeUnknown
      }

    def unsafeFromInt(n: Int): ParameterMode =
      fromInt(n).getOrElse(throw InvalidOrdinal[ParameterMode](n))

    implicit val EqualParameterMode: Equal[ParameterMode] =
      Equal.equalBy(_.toInt)

  }

}
