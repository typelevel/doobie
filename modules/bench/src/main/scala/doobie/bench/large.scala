// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.bench

import cats.effect.IO
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.*
import doobie.syntax.all.*
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import scala.util.Using

@State(Scope.Benchmark)
@OperationsPerInvocation(10000) // We process 10k rows so adjust the benchmark output accordingly
class LargeRow {
  import cats.effect.unsafe.implicits.global

  private val hikariConfig = {
    val config = new HikariConfig()
    config.setDriverClassName("org.postgresql.Driver")
    config.setJdbcUrl("jdbc:postgresql:world")
    config.setUsername("postgres")
    config.setPassword("password")
    config.setMaximumPoolSize(2)
    config
  }

  val pool = new HikariDataSource(hikariConfig)

  val (xa, cleanup) = {
    (for {
      connectEC <- ExecutionContexts.fixedThreadPool[IO](hikariConfig.getMaximumPoolSize)
    } yield Transactor.fromDataSource[IO].apply[HikariDataSource](pool, connectEC)).allocated.unsafeRunSync()
  }

  @Setup(Level.Trial)
  def setup(): Unit = {
    val connio = for {
      _ <- sql"""DROP TABLE IF EXISTS data""".update.run
      _ <- sql"""CREATE TABLE data (
                  col1 DOUBLE PRECISION,
                  col2 VARCHAR(50),
                  col3 INTEGER,
                  col4 VARCHAR(50),
                  col5 DOUBLE PRECISION,
                  col6 DOUBLE PRECISION,
                  col7 VARCHAR(50),
                  col8 VARCHAR(50)
              );""".update.run
      _ <- sql"select setseed(0.5)".query[Unit].unique // deterministic seed
      _ <- sql"""INSERT INTO data (col1, col2, col3, col4, col5, col6, col7, col8)
              SELECT random(), random() :: text, (random() * 1000) :: int, random() :: text, random(), random(), random() :: text, random() :: text
              FROM generate_series(1, 10000)
          """.update.run
    } yield ()

    connio.transact(xa).unsafeRunSync()
  }

  @TearDown(Level.Trial)
  def teardown(): Unit = {
    pool.close()
    cleanup.unsafeRunSync()
  }

  @Benchmark
  def tuple(bh: Blackhole): Unit = {
    bh.consume(sql"""SELECT col1, col2, col3, col4, col5, col6, col7, col8 FROM data"""
      .query[(Double, String, Int, String, Double, Double, String, String)].to[List].transact(xa).unsafeRunSync())
  }

  @Benchmark
  def tupleOpt(bh: Blackhole): Unit = {
    bh.consume(sql"""SELECT col1, col2, col3, col4, col5, col6, col7, col8 FROM data"""
      .query[Option[(Double, String, Int, String, Double, Double, String, String)]].to[List].transact(xa).unsafeRunSync())
  }

  @Benchmark
  def semiautoDerivedComplex(bh: Blackhole): Unit = {
    import SemiautoDerivedInstances.*
    bh.consume(sql"""SELECT col1, col2, col3, col4, col5, col6, col7, col8 FROM data"""
      .query[Complex].to[List].transact(xa).unsafeRunSync())
  }

  @Benchmark
  def semiautoDerivedComplexOpt(bh: Blackhole): Unit = {
    import SemiautoDerivedInstances.*
    bh.consume(sql"""SELECT col1, col2, col3, col4, col5, col6, col7, col8 FROM data"""
      .query[Option[Complex]].to[List].transact(xa).unsafeRunSync())
  }

  @Benchmark
  def autoDerivedComplex(bh: Blackhole): Unit = {
    import doobie.implicits.*
    bh.consume(sql"""SELECT col1, col2, col3, col4, col5, col6, col7, col8 FROM data"""
      .query[Complex].to[List].transact(xa).unsafeRunSync())
  }

  @Benchmark
  def autoDerivedComplexOpt(bh: Blackhole): Unit = {
    import doobie.implicits.*
    bh.consume(sql"""SELECT col1, col2, col3, col4, col5, col6, col7, col8 FROM data"""
      .query[Option[Complex]].to[List].transact(xa).unsafeRunSync())
  }

  @Benchmark
  def rawJdbcComplex(bh: Blackhole): Unit = {
    var l: List[Complex] = null
    Using.resource(pool.getConnection()) { c =>
      Using.resource(c.prepareStatement("SELECT col1, col2, col3, col4, col5, col6, col7, col8 FROM data")) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          val m = scala.collection.mutable.ListBuffer.empty[Complex]
          while (rs.next()) {
            m += Complex(
              DSIS(
                DS(
                  rs.getDouble(1),
                  rs.getString(2)
                ),
                IS(
                  rs.getInt(3),
                  rs.getString(4)
                )
              ),
              DDSS(
                DD(
                  rs.getDouble(5),
                  rs.getDouble(6)
                ),
                SS(
                  rs.getString(7),
                  rs.getString(8)
                )
              )
            )
          }
          l = m.toList
        }
      }

    }
    bh.consume(l)
  }

  @Benchmark
  def rawJdbcTuple(bh: Blackhole): Unit = {
    type Tup = (Double, String, Int, String, Double, Double, String, String)
    var l: List[Tup] = null
    Using.resource(pool.getConnection()) { c =>
      Using.resource(c.prepareStatement("SELECT col1, col2, col3, col4, col5, col6, col7, col8 FROM data")) { ps =>
        Using.resource(ps.executeQuery()) { rs =>
          val m =
            scala.collection.mutable.ListBuffer.empty[Tup]
          while (rs.next()) {
            m += Tuple8(
              rs.getDouble(1),
              rs.getString(2),
              rs.getInt(3),
              rs.getString(4),
              rs.getDouble(5),
              rs.getDouble(6),
              rs.getString(7),
              rs.getString(8)
            )
          }
          l = m.toList
        }
      }

    }
    bh.consume(l)
  }
}

case class IS(i: Int, s: String)
case class DS(d: Double, s: String)
case class DSIS(ds: DS, is: IS)
case class DD(d0: Double, d1: Double)
case class SS(s0: String, s1: String)
case class DDSS(dd: DD, ss: SS)
case class Complex(dsis: DSIS, ddss: DDSS)

object SemiautoDerivedInstances {
  implicit val isRead: Read[IS] = Read.derived
  implicit val dsRead: Read[DS] = Read.derived
  implicit val dsisRead: Read[DSIS] = Read.derived
  implicit val ddRead: Read[DD] = Read.derived
  implicit val ssRead: Read[SS] = Read.derived
  implicit val ddssRead: Read[DDSS] = Read.derived
  implicit val cRead: Read[Complex] = Read.derived
}
