// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package example

import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import cats.implicits._
import doobie._
import doobie.implicits._
import fs2.{ io, text }
import java.nio.file.Paths
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object StreamToFile extends IOApp {

  private val blockingExecutionContext =
    Resource.make(IO(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))))(ec => IO(ec.shutdown()))

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
  )

  def run(args: List[String]): IO[ExitCode] =
    blockingExecutionContext.use { bec =>
      sql"select name, population from country"
        .query[(String, Int)]
        .stream
        .map { case (n, p) => show"$n, $p" }
        .intersperse("\n")
        .through(text.utf8Encode)
        .through(io.file.writeAll(Paths.get("/tmp/out.txt"), Blocker.liftExecutionContext(bec)))
        .compile
        .drain
        .transact(xa)
        .as(ExitCode.Success)
    }

}
