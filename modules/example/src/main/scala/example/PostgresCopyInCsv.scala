// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package example

import cats.implicits._
import cats.effect.{ IO, IOApp, ExitCode }
import doobie._
import doobie.implicits._
import doobie.postgres._
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

object PostgresCopyInCsv extends IOApp {
  val csv = """name,food
                |piglet,haycorns
                |eeyore,thistles
                |pooh,honey
                |tigger,extract of malt""".stripMargin

  val is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql://localhost/postgres", "postgres", "super-secret"
  )

  val createTable: ConnectionIO[Int] = sql"CREATE TEMP TABLE favorite_foods(name TEXT, food TEXT)".update.run


  val copyIn: ConnectionIO[Long] = PHC.pgGetCopyAPI(PFCM.copyIn("COPY favorite_foods(name, food) FROM STDIN WITH (FORMAT CSV, HEADER, DELIMITER ',')", is))

  def run(args: List[String]): IO[ExitCode] = 
    (createTable >> copyIn).transact(xa)
        .flatMap(ct => IO(Console.println(show"loaded ${ct}")))
        .as(ExitCode.Success)
    
}