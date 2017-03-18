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

    // Both of these will fail at runtime if called with a null value, we check that this is
    // avoided below.
    implicit def FooMeta: Meta[Foo] = Meta[String].xmap(s => Foo(s.toUpperCase), _.s)
    implicit def barMeta: Meta[Bar] = Meta[Int].xmap(n => if (n == 0) sys.error("") else Bar(n), _.n)

    "not allow xmap to observe null on the read side (AnyRef)" in {
      val x = sql"select null".query[Option[Foo]].unique.transact(xa).unsafePerformIO
      x must_== None
    }

    "read non-null value (AnyRef)" in {
      val x = sql"select 'abc'".query[Foo].unique.transact(xa).unsafePerformIO
      x must_== Foo("ABC")
    }

    "throw when reading a NULL into an unlifted Scala type (AnyRef)" in {
      def x = sql"select null".query[Foo].unique.transact(xa).unsafePerformIO
      x must throwA[doobie.util.invariant.NonNullableColumnRead]
    }


    "not allow xmap to observe null on the read side (AnyVal)" in {
      val x = sql"select null".query[Option[Bar]].unique.transact(xa).unsafePerformIO
      x must_== None
    }

    "read non-null value (AnyVal)" in {
      val x = sql"select 1".query[Bar].unique.transact(xa).unsafePerformIO
      x must_== Bar(1)
    }

    "throw when reading a NULL into an unlifted Scala type (AnyVal)" in {
      def x = sql"select null".query[Bar].unique.transact(xa).unsafePerformIO
      x must throwA[doobie.util.invariant.NonNullableColumnRead]
    }

  }


}
