package doobie.util

import shapeless._, shapeless.test._
import scala.Predef.classOf
import doobie.imports._
import doobie.enum.jdbctype._
import org.specs2.mutable.Specification
#+cats
import fs2.interop.cats._
#-cats

object metaspec extends Specification {
  case class X(x: Int)
  case class Y(x: String) extends AnyVal
  case class P(x: Int) extends AnyVal
  case class Q(x: String)

  case class Z(i: Int, s: String)
  object S

  case class Reg1(x: Int)
  case class Reg2(x: Int)

  val xa = DriverManagerTransactor[IOLite](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  case class Foo(s: String)
  case class Bar(n: Int)

  "Meta" should {

    "exist for primitive types" in {
      Meta[Int]
      Meta[String]

      true
    }

    "be derived for unary products" in {
      Meta[X]
      Meta[Y]
      Meta[P]
      Meta[Q]

      true
    }

    "not be derived for non-unary products" in {
      illTyped("Meta[Z]")
      illTyped("Meta[(Int, Int)]")
      illTyped("Meta[S.type]")

      true
    }

    "register new instances" in {
      Meta[Reg1]
      Meta.readersOf(Integer, "").filter(_.scalaType == "doobie.util.metaspec.Reg1").size must_== 1
    }

    "not register multiple equivalent instances" in {
      Meta[Reg2]
      Meta[Reg2]
      Meta[Reg2]
      Meta.readersOf(Integer, "").filter(_.scalaType == "doobie.util.metaspec.Reg2").size must_== 1
    }

    "not allow xmap to observe null on the read side (AnyRef)" in {
      implicit def FooMeta: Meta[Foo] = Meta[String].xmap(s => Foo(s.toUpperCase), _.s)
      val x = sql"select null".query[Option[Foo]].unique.transact(xa).unsafePerformIO
      x must_== None
    }

    "not allow xmap to observe null on the read side (AnyVal)" in {
      implicit def FooMeta: Meta[Bar] = Meta[Int].xmap(n => if (n == 0) sys.error("") else Bar(n), _.n)
      val x = sql"select null".query[Option[Bar]].unique.transact(xa).unsafePerformIO
      x must_== None
    }

  }

}
