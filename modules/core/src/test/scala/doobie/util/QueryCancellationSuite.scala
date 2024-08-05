package doobie.util

import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxApplicativeId
import doobie.Transactor
import doobie.*
import doobie.implicits.*
import doobie.syntax.*
import cats.syntax.all.*

import scala.concurrent.duration.DurationInt

class QueryCancellationSuite extends munit.FunSuite {
  import cats.effect.unsafe.implicits.global

  val xa = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  val withLockTable: Resource[IO, Unit] =
    for {
      elevator <- WeakAsync.liftIO[ConnectionIO]
      _ <- {
        sql"select * from example_table for update".query[Int].unique >> elevator.liftIO(IO.sleep(1.seconds)) >> elevator.liftIO(IO.never[Unit])
      }.transact(xa).background
    } yield ()

  test("Query cancel") {
    val scenario = for {
      _ <- sql"CREATE TABLE IF NOT EXISTS example_table ( id INT)".update.run.transact(xa)
      _ <- sql"TRUNCATE TABLE example_table".update.run.transact(xa)

      insertWithLockFiber <- withLockTable.use(
        _ => for {
          insertFiber <- sql"INSERT INTO example_table (id) VALUES (1)".update.run.transact(xa).start
          _ <- IO.sleep(1.second)
          _ <- insertFiber.cancel
          _ <- IO(println("CANCEL"))
        } yield ()
      ).start

      _ <- IO.race(insertWithLockFiber.join, IO.sleep(5.seconds) >> IO(fail("Cancellation is blocked")))
      result <- sql"SELECT * FROM example_table".query[Int].to[List].transact(xa)
      _ = println("OMG OMG")
    } yield assertEquals(result, List.empty[Int])

    scenario.unsafeRunSync()
  }
}
