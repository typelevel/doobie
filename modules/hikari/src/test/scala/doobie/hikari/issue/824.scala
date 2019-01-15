// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.hikari.issue

import cats.effect._
import cats.implicits._
import doobie._
import doobie.hikari._
import doobie.implicits._
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object `824` extends Specification {

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  implicit def timer: Timer[IO] =
    IO.timer(ExecutionContext.global)

  val transactor: Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](16) // our connect EC
      te <- ExecutionContexts.cachedThreadPool[IO]    // our transaction EC
      xa <- HikariTransactor.newHikariTransactor[IO](
              "org.h2.Driver",                        // driver classname
              "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",   // connect URL
              "sa",                                   // username
              "",                                     // password
              ce,                                     // await connection here
              te                                      // execute JDBC operations here
            )
    } yield xa

  val prog: IO[Int] =
    transactor.use { xa =>

      // Show the state of the pool
      val report: IO[Unit] =
        IO {
          val mx = xa.kernel.getHikariPoolMXBean; import mx._
          println(s"Idle: $getIdleConnections, Active: $getActiveConnections, Total: $getTotalConnections, Waiting: $getThreadsAwaitingConnection")
        }

      // Kick off a concurrent transaction, reporting the pool state on exit
      val random: IO[Fiber[IO, Unit]] =
        for {
          d <- IO(Random.nextInt(200) milliseconds)
          f <- (IO.sleep(d) *> report).to[ConnectionIO].transact(xa).start
        } yield f

      // Run a bunch of transactions at once, then return the active connection count
      for {
        _  <- IO(xa.kernel.setMaximumPoolSize(10)) // max connections
        _  <- ().pure[ConnectionIO].transact(xa)  // do this once to init the MBean
        _  <- report
        fs <- random.replicateA(50)
        _  <- fs.traverse(_.join)
        _  <- report
        a  <- IO(xa.kernel.getHikariPoolMXBean.getActiveConnections)
      } yield a
    }

  "HikariTransactor" should {
    "close connections promptly" in {
      prog.unsafeRunSync must_== 0
    }
  }

}
