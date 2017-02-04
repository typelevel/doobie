package doobie.example

import doobie.imports._
import doobie.util.iolite.IOLite
import doobie.postgres.imports._

import org.postgresql.geometric.PGpoint

#+cats
import fs2.interop.cats._
#-cats

object PostgresPoint extends App {

  val xa = DriverManagerTransactor[IOLite]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

  // A custom Point type with a Meta instance xmapped from the PostgreSQL native type (which
  // would be weird to use directly in a data model). Note that the presence of this `Meta`
  // instance precludes mapping `Point` to two columns. If you want two mappings you need two types.
  case class Point(x: Double, y: Double)
  object Point {
    implicit val PointType: Meta[Point] =
      Meta[PGpoint].nxmap(p => new Point(p.x, p.y), p => new PGpoint(p.x, p.y))
  }

  // Point is now a perfectly cromulent input/output type
  val q = sql"select '(1, 2)'::point".query[Point]
  val a = q.list.transact(xa).unsafePerformIO
  Console.println(a) // List(Point(1.0,2.0))

  // Just to be clear; the Composite instance has width 1, not 2
  Console.println(Composite[Point].length) // 1

}
