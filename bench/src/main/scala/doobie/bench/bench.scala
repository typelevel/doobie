package doobie.bench

import doobie.imports._
import java.sql.DriverManager
import scalaz._, Scalaz._
import scalaz.concurrent.Task

/** Rough benchmark based on non/jawn */
object bench {

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
          var accum = List.empty[(String,String,String)]
          while (rs.next) {
            val a = rs.getString(1) ; rs.wasNull
            val b = rs.getString(2) ; rs.wasNull
            val c = rs.getString(3) ; rs.wasNull
            accum = (a, b, c) :: accum
          }
          accum.reverse.length
        } finally {
          rs.close
        }
      } finally {
        ps.close
      }
    } finally {
      co.commit()
      co.close()
    }
  }

  // Reading via .process, which adds a fair amount of overhead
  def doobieBenchP(n: Int): Int = {
    val xa = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
    sql"select a.name, b.name, c.name from country a, country b, country c limit $n"
      .query[(String,String,String)]
      .process
      .list
      .transact(xa)
      .map(_.length)
      .run
  }

  // Reading via .list, which uses a lower-level collector
  def doobieBench(n: Int): Int = {
    val xa = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
    sql"select a.name, b.name, c.name from country a, country b, country c limit $n"
      .query[(String,String,String)]
      .list
      .transact(xa)
      .map(_.length)
      .run
  }

  // Reading with a custom row reader
  def doobieBenchProto(n: Int): Int = {
    import doobie.imports._, scalaz._, Scalaz._, scalaz.concurrent.Task
    val xa = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
    val sql = "select a.name, b.name, c.name from country a, country b, country c limit ?"
    val read = FRS.raw { rs =>
      val a = rs.getString(1) ; rs.wasNull
      val b = rs.getString(2) ; rs.wasNull
      val c = rs.getString(3) ; rs.wasNull
      (a, b, c)
    }
    def unroll[A](a: FRS.ResultSetIO[A]): FRS.ResultSetIO[List[A]] = {
      def go(as: List[A]): FRS.ResultSetIO[List[A]] = 
        FRS.next flatMap { b =>
          if (b) a.flatMap { a => go(a :: as) }
          else as.point[FRS.ResultSetIO]
        }
      go(Nil).map(_.reverse)
    }
    HC.prepareStatement(sql)(FPS.setInt(1, n) >> HPS.executeQuery(unroll(read)))
      .transact(xa)
      .map(_.length)
      .run
  }

  case class Bench(warmups: Int, runs: Int, ns: List[Int]) {
    def test[A](n: Int)(f: Int => A): Double = {
      var h = 0
      (0 until warmups).foreach { _ =>
        val result = f(n)
        h = h ^ result.##
        System.gc()
      }
      var t = 0.0
      (0 until runs).foreach { _ =>
        val t0 = System.nanoTime()
        val result = f(n)
        t += (System.nanoTime() - t0).toDouble / 1000000
        h = h ^ result.##
        System.gc()
      }
      t / runs
    }
    case class Case[A](name: String, f: (Int => A)) {
      def run(baseline: Option[List[Double]]): List[Double] = {        
        val ts = ns.map(test(_)(f))
        baseline match {
          case None =>
            println(f"${"* " + name}%10s" ++ ts.map(n => f" |${n}%6.0f    --").mkString)
          case Some(bs) =>
            val ss = ts.zip(bs).map { case (t, b) => 
              val r = (100.0 * t / b)
              val c = if (r < 125) Console.GREEN else Console.RED
              f" |${t}%6.0f $c%s$r%5.0f${Console.RESET}"
            }
            println(f"${name}%10s" ++ ss.mkString)
        }
        ts
      }
    }
    def run(baseline: Case[_], tests: List[Case[_]]): Unit = {
      // println(f"${""}%10s" ++ ns.map(n => f"$n%7d   ").mkString)
      val bs = baseline.run(None)
      tests.foreach(_.run(Some(bs)))
    }
  }


  def main(args: Array[String]): Unit = {
    val bench    = Bench(2, 5, List(10, 100, 1000, 10000, 100000, 1000000))
    val baseline = bench.Case("jdbc", jdbcBench)
    val cases = List(
      bench.Case("process", doobieBenchP),
      bench.Case("list", doobieBench),
      bench.Case("proto",  doobieBenchProto)
    )
    bench.run(baseline, cases)
  }

}
