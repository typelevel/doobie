// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect.IO
import doobie._, doobie.implicits._

class ManyRowsSuite extends munit.FunSuite {

  import cats.effect.unsafe.implicits.global

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  test("select should take consistent memory") {
    val q = sql"""select a.name, b.name from city a, city b""".query[(String, String)]
    q.stream.take(5).transact(xa).compile.drain.unsafeRunSync()
  }

}
