// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.specs2

import cats.Id
import cats.effect.{ ContextShift, IO }
import doobie.syntax.string._
import doobie.util.transactor.Transactor
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger


trait CheckerChecks[M[_]] extends Specification with Checker[M] {

  implicit def contextShift: ContextShift[M]

  lazy val transactor = Transactor.fromDriverManager[M](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  check(sql"select 1".query[Int])

  // Abstract type parameters should be handled correctly
  {
    final case class Foo[F[_]](x: Int)
    check(sql"select 1".query[Foo[Id]])
  }
}

class IOCheckerCheck extends CheckerChecks[IO] with IOChecker {

  def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  implicit val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

}
