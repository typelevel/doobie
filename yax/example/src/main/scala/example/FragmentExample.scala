package doobie.example

import doobie.imports._
#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats._, cats.implicits._
import fs2.interop.cats._ // for Monad[IOLite]
#-cats

object FragmentExample extends App {

  // Import some convenience constructors.
  import Fragments.{ in, whereAndOpt }

  // Country Info
  case class Info(name: String, code: String, population: Int)

  // Construct a Query0 with some optional filter conditions and a configurable LIMIT.
  def select(name: Option[String], pop: Option[Int], codes: List[String], limit: Long) = {

    // Three Option[Fragment] filter conditions.
    val f1 = name.map(s => fr"name LIKE $s")
    val f2 = pop.map(n => fr"population > $n")
    val f3 = codes.toNel.map(cs => in(fr"code", cs))

    // Our final query
    val q: Fragment =
      fr"SELECT name, code, population FROM country" ++
      whereAndOpt(f1, f2, f3)                        ++
      fr"LIMIT $limit"

    // Consruct a Query0
    q.query[Info]

  }

  // Our world database
  val xa = DriverManagerTransactor[IOLite](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  // Some quick examples.
  val prog = List(
    select(None, None, Nil, 10),
    select(Some("U%"), None, Nil, 10),
    select(None, Some(100000000), Nil, 10),
    select(Some("U%"), None, List("USA", "GBR", "FRA"), 10),
    select(Some("U%"), Some(100000000), List("USA", "GBR", "FRA"), 10)
  ).traverse { q =>
    val y = xa.yolo
    import y._
    q.check *> q.quick
  }

  // End of the world
  prog.unsafePerformIO

}
