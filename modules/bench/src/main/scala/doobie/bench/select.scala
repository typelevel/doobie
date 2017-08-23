package doobie.bench

import cats.effect.IO
import doobie._, doobie.implicits._
import java.sql.DriverManager
import org.openjdk.jmh.annotations._

object bench {
  @State(Scope.Benchmark)
  val xa = Transactor.fromDriverManager[IO]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
}

/** Rough benchmark based on non/jawn */
class bench {
  import bench._

  // Baseline hand-written JDBC code
  def jdbcBench(n: Int): Int = {
    Class.forName("org.postgresql.Driver")
    val co = DriverManager.getConnection("jdbc:postgresql:world", "postgres", "")
    try {
      co.setAutoCommit(false)
      val ps = co.prepareStatement("select a.name, b.name, co.name from country a, country b, country co limit ?")
      try {
        ps.setInt(1, n)
        val rs = ps.executeQuery
        try {
          val accum = List.newBuilder[(String,String,String)]
          while (rs.next) {
            val a = rs.getString(1) ; rs.wasNull
            val b = rs.getString(2) ; rs.wasNull
            val c = rs.getString(3) ; rs.wasNull
            accum += ((a, b, c))
          }
          accum.result().length
        } finally rs.close
      } finally ps.close
    } finally {
      co.commit()
      co.close()
    }
  }

  // Reading via .process, which adds a fair amount of overhead
  def doobieBenchP(n: Int): Int =
    sql"select a.name, b.name, c.name from country a, country b, country c limit $n"
      .query[(String,String,String)]
      .process
      .list
      .transact(xa)
      .map(_.length)
      .unsafeRunSync

  // Reading via .list, which uses a lower-level collector
  def doobieBench(n: Int): Int =
    sql"select a.name, b.name, c.name from country a, country b, country c limit $n"
      .query[(String,String,String)]
      .list
      .transact(xa)
      .map(_.length)
      .unsafeRunSync

  // Reading via .vector, which uses a lower-level collector
  def doobieBenchV(n: Int): Int =
    sql"select a.name, b.name, c.name from country a, country b, country c limit $n"
      .query[(String,String,String)]
      .vector
      .transact(xa)
      .map(_.length)
      .unsafeRunSync


  @Benchmark
  @OperationsPerInvocation(1000)
  def list_accum_1000_jdbc: Int = jdbcBench(1000)

  @Benchmark
  @OperationsPerInvocation(1000)
  def list_accum_1000: Int = doobieBench(1000)

  @Benchmark
  @OperationsPerInvocation(1000)
  def vector_accum_1000: Int = doobieBenchV(1000)

  @Benchmark
  @OperationsPerInvocation(1000)
  def stream_accum_1000: Int = doobieBenchP(1000)

}
