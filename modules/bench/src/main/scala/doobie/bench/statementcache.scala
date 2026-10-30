// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.bench

import cats.effect.IO
import cats.syntax.all.*
import java.sql.{Connection, DriverManager}
import org.typelevel.doobie.*
import org.typelevel.doobie.implicits.*
import org.typelevel.doobie.util.PreparedStatementCache
import org.openjdk.jmh.annotations.*

/** Runs the same parameterized query (or insert) many times on one connection, with and without
  * `PreparedStatementCache`.
  *
  * sqlite-jdbc does not cache prepared statements, so each prepare compiles the SQL again. H2 caches parsed commands
  * per session, so it shows the effect when the driver caches too.
  *
  * Run with e.g. `bench/Jmh/run -prof gc .*StatementCache.*`
  */
@State(Scope.Benchmark)
@OperationsPerInvocation(StatementCache.N)
class StatementCache {
  import cats.effect.unsafe.implicits.global
  import StatementCache.N

  @Param(Array("sqlite", "h2"))
  var db: String = ""

  @Param(Array("false", "true"))
  var cache: Boolean = false

  private var connection: Connection = null
  private var xa: Transactor[IO] = null

  @Setup(Level.Trial)
  def setup(): Unit = {
    val raw = db match {
      case "sqlite" => DriverManager.getConnection("jdbc:sqlite::memory:")
      case "h2"     => DriverManager.getConnection("jdbc:h2:mem:", "sa", "")
      case other    => sys.error(s"unknown db $other")
    }
    connection = if (cache) PreparedStatementCache.wrap(raw, maxSize = 16) else raw
    xa = Transactor.fromConnection[IO](connection, logHandler = None)
    val init = for {
      _ <- sql"create table item (id integer primary key, name varchar(100) not null, qty integer not null)".update.run
      _ <- sql"create table log (id integer not null, name varchar(100) not null)".update.run
      _ <- (1 to N).toList.traverse_(i => sql"insert into item values ($i, ${s"item $i"}, $i)".update.run)
    } yield ()
    init.transact(xa).unsafeRunSync()
  }

  @Setup(Level.Iteration)
  def clearLog(): Unit = {
    val _ = sql"delete from log".update.run.transact(xa).unsafeRunSync()
  }

  @TearDown(Level.Trial)
  def tearDown(): Unit = connection.close()

  private def select(id: Int): ConnectionIO[(String, Int)] =
    sql"select name, qty from item where id = $id".query[(String, Int)].unique

  private def insert(id: Int): ConnectionIO[Int] =
    sql"insert into log (id, name) values ($id, ${s"entry $id"})".update.run

  /** N selects of the same SQL in one transaction. */
  @Benchmark
  def query(): Int = (1 to N).toList.traverse(select).transact(xa).unsafeRunSync().length

  /** N inserts of the same SQL in one transaction. */
  @Benchmark
  def update(): Int = (1 to N).toList.traverse(insert).transact(xa).unsafeRunSync().sum
}

object StatementCache {
  final val N = 100
}
