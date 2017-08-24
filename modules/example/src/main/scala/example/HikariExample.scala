// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.example

import cats.effect.IO
import doobie.implicits._
import doobie.hikari._, doobie.hikari.implicits._

object HikariExample {

  def tmain: IO[Unit] =
    for {
      xa <- HikariTransactor[IO]("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
      _  <- FreeUsage.examples.transact(xa)
      _  <- xa.shutdown
    } yield ()

  def main(args: Array[String]): Unit =
    tmain.unsafeRunSync

}
