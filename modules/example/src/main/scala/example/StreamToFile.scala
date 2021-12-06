// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package example

import cats.effect.{ IO, IOApp }
import cats.syntax.all._
import doobie._
import doobie.implicits._
import fs2.io.file.Files
import java.nio.file.Paths
import fs2.text.utf8


object StreamToFile extends IOApp.Simple {

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
  )

  def run: IO[Unit] =
      sql"select name, population from country"
        .query[(String, Int)]
        .stream
        .map { case (n, p) => show"$n, $p" }
        .intersperse("\n")
        .through(utf8.encode)
        .transact(xa)
        .through(Files[IO].writeAll(Paths.get("/tmp/out.txt")))
        .compile
        .drain

}
