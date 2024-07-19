// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie

import cats.data.NonEmptyList
import doobie.hikari.HikariTransactor
import doobie.implicits._
import cats.syntax.all._
import cats.effect.IO
import cats.effect.std.Random
import com.zaxxer.hikari.HikariConfig
import doobie.hi.{connection => IHC, resultset => IHRS}
import doobie.free.{connection => IFC, preparedstatement => IFPS, resultset => IFRS}
import doobie.util.log.{Parameters, LoggingInfo}
import org.postgresql.util.PSQLException
import cats.effect.unsafe.implicits.global

import scala.concurrent.duration._

class ResourceCleanupSuite extends munit.FunSuite {

  test("Connection is always closed when running a query (with or without errors)") {

    val config = {
      val c = new HikariConfig()
      c.setDriverClassName("org.postgresql.Driver")
      c.setJdbcUrl("jdbc:postgresql://localhost/postgres")
      c.setUsername("postgres")
      c.setPassword("password")
      // Use a small pool so if there's a connection leak we'll find out easier
      c.setMaximumPoolSize(2)
      c
    }
    val pool = HikariTransactor.fromHikariConfig[IO](config, logHandler = None)

    pool.use { xa =>
      for {
        rnd <- Random.scalaUtilRandom[IO]
        _ <- (for {
          scenario <- rnd.betweenInt(0, 5)
          res <- q(scenario).transact(xa).attempt
          _ = res match {
            case Left(_: SimulatedError) => () // pass
            case Left(e: PSQLException)  => assert(e.getMessage.contains("""relation "not_exist" does not exist"""))
            case Left(e)                 => fail("Unexpected error from scenario", e)
            case Right(_)                => () // pass
          }
        } yield ())
          .parReplicateA_(100)
          .timeout(10.seconds)
      } yield ()
    }.unsafeRunSync()
  }

  def q(i: Int): ConnectionIO[NonEmptyList[Int]] = {
    i match {
      case 0 => // Bad SQL provided when constructing PS
        IHC.executeWithResultSet(
          create = IFC.prepareStatement("select * from not_exist"),
          prep = IFPS.unit,
          exec = IFPS.executeQuery,
          process = IFRS.raiseError(new Exception("shouldn't reach here")),
          loggingInfo = loggingInfo
        )
      case 1 => // Error in PS "prepare" step
        IHC.executeWithResultSet(
          create = IFC.prepareStatement("SELECT * FROM generate_series(1, ?)"),
          prep = IFPS.setInt(1, 10) *> IFPS.raiseError(new SimulatedError),
          exec = IFPS.raiseError(new Exception("shouldn't reach here")),
          process = IFRS.raiseError(new Exception("shouldn't reach here")),
          loggingInfo = loggingInfo
        )
      case 2 => // Error during execute step
        IHC.executeWithResultSet(
          IFC.prepareStatement("SELECT * FROM generate_series(1, ?)"),
          IFPS.setInt(1, 10),
          IFPS.executeQuery *> IFPS.raiseError(new SimulatedError),
          IFRS.raiseError(new Exception("shouldn't reach here")),
          loggingInfo
        )
      case 3 => // Error during process step
        IHC.executeWithResultSet(
          IFC.prepareStatement("SELECT * FROM generate_series(1, ?)"),
          IFPS.setInt(1, 10),
          IFPS.executeQuery,
          IHRS.nel[Int] *> IFRS.raiseError(new SimulatedError),
          loggingInfo
        )
      case 4 => // No error
        IHC.executeWithResultSet(
          IFC.prepareStatement("SELECT * FROM generate_series(1, ?)"),
          IFPS.setInt(1, 10),
          IFPS.executeQuery,
          IHRS.nel[Int],
          loggingInfo
        )
      case other => throw new Exception(s"Unexpected random num $other")
    }
  }

  private val loggingInfo = LoggingInfo(
    "select * from not_exist",
    Parameters.nonBatchEmpty,
    "unset"
  )

  private class SimulatedError() extends Exception("simulated error")

}
