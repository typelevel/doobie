import cats.effect.{IO, IOApp, Resource}
import doobie.{ConnectionIO, WeakAsync}
import cats.effect.{IO, IOApp}
import doobie._
import doobie.implicits._
import doobie.syntax._

import cats.effect._
import cats.implicits._

import scala.concurrent.duration._

object ResearchWithIO2 extends IOApp.Simple {

  val xa = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",  // JDBC driver classname
    url = "jdbc:postgresql://localhost:5432/postgres",     // Connect URL
    user = "postgres",                 // Database user name
    password = "mysecretpassword",             // Database password
    logHandler = None
  )

  val withLockTable: Resource[IO, Unit] =
    for {
      elevator <- WeakAsync.liftIO[ConnectionIO]
      _ <- {
        sql"LOCK TABLE example_table IN ACCESS EXCLUSIVE MODE".update.run >> elevator.liftIO(IO.sleep(10.seconds)) >> elevator.liftIO(IO.never[Unit])
      }.transact(xa).background
    } yield ()


  override def run: IO[Unit] = {

    for {
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

      _ <- IO.race(insertWithLockFiber.join, IO.sleep(5.seconds) >> IO(println("Cancellation is blocked")))
      result <- sql"SELECT * FROM example_table".query[Int].to[List].transact(xa)
      _ = println("OMG OMG")
    } yield println(result)

  }

}
