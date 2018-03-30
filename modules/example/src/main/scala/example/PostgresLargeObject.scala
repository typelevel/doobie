// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

// relies on whenM, etc. so no cats for now
package example

import doobie._, doobie.implicits._
import doobie.postgres._
import java.io.File

import cats.effect.IO

/**
  * Example of using the high-level Large Object API. See the Postgres JDBC driver doc and the
  * source in doobie.contrib.postgresql for more information.
  */
object PostgresLargeObject {

  val xa = Transactor.fromDriverManager[IO]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

  val prog: LargeObjectManagerIO[Long] =
    for {
      oid <- PHLOM.createLOFromFile(1024, new File("world.sql"))
      _   <- PHLOM.createFileFromLO(1024, oid, new File("world2.sql"))
      _   <- PHLOM.delete(oid)
    } yield oid

  val task: IO[Unit] =

    PHC.pgGetLargeObjectAPI(prog).transact(xa).flatMap { oid =>
      IO(Console.println("oid was " + s"$oid"))
    }

  def main(args: Array[String]): Unit =
    task.unsafeRunSync()

}