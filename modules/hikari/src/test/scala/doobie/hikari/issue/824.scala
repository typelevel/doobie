// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.hikari.issue

import cats.effect.*
import cats.syntax.all.*
import com.zaxxer.hikari.HikariDataSource
import doobie.*
import doobie.hikari.*
import doobie.implicits.*
import scala.concurrent.duration.*
import scala.util.Random
import doobie.util.ExecutionContexts

class Issue824 extends munit.FunSuite:

  import cats.effect.unsafe.implicits.global

  val transactor: Resource[IO, HikariTransactor[IO]] =
    for
      ce <- ExecutionContexts.fixedThreadPool[IO](16) // our connect EC
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.h2.Driver", // driver classname
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", // connect URL
        "sa", // username
        "", // password
        ce // await connection here
      )
    yield xa

  // Show the state of the pool
  def report(ds: HikariDataSource): IO[Unit] =
    IO {
      val mx = ds.getHikariPoolMXBean
      import mx.*
      println(s"Idle: $getIdleConnections, Active: $getActiveConnections, Total: $getTotalConnections, Waiting: $getThreadsAwaitingConnection")
    }

  // Yield final active connections within the use block, as well as total connections after use
  // block. Both should be zero
  val prog: IO[(Int, Int)] =
    transactor.use { xa =>
      // Kick off a concurrent transaction, reporting the pool state on exit
      val random: IO[Fiber[IO, Throwable, Unit]] =
        for 
          d <- IO(Random.nextInt(200).milliseconds)
          f <- (IO.sleep(d) *> xa.liftF(report(xa.kernel))).start
        yield f

      // Run a bunch of transactions at once, then return the active connection count
      for 
        _ <- IO(xa.kernel.setMaximumPoolSize(10)) // max connections
        _ <- ().pure[ConnectionIO].transact(xa) // do this once to init the MBean
        _ <- report(xa.kernel)
        fs <- List.fill(50)(random).sequence
        _ <- fs.traverse(_.joinWithNever)
        _ <- report(xa.kernel)
       yield (xa.kernel.getHikariPoolMXBean.getActiveConnections, xa.kernel)

    }.flatMap { case (n, ds) =>
      // One final report to show that all connections are disposed
      report(ds) *> IO((n, ds.getHikariPoolMXBean.getTotalConnections))

    }

  test("HikariTransactor should close connections logically within `use` block and physically afterward."):
    assertEquals(prog.unsafeRunSync(), (0, 0))
  


