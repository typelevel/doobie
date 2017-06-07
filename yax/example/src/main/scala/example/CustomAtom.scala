package doobie.example

import doobie.imports._

import java.sql.Date

import scalaz.{ Tag, @@ }

object CustomAtom {

  // Treat a Long as a date in code, but store it as an SQL DATE for reporting purposes.
  // We'll use a scalaz tagged type, but a value class would work fine as well.
  trait PosixTimeTag
  type PosixTime = Long @@ PosixTimeTag
  val  PosixTime = Tag.of[PosixTimeTag]

  // Create our base Meta by invariant mapping an existing one.
  implicit val LongPosixTimeScalaType: Meta[PosixTime] =
    Meta[Date].xmap(d => PosixTime(d.getTime), t => new Date(PosixTime.unwrap(t)))

  // What we just defined
  Meta[PosixTime]

  // Free derived composites containing atomic types
  Composite[(PosixTime, Int, String)]
  Composite[(Option[PosixTime], Int, String)]

  // You can now use PosixTime as a column or parameter type (both demonstrated here)
  def query(lpt: PosixTime): Query0[(String, PosixTime)] =
    sql"SELECT NAME, DATE FROM FOO WHERE DATE > $lpt".query[(String, PosixTime)]

}
