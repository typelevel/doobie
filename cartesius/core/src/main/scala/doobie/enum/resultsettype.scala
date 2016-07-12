package doobie.enum

import doobie.util.invariant._

import java.sql.ResultSet._

#+scalaz
import scalaz.Equal
import scalaz.std.anyVal.intInstance
#-scalaz
#+cats
import cats.kernel.Eq
import cats.kernel.std.int._
#-cats

object resultsettype {

  /** @group Implementation */
  sealed abstract class ResultSetType(val toInt: Int)

  /** @group Values */ case object TypeForwardOnly       extends ResultSetType(TYPE_FORWARD_ONLY)
  /** @group Values */ case object TypeScrollInsensitive extends ResultSetType(TYPE_SCROLL_INSENSITIVE)
  /** @group Values */ case object TypeScrollSensitive   extends ResultSetType(TYPE_SCROLL_SENSITIVE)

  /** @group Implementation */
  object ResultSetType {

    def fromInt(n: Int): Option[ResultSetType] =
      Some(n) collect {
        case TypeForwardOnly.toInt       => TypeForwardOnly
        case TypeScrollInsensitive.toInt => TypeScrollInsensitive
        case TypeScrollSensitive.toInt   => TypeScrollSensitive
      }

    def unsafeFromInt(n: Int): ResultSetType =
      fromInt(n).getOrElse(throw InvalidOrdinal[ResultSetType](n))

#+scalaz
    implicit val EqualResultSetType: Equal[ResultSetType] =
      Equal.equalBy(_.toInt)
#-scalaz
#+cats
    implicit val EqResultSetType: Eq[ResultSetType] =
      Eq.by(_.toInt)
#-cats
  }

}