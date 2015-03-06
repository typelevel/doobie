
package doobie.example

import doobie.imports._
import doobie.contrib.postgresql.pgtypes._

import org.postgresql.geometric.PGpoint

import scalaz.concurrent.Task

object PostgresPoint extends App {

  val xa = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

  // A custom Point type with a Meta instance xmapped from the PostgreSQL native type (which
  // would be weird to use directly in a data model). Note that the presence of this `Meta`
  // instance precludes mapping `Point` to two columns. If you want two mappings you need two types.
  case class Point(x: Double, y: Double)
  object Point {
    implicit val PointType: Meta[Point] =
      Meta[PGpoint].xmap(p => new Point(p.x, p.y), p => new PGpoint(p.x, p.y))
  }

  // Point is now a perfectly cromulent input/output type
  def q = sql"select '(1, 2)'::point".query[Point]
  val a = q.list.transact(xa).run
  Console.println(a) // List(Point(1.0,2.0))
}
