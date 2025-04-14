// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.IO
import doobie.enumerated.JdbcType
import doobie.testutils.VoidExtensions
import doobie.util.transactor.Transactor

trait TransactorProvider {
  lazy val xa = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )
}

class GetSuite extends munit.CatsEffectSuite with GetSuitePlatform {

  case class X(x: Int)
  case class Q(x: String)

  case class Z(i: Int, s: String)
  object S

  test("Get should exist for primitive types") {
    Get[Int].void
    Get[String].void
  }

}

final case class Foo(s: String)
final case class Bar(n: Int)

class GetDBSuite extends munit.CatsEffectSuite with TransactorProvider with GetDBSuitePlatform {
  import doobie.syntax.all.*

  // Both of these will fail at runtime if called with a null value, we check that this is
  // avoided below.
  implicit val FooMeta: Get[Foo] = Get[String].map(s => Foo(s.toUpperCase))
  implicit val barMeta: Get[Bar] = Get[Int].temap(n => if (n == 0) Left("cannot be 0") else Right(Bar(n)))

  test("Get should not allow map to observe null on the read side (AnyRef)") {
    sql"select null".query[Option[Foo]].unique.transact(xa).assertEquals(None)
  }

  test("Get should read non-null value (AnyRef)") {
    sql"select 'abc'".query[Foo].unique.transact(xa).assertEquals(Foo("ABC"))
  }

  test("Get should error when reading a NULL into an unlifted Scala type (AnyRef)") {
    sql"select null".query[Foo].unique.transact(xa).attempt.assertEquals(Left(
      doobie.util.invariant.NonNullableColumnRead(1, JdbcType.Char)))
  }

  test("Get should error when reading an incorrect value") {
    sql"select 0".query[Bar].unique.transact(xa).attempt.assertEquals(Left(doobie.util.invariant.InvalidValue[Int, Bar](
      0,
      "cannot be 0")))
  }

}
