package doobie.bench

import doobie.imports._

import java.sql.DriverManager

#+cats
import fs2.interop.cats._
#-cats

/** Rough benchmark based on non/jawn */
object bench {

  val xa = DriverManagerTransactor[IOLite]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

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
  def doobieBenchP(n: Int): Int =
    sql"select a.name, b.name, c.name from country a, country b, country c limit $n"
      .query[(String,String,String)]
      .process
      .list
      .transact(xa)
      .map(_.length)
      .unsafePerformIO

  // Reading via .list, which uses a lower-level collector
  def doobieBench(n: Int): Int =
    sql"select a.name, b.name, c.name from country a, country b, country c limit $n"
      .query[(String,String,String)]
      .list
      .transact(xa)
      .map(_.length)
      .unsafePerformIO

  // Reading via .vector, which uses a lower-level collector
  def doobieBenchV(n: Int): Int =
    sql"select a.name, b.name, c.name from country a, country b, country c limit $n"
      .query[(String,String,String)]
      .vector
      .transact(xa)
      .map(_.length)
      .unsafePerformIO

#+scalaz
  // Reading via .ilist, which uses a lower-level collector
  def doobieBenchI(n: Int): Int =
    HC.prepareStatement(s"select a.name, b.name, c.name from country a, country b, country c limit $n") {
      HPS.executeQuery {
        HRS.ilist[(String, String, String)]
      }
    } .transact(xa)
      .map(_.length)
      .unsafePerformIO
#-scalaz

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
            println(f"${"* " + name}%10s" ++ ts.map(n => f" |${n}%6.2f    --").mkString)
          case Some(bs) =>
            val ss = ts.zip(bs).map { case (t, b) =>
              val r = (100.0 * t / b)
              val c = if (r < 125) Console.GREEN else Console.RED
              f" |${t}%6.2f $c%s$r%5.0f${Console.RESET}"
            }
            println(f"${name}%10s" ++ ss.mkString)
        }
        ts
      }
    }
    def run(baseline: Case[_], tests: List[Case[_]]): Unit = {
      val bs = baseline.run(None)
      tests.foreach(_.run(Some(bs)))
    }
  }


  def main(args: Array[String]): Unit = {
    val bench    = Bench(2, 5, List(10, 100, 1000, 10000, 100000, 1000000))
    println("Warming up ...")
    val baseline = bench.Case("jdbc", jdbcBench)
    val cases = List(
      bench.Case("process", doobieBenchP),
#+scalaz
      bench.Case("ilist",   doobieBenchI),
#-scalaz
      bench.Case("list",    doobieBench),
      bench.Case("vector",  doobieBenchV),
      bench.Case("jdbc",    jdbcBench)
    )
    bench.run(baseline, cases)
  }

}


// > bench/run
// [info] Compiling 1 Scala source to /Users/rnorris/Scala/doobie/core/target/scala-2.11/classes...
// [info] Running doobie.bench.bench
//     * jdbc |     6    -- |     6    -- |     8    -- |    23    -- |    96    -- |  1371    --
//    process |     9   154 |    15   238 |    27   325 |    51   226 |   506   526 |  6018   439
//       list |     6   100 |     8   132 |     8    95 |    17    77 |   103   107 |  1403   102
//     vector |     6   100 |     8   133 |     9   105 |    19    85 |   102   106 |  1661   121
//      ilist |     7   121 |     7   107 |     9   105 |    20    90 |   111   115 |  1534   112
//       jdbc |     6    98 |     6   103 |     6    78 |    16    70 |   103   107 |  1593   116
// [success] Total time: 168 s, completed Jul 11, 2015 9:06:41 PM
