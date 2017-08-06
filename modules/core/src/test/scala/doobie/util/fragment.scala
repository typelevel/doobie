package doobie.util

import cats._
import cats.implicits._
import cats.effect.IO
import doobie.imports._
import org.specs2.mutable.Specification
import shapeless._

object fragmentspec extends Specification {

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:fragmentspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  "Fragment" should {

    val a = 1
    val b = "two"
    val c = true

    val fra = fr"$a"
    val frb = fr"$b"
    val frc = fr"$c"

    "substitute placeholders properly" in {
      fr"foo $a $b bar".query[HNil].sql must_== "foo ? ? bar "
    }

    "concatenate properly" in {
      (fr"foo" ++ fr"bar $a baz").query[HNil].sql must_== "foo bar ? baz "
    }

    "maintain parameter indexing (in-order)" in {
      val s = fr"select" ++ List(fra, frb, frc).intercalate(fr",")
      s.query[(Int, String, Boolean)].unique.transact(xa).unsafeRunSync must_== ((a, b, c))
    }

    "maintain parameter indexing (out-of-order)" in {
      val s = fr"select" ++ List(frb, frc, fra).intercalate(fr",")
      s.query[(String, Boolean, Int)].unique.transact(xa).unsafeRunSync must_== ((b, c, a))
    }

    "maintain associativity (left)" in {
      val s = fr"select" ++ List(fra, fr",", frb, fr",", frc).foldLeft(Fragment.empty)(_ ++ _)
      s.query[(Int, String, Boolean)].unique.transact(xa).unsafeRunSync must_== ((a, b, c))
    }

    "maintain associativity (right)" in {
      val s = fr"select" ++ List(fra, fr",", frb, fr",", frc).foldRight(Fragment.empty)(_ ++ _)
      s.query[(Int, String, Boolean)].unique.transact(xa).unsafeRunSync must_== ((a, b, c))
    }

    "translate to and from Query0" in {
      val f = fr"bar $a $b $c baz"
      f.query[HNil].toFragment unsafeEquals f
    }

    "translate to and from Update0" in {
      val f = fr"bar $a $b $c baz"
      f.update.toFragment unsafeEquals f
    }

    "Add a trailing space when constructed with .const" in {
      Fragment.const("foo").query[Int].sql must_== "foo "
    }

    "Not add a trailing space when constructed with .const0" in {
      Fragment.const0("foo").query[Int].sql must_== "foo"
    }

  }

}
