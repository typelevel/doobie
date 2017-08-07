package doobie.example

import cats.data._
import cats.effect._
import cats.implicits._
import doobie.imports._
import fs2.Stream
import java.sql.Connection

/**
 * Example of resource-safe transactional database-to-database copy with fs2. If you induce failures
 * on either side (by putting a typo in the `read` or `write` statements) both transactions will
 * roll back.
 */
object StreamingCopy {

  /**
   * Stream from `source` through `sink`, where source and sink run on distinct transactors. To do
   * this we have to wrap one transactor around the other. Thanks to @wedens for
   */
  def fuseMap[F[_]: Effect, A, B](
    source: Stream[ConnectionIO, A],
    sink:   A => ConnectionIO[B]
  )(
    sourceXA: Transactor[F],
    sinkXA:   Transactor[F]
  ): Stream[F, B] = {

    // Interpret a ConnectionIO into a Kleisli arrow for F via the sink interpreter.
    def interpS[T](f: ConnectionIO[T]): Connection => F[T] =
      f.foldMap(sinkXA.interpret).run

    // Open a connection in `F` via the sink transactor. Need patmat due to the existential.
    val open: F[Connection] =
      sinkXA match { case xa => xa.connect(xa.kernel) }

    // Given a Connection we can construct the stream we want.
    def mkStream(c: Connection): Stream[F, B] = {

      // Now we can interpret a ConnectionIO into a Stream of F via the sink interpreter.
      def evalS(f: ConnectionIO[_]): Stream[F, Nothing] =
        Stream.eval_(interpS(f)(c))

      // And can thus lift all the sink operations into Stream of F
      val sinkʹ  = (a: A) => evalS(sink(a))
      val before = evalS(sinkXA.strategy.before)
      val after  = evalS(sinkXA.strategy.after )
      def oops(t: Throwable) = evalS(sinkXA.strategy.oops <* FC.raiseError(t))

      // And construct our final stream.
      (before ++ source.transact(sourceXA).flatMap(sinkʹ) ++ after).onError(oops)

    }

    // And our cleanup action.
    val cleanup: Connection => F[Unit] =
      interpS(sinkXA.strategy.always)

    // And we're done!
    Stream.bracket(open)(mkStream, cleanup)

  }

  // /**
  //  * Stream from `source` into `sink`, where source and sink run on distinct transactors. Unlike
  //  * fuseMap above, this doesn't return a stream. But it's a much simpler implementation. Thanks
  //  * @wedens for this one.
  //  */
  // def fuseMap2[F[_]: Async, A, B](
  //   source: Stream[ConnectionIO, A],
  //   sink: A => ConnectionIO[B]
  // )(
  //   sourceXA: Transactor[F],
  //   sinkXA: Transactor[F]
  // ): F[Unit] =
  //   sinkXA.exec.apply {
  //     source
  //       .transact(sourceXA)
  //       .translate(λ[F ~> Kleisli[F, Connection, ?]](a => Kleisli(_ => a)))
  //       .evalMap(sink(_).foldMap(sinkXA.interpret))
  //       .run
  //   }

  // Everything below is code to demonstrate the combinator above.

  /** Prepend a ConnectionIO program with a log message. */
  def printBefore(tag: String, s: String): ConnectionIO[Unit] => ConnectionIO[Unit] =
    HC.delay(Console.println(s"$tag: $s")) <* _

  /** Derive a new transactor that logs stuff. */
  def addLogging[F[_], A](name: String)(xa: Transactor[F]): Transactor[F] = {
    import Transactor._ // bring the lenses into scope
    val update: State[Transactor[F], Unit] =
      for {
        _ <- before %= printBefore(name, "before - setting up the connection")
        _ <- after  %= printBefore(name, "after - committing")
        _ <- oops   %= printBefore(name, "oops - rolling back")
        _ <- always %= printBefore(name, "always - closing")
      } yield ()
    update.runS(xa).value
  }


  // A data type to move.
  case class City(id: Int, name: String, countrycode: String, district: String, population: Int)

  // A producer of cities, to be run on database 1
  def read: Stream[ConnectionIO, City] =
    sql"""
      SELECT id, name, countrycode, district, population
      FROM city
      WHERE name like 'X%'
    """.query[City].stream

  // A consumer of cities, to be run on database 2
  def write(c: City): ConnectionIO[Unit] =
    printBefore("write", c.toString)(
      sql"""
        INSERT INTO city (id, name, countrycode, district, population)
        VALUES (${c.id}, ${c.name}, ${c.countrycode}, ${c.district}, ${c.population})
      """.update.run.void
    )

  /** Table creation for our destination DB. We assume the source is populated. */
  val ddl: ConnectionIO[Unit] =
    sql"""
      CREATE TABLE IF NOT EXISTS city (
          id integer NOT NULL,
          name varchar NOT NULL,
          countrycode character(3) NOT NULL,
          district varchar NOT NULL,
          population integer NOT NULL
      )
    """.update.run.void


  // A postges transactor for our source. We assume the WORLD database is set up already.
  val pg = addLogging("Postgres")(Transactor.fromDriverManager[IO]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""))

  // An h2 transactor for our sink.
  val h2 = addLogging("H2") {
    val xa = Transactor.fromDriverManager[IO]("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
    Transactor.before.modify(xa, _ *> ddl) // Run our DDL on every connection
  }

  // Our main program
  val io: IO[Unit] =
    for {
      _ <- fuseMap(read, write)(pg, h2).run // do the copy with fuseMap
      // _ <- fuseMap2(read, write)(pg, h2)    // again with fuseMap2
      n <- sql"select count(*) from city".query[Int].unique.transact(h2)
      _ <- IO(Console.println(s"Copied $n cities!"))
    } yield ()

  // Scala entry point
  def main(args: Array[String]): Unit =
    io.unsafeRunSync

}
