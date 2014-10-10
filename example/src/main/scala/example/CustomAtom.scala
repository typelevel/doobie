package doobie.example

import doobie.util.scalatype.ScalaType
import doobie.util.atom.Atom
import doobie.util.composite.Composite

import doobie.syntax.string._

import java.sql.Date

import scalaz.{ Tag, @@ }

object CustomAtom {

  // Treat a Long as a date in code, but store it as a SQL DATE for reporting purposes.
  // We'll use a scalaz tagged type, but a value class would work fine as well.
  trait PosixTime
  val PosixTime = Tag.of[PosixTime]
  type LongPosixTime = Long @@ PosixTime

  // Create our base ScalaType by invariant mapping an existing one.
  implicit val LongPosixTimeScalaType: ScalaType[LongPosixTime] =
    ScalaType[Date].xmap(d => PosixTime(d.getTime), t => new Date(PosixTime.unwrap(t)))

  // Our base unsafe column type
  ScalaType[LongPosixTime]

  // Atom with null handling
  Atom[LongPosixTime] // non-nullable
  Atom[Option[LongPosixTime]] // nullable

  // Composites containing atomic types
  Composite[(LongPosixTime, Int, String)]
  Composite[(Option[LongPosixTime], Int, String)]

  // You can now use LongPosixTime as a column or parameter type
  def test(lpt: LongPosixTime) = sql"UPDATE WOOZLE SET DATE = $lpt".update 

}

