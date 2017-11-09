package doobie.bench

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import org.openjdk.jmh.annotations._


final case class Person(name: String, age: Int)

class csv {
  import shared._

  def people(n: Int): List[Person] =
    List.fill(n)(Person("Bob", 42))

  def ddl: ConnectionIO[Unit] =
    sql"drop table if exists bench_person".update.run *>
    sql"create table bench_person (name varchar not null, age integer not null)".update.run.void

  def naive(n: Int): ConnectionIO[Int] =
    ddl *> HC.prepareStatement("insert into bench_person (name, age) values (?, ?)")(
      people(n).foldRight(HPS.executeBatch)((p, k) => HPS.set(p) *> HPS.addBatch *> k)
    ).map(_.combineAll)

  def optimized(n: Int): ConnectionIO[Int] =
    ddl *> HC.prepareStatement("insert into bench_person (name, age) values (?, ?)")(
      FPS.raw { ps =>
        people(n).foreach { p =>
          ps.setString(1, p.name)
          ps.setInt(2, p.age)
          ps.addBatch
        }
        ps.executeBatch.sum
      }
    )

  def copyin(n: Int): ConnectionIO[Long] =
    ddl *> sql"COPY bench_person (name, age) FROM STDIN WITH (FORMAT csv)".copyIn(people(n))

  @Benchmark
  @OperationsPerInvocation(20000)
  def naive_copyin: Int = naive(20000).transact(xa).unsafeRunSync

  @Benchmark
  @OperationsPerInvocation(20000)
  def jdbc_copyin: Int = optimized(20000).transact(xa).unsafeRunSync

  @Benchmark
  @OperationsPerInvocation(20000)
  def fast_copyin: Long = copyin(20000).transact(xa).unsafeRunSync

}
