// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie

import cats.effect.ContextShift
import cats.effect.IO
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext

trait H2Spec extends Specification {

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  implicit val logger: Logger[IO] =
    Slf4jLogger.getLoggerFromClass[IO](getClass())

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:fragmentspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

}