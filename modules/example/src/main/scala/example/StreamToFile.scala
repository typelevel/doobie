// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package example

import cats.effect.{ ExitCode, IO, IOApp }
import cats.implicits._
import doobie._
import doobie.implicits._
import fs2.text
import fs2.io.file.Files
import java.nio.file.Paths


object StreamToFile extends IOApp {

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
  )

  def run(args: List[String]): IO[ExitCode] =
      sql"select name, population from country"
        .query[(String, Int)]
        .stream
        .map { case (n, p) => show"$n, $p" }
        .intersperse("\n")
        .through(text.utf8Encode)
        .through(Files[ConnectionIO].writeAll(Paths.get("/tmp/out.txt")))
        .compile
        .drain
        .transact(xa)
        .as(ExitCode.Success)    

}
