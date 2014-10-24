
package doobie.example

import doobie.syntax.process._
import doobie.syntax.string._
import doobie.util.transactor.DriverManagerTransactor
import doobie.contrib.postgresql.pgtypes._
import doobie.util.scalatype.ScalaType

import org.postgresql.geometric.PGpoint

import scalaz.concurrent.Task

object PostgresPoint extends App {

  val xa = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "rnorris", "")

  // A custom Point type with a ScalaType instance xmapped from the PostgreSQL native type (which
  // would be weird to use directly in a data model). Note that the presence of this `ScalaType`
  // instance precludes mapping `Point` to two columns. If you want two mappings you need two types.
  case class Point(x: Double, y: Double)
  object Point {
    implicit val PointType: ScalaType[Point] = 
      ScalaType[PGpoint].xmap(p => new Point(p.x, p.y), p => new PGpoint(p.x, p.y))
  }

  // Point is now a perfectly cromulent input/output type
  def q = sql"select '(1, 2)'::point".query[Point]
  val a = xa.transact(q.run.list).run
  Console.println(a)

}