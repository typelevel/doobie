// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import doobie.implicits._

object manyrows extends PgSpec {

  "select" should {

    // TODO add timeout to test the server-side cursor
    "take consistent memory" in {
      val q = sql"""select a.name, b.name from city a, city b""".query[(String, String)]
      q.stream.take(5).transact(xa).compile.drain.unsafeRunSync
      true
    }
  }

}
