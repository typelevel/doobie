// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package example

import cats.effect.{ IO, IOApp, ExitCode }
import doobie._
import doobie.enum.JdbcType.Other
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import cats.implicits._

object CallableStatementExample extends IOApp {

  def names(limit: Int): ConnectionIO[List[String]] =
    HC.prepareCall("{ call getCountries(?, ?) }") {
      for {
        _  <- FCS.setInt(1, limit)
        _  <- FCS.registerOutParameter(2, Other.toInt)
        _  <- FCS.execute
        rs <- FCS.getObject(2).map(_.asInstanceOf[java.sql.ResultSet])
        ns <- FCS.embed(rs, HRS.list[String] guarantee FRS.close)
      } yield ns
    }

  val xa: Transactor[IO] =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
    )

  def run(args: List[String]): IO[ExitCode] =
    for {
      ns <- names(10).transact(xa)
      _  <- ns.traverse(s => IO(println(s)))
    } yield ExitCode.Success

}
