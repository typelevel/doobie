package doobie.bench

import doobie.imports._
import java.sql.DriverManager
import scalaz._, Scalaz._
import scalaz.concurrent.Task

/** Rough benchmark based on non/jawn */
object bench {

  val xa = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

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
  def doobieBenchP(n: Int): Int =
    sql"select a.name, b.name, c.name from country a, country b, country c limit $n"
      .query[(String,String,String)]
      .process
      .list
      .transact(xa)
      .map(_.length)
      .run

  // Reading via .list, which uses a lower-level collector
  def doobieBench(n: Int): Int = 
    sql"select a.name, b.name, c.name from country a, country b, country c limit $n"
      .query[(String,String,String)]
      .list
      .transact(xa)
      .map(_.length)
      .run

  // Reading via .vector, which uses a lower-level collector
  def doobieBenchV(n: Int): Int =
    sql"select a.name, b.name, c.name from country a, country b, country c limit $n"
      .query[(String,String,String)]
      .vector
      .transact(xa)
      .map(_.length)
      .run

  // Reading via .ilist, which uses a lower-level collector
  def doobieBenchI(n: Int): Int =
    HC.prepareStatement(s"select a.name, b.name, c.name from country a, country b, country c limit $n") {
      HPS.executeQuery {
        HRS.ilist[(String, String, String)]
      }
    } .transact(xa)
      .map(_.length)
      .run

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
      val bs = baseline.run(None)
      tests.foreach(_.run(Some(bs)))
    }
  }


  def main(args: Array[String]): Unit = {
    val bench    = Bench(2, 5, List(10, 100, 1000, 10000)) //, 100000, 1000000))
    val baseline = bench.Case("jdbc", jdbcBench)
    val cases = List(
      bench.Case("process", doobieBenchP) //,
      // bench.Case("list",    doobieBench),
      // bench.Case("vector",  doobieBenchV),
      // bench.Case("ilist",   doobieBenchI)
    )
    bench.run(baseline, cases)
  }

}


// > bench/run
// [info] Compiling 1 Scala source to /Users/rnorris/Scala/doobie/core/target/scala-2.11/classes...
// [info] Running doobie.bench.bench 
//     * jdbc |     9    -- |     8    -- |    11    -- |    18    -- |   117    -- |  1560    --
//    process |    16   191 |    45   557 |    87   776 |   116   648 |  1058   904 | 12161   779
//       list |     6    75 |     8    96 |    11   102 |    51   286 |   542   463 |  6288   403
//      proto |     6    75 |     7    81 |     9    81 |    22   124 |   201   172 |  2619   168
// [success] Total time: 247 s, completed Jul 5, 2015 1:07:40 PM
// > bench/run
// [info] Compiling 1 Scala source to /Users/rnorris/Scala/doobie/core/target/scala-2.11/classes...
// [info] Running doobie.bench.bench 
//     * jdbc |     8    -- |     9    -- |    10    -- |    25    -- |   147    -- |  1357    --
//    process |    13   160 |    31   359 |    35   354 |    97   382 |   944   640 | 10006   737
//       list |     8   100 |     7    82 |    14   146 |    53   207 |   562   381 |  6429   474
//      proto |     6    76 |     7    84 |    10   103 |    23    89 |   204   138 |  2524   186
// [success] Total time: 227 s, completed Jul 5, 2015 1:12:18 PM

