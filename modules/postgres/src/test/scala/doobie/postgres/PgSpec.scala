// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import doobie._
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import cats.effect.ContextShift
import cats.effect.IO

trait PgSpec extends Specification {

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  implicit def logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

}