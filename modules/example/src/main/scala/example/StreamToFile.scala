// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package example

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import doobie._
import doobie.implicits._
import fs2.{ io, text }
import java.nio.file.Paths
import java.util.concurrent.Executors

object StreamToFile extends IOApp {

  private val blockerR = Blocker.fromExecutorService(IO(Executors.newFixedThreadPool(2)))

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
  )

  def run(args: List[String]): IO[ExitCode] =
    blockerR.use { blocker =>
      sql"select name, population from country"
        .query[(String, Int)]
        .stream
        .map { case (n, p) => show"$n, $p" }
        .intersperse("\n")
        .through(text.utf8Encode)
        .through(io.file.writeAll(Paths.get("/tmp/out.txt"), blocker))
        .compile
        .drain
        .transact(xa)
        .as(ExitCode.Success)
    }

}
