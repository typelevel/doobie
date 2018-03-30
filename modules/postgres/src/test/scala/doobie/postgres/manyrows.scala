// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect.IO
import doobie._, doobie.implicits._
import doobie.postgres._, doobie.postgres.implicits._
import org.specs2.mutable.Specification
import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object manyrows extends Specification {

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  "select" should {

    // TODO add timeout to test the server-side cursor
    "take consistent memory" in {
      val q = sql"""select a.name, b.name from city a, city b""".query[(String, String)]
      q.stream.take(5).transact(xa).compile.drain.unsafeRunSync
      true
    }
  }

}
