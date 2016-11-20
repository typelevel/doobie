package doobie.example

import doobie.imports._
#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats._, cats.implicits._
#-cats

object FragmentExample extends App {

  // Returns `WHERE c1 AND c1 AND ... cN` if any are defined, otherwise Fragment.empty. This is an
  // easy way to crush a list of optional filters into a single fragment.
  def where(clauses: Option[Fragment]*): Fragment =
    clauses.flatten.toList.foldSmash1(fr"WHERE", fr"AND", Fragment.empty)

  // Returns `x IN (a, b, ...)`
  def in[F[_]: Foldable: Functor, A: Param](x: Fragment, as: F[A]): Option[Fragment] =
    if (as.empty) None else Some(as.map(a => fr0"$a").foldSmash1(x + fr0"in (", fr",", fr")"))

  // Construct a Query0 with three optional filter conditions, one of which is an IN clause
  def select(name: Option[String], pop: Option[Int], codes: List[String]): Query0[(String, String, Int)] = {
    val s  = fr"SELECT name, code, population FROM country"
    val f1 = name.map(s => fr"name LIKE $s")
    val f2 = pop.map(n => fr"population > $n")
    val f3 = in(fr"code", codes)
    val q  = s + where(f1, f2, f3) + fr"LIMIT 10" // truncate
    q.query[(String, String, Int)]
  }

  // Our world database
  val xa = DriverManagerTransactor[IOLite](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  // Some quick examples.
  List(
    select(None, None, Nil),
    select(Some("U%"), None, Nil),
    select(None, Some(100000000), Nil),
    select(Some("U%"), None, List("USA", "GBR", "FRA")),
    select(Some("U%"), Some(100000000), List("USA", "GBR", "FRA"))
  ).foreach { q =>
    import xa.yolo._
    q.check.unsafePerformIO
    q.quick.unsafePerformIO
  }

}
