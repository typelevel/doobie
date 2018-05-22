// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package example

import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import org.postgresql.geometric.PGpoint

object PostgresPoint extends App {

  val xa = Transactor.fromDriverManager[IO]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

  // A custom Point type with a Meta instance xmapped from the PostgreSQL native type (which
  // would be weird to use directly in a data model). Note that the presence of this `Meta`
  // instance precludes mapping `Point` to two columns. If you want two mappings you need two types.
  final case class Point(x: Double, y: Double)
  object Point {
    implicit val PointType: Meta[Point] =
      Meta[PGpoint].timap(p => new Point(p.x, p.y))(p => new PGpoint(p.x, p.y))
  }

  // Point is now a perfectly cromulent input/output type
  val q = sql"select '(1, 2)'::point".query[Point]
  val a = q.to[List].transact(xa).unsafeRunSync
  Console.println(a) // List(Point(1.0,2.0))

  // Just to be clear; the Write instance has width 1, not 2
  Console.println(Write[Point].length) // 1

}
