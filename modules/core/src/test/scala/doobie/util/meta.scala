<<<<<<< HEAD
<<<<<<< HEAD
// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.IO
import doobie._, doobie.implicits._
import doobie.enum.JdbcType.{ Array => _, _ }
import org.specs2.mutable.Specification
import shapeless.test._

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Equals"))
object metaspec extends Specification {
  final case class X(x: Int)
  final case class Y(x: String) extends AnyVal
  final case class P(x: Int) extends AnyVal
  final case class Q(x: String)

  final case class Z(i: Int, s: String)
  object S

  final case class Reg1(x: Int)
  final case class Reg2(x: Int)

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  final case class Foo(s: String)
  final case class Bar(n: Int)

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
      val x = sql"select null".query[Option[Foo]].unique.transact(xa).unsafeRunSync
      x must_== None
    }

    "read non-null value (AnyRef)" in {
      val x = sql"select 'abc'".query[Foo].unique.transact(xa).unsafeRunSync
      x must_== Foo("ABC")
    }

    "throw when reading a NULL into an unlifted Scala type (AnyRef)" in {
      def x = sql"select null".query[Foo].unique.transact(xa).unsafeRunSync
      x must throwA[doobie.util.invariant.NonNullableColumnRead]
    }


    "not allow xmap to observe null on the read side (AnyVal)" in {
      val x = sql"select null".query[Option[Bar]].unique.transact(xa).unsafeRunSync
      x must_== None
    }

    "read non-null value (AnyVal)" in {
      val x = sql"select 1".query[Bar].unique.transact(xa).unsafeRunSync
      x must_== Bar(1)
    }
=======
// // Copyright (c) 2013-2017 Rob Norris
// // This software is licensed under the MIT License (MIT).
// // For more information see LICENSE or https://opensource.org/licenses/MIT
=======
// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT
>>>>>>> remove mentions of Meta in code, resurrect/split metaspec

package doobie.util

import cats.effect.IO
import doobie._, doobie.implicits._
import doobie.enum.JdbcType.{ Array => _, _ }
import org.specs2.mutable.Specification
import shapeless.test._

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Equals"))
object metaspec extends Specification {

  "Meta" should {

    "exist for primitive types" in {
      Meta[Int]
      Meta[String]
      true
    }

    "imply Get" in {
      def foo[A: Meta] = Get[A]
      true
    }

    "imply Put" in {
      def foo[A: Meta] = Put[A]
      true
    }

  }

<<<<<<< HEAD
//     "exist for primitive types" in {
//       Meta[Int]
//       Meta[String]

//       true
//     }

//     "be derived for unary products" in {
//       Meta[X]
//       Meta[Y]
//       Meta[P]
//       Meta[Q]

//       true
//     }

//     "not be derived for non-unary products" in {
//       illTyped("Meta[Z]")
//       illTyped("Meta[(Int, Int)]")
//       illTyped("Meta[S.type]")

//       true
//     }

//     "register new instances" in {
//       Meta[Reg1]
//       Meta.readersOf(Integer, "").filter(_.scalaType == "doobie.util.metaspec.Reg1").size must_== 1
//     }

//     "not register multiple equivalent instances" in {
//       Meta[Reg2]
//       Meta[Reg2]
//       Meta[Reg2]
//       Meta.readersOf(Integer, "").filter(_.scalaType == "doobie.util.metaspec.Reg2").size must_== 1
//     }

//     // Both of these will fail at runtime if called with a null value, we check that this is
//     // avoided below.
//     implicit def FooMeta: Meta[Foo] = Meta[String].xmap(s => Foo(s.toUpperCase), _.s)
//     implicit def barMeta: Meta[Bar] = Meta[Int].xmap(n => if (n == 0) sys.error("") else Bar(n), _.n)

//     "not allow xmap to observe null on the read side (AnyRef)" in {
//       val x = sql"select null".query[Option[Foo]].unique.transact(xa).unsafeRunSync
//       x must_== None
//     }

//     "read non-null value (AnyRef)" in {
//       val x = sql"select 'abc'".query[Foo].unique.transact(xa).unsafeRunSync
//       x must_== Foo("ABC")
//     }

//     "throw when reading a NULL into an unlifted Scala type (AnyRef)" in {
//       def x = sql"select null".query[Foo].unique.transact(xa).unsafeRunSync
//       x must throwA[doobie.util.invariant.NonNullableColumnRead]
//     }


//     "not allow xmap to observe null on the read side (AnyVal)" in {
//       val x = sql"select null".query[Option[Bar]].unique.transact(xa).unsafeRunSync
//       x must_== None
//     }

//     "read non-null value (AnyVal)" in {
//       val x = sql"select 1".query[Bar].unique.transact(xa).unsafeRunSync
//       x must_== Bar(1)
//     }
>>>>>>> tests pass

//     "throw when reading a NULL into an unlifted Scala type (AnyVal)" in {
//       def x = sql"select null".query[Bar].unique.transact(xa).unsafeRunSync
//       x must throwA[doobie.util.invariant.NonNullableColumnRead]
//     }

//   }


// }
=======
}
>>>>>>> remove mentions of Meta in code, resurrect/split metaspec
