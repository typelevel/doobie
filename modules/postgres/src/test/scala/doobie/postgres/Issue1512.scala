// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect.IO
import cats.syntax.applicative.catsSyntaxApplicativeId
import doobie.ConnectionIO
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import munit.CatsEffectSuite
import org.postgresql.ds.PGSimpleDataSource

import javax.sql.DataSource

class Issue1512 extends CatsEffectSuite {

  val minChunkSize = 200

  val datasource: DataSource = {
    val ds = new PGSimpleDataSource
    ds.setUser("postgres")
    ds.setPassword("password")
    ds
  }
  val xa: Transactor[IO] =
    Transactor.fromDataSource[IO](datasource, scala.concurrent.ExecutionContext.global)

  val setup: IO[Unit] =
    sql"""
        DROP TABLE IF EXISTS demo;
        CREATE TABLE demo(id BIGSERIAL PRIMARY KEY NOT NULL, data BIGINT NOT NULL);
         """.update.run
      .transact(xa).void

  test("A stream with a Pure effect inserts items properly") {

    setup.unsafeRunSync()

    // A pure stream is fine - can copy many items
    val count = 10000
    val stream = fs2.Stream.emits(1 to count)

    sql"COPY demo(data) FROM STDIN".copyIn(stream, minChunkSize).transact(xa).void.unsafeRunSync()

    val queryCount =
      sql"SELECT count(*) from demo".query[Int].unique.transact(xa).unsafeRunSync()

    assertEquals(queryCount, count)
  }

  test("A stream with a ConnectionIO effect copies <= than minChunkSize items") {

    setup.unsafeRunSync()

    // Can copy up to minChunkSize just fine with ConnectionIO
    val inputs = 1 to minChunkSize
    val stream = fs2.Stream.emits[ConnectionIO, Int](inputs)
      .evalMap(i => (i + 2).pure[ConnectionIO])

    val copiedRows = sql"COPY demo(data) FROM STDIN".copyIn(stream, minChunkSize).transact(xa).unsafeRunSync()

    assertEquals(copiedRows, inputs.size.toLong)

    val queryCount =
      sql"SELECT count(*) from demo".query[Int].unique.transact(xa).unsafeRunSync()

    assertEquals(queryCount, minChunkSize)
  }

  test("A stream with a ConnectionIO effect copies items with count > minChunkSize") {

    setup.unsafeRunSync()

    val inputs = 1 to minChunkSize + 1
    val stream = fs2.Stream.emits[ConnectionIO, Int](inputs)
      .evalMap(i => (i + 2).pure[ConnectionIO])

    val copiedRows = sql"COPY demo(data) FROM STDIN".copyIn(stream, minChunkSize).transact(xa).unsafeRunSync()
    assertEquals(copiedRows, inputs.size.toLong)

    val queryCount =
      sql"SELECT count(*) from demo".query[Int].unique.transact(xa).unsafeRunSync()

    assertEquals(queryCount, minChunkSize + 1)
  }
}
