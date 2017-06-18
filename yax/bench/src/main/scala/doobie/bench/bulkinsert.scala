#+scalaz
package doobie.bench

import doobie.imports._
import doobie.postgres.imports._
import doobie.postgres.csv._

import scalaz._, scalaz.effect._, Scalaz._

object bulkinsert extends SafeApp {

  val xa = DriverManagerTransactor[IO]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

  case class Person(name: String, age: Int)

  val people: List[Person] =
    List.fill(10000)(Person("Bob", 42))

  def ddl: ConnectionIO[Unit] =
    sql"drop table if exists bench_person".update.run *>
    sql"create table bench_person (name varchar not null, age integer not null)".update.run.void

  val naive: ConnectionIO[Unit] =
    HC.prepareStatement("insert into bench_person (name, age) values (?, ?)")(
      people.foldRight(HPS.executeBatch.void)((p, k) => HPS.set(p) *> HPS.addBatch *> k)
    )

  val optimized: ConnectionIO[Unit] =
    HC.prepareStatement("insert into bench_person (name, age) values (?, ?)")(
      FPS.raw { ps =>
        people.foreach { p =>
          ps.setString(1, p.name)
          ps.setInt(2, p.age)
          ps.addBatch
        }
        ps.executeBatch
        ()
      }
    )

  val copyin: ConnectionIO[Unit] =
    sql"COPY bench_person (name, age) FROM STDIN WITH (FORMAT csv)".copyIn(people).void

  val now: ConnectionIO[Long] =
    HC.delay(System.currentTimeMillis)

  def bench(name: String, prog: ConnectionIO[_]): ConnectionIO[Unit] =
    for {
      start <- now
      _     <- ddl
      _     <- prog
      end   <- now
      rows  <- sql"select count(*) from bench_person".query[Int].unique
      _     <- HC.delay(Console.println(s"$name : $rows rows inserted in ${end - start} ms"))
    } yield ()

  override val runc =
    bench("naive",     naive    ).transact(xa) *>
    bench("optimized", optimized).transact(xa) *>
    bench("copyin",    copyin   ).transact(xa)

}
#-scalaz
