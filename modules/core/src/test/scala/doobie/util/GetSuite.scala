// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.enumerated.JdbcType.{Array => _, _}

import scala.annotation.nowarn

class GetSuite extends munit.FunSuite with GetSuitePlatform {

  case class X(x: Int)
  case class Q(x: String)

  case class Z(i: Int, s: String)
  object S

  test("Get should exist for primitive types") {
    Get[Int]
    Get[String]
  }

  test("Get should be derived for unary products") {
    Get[X]
    Get[Q]
  }

  test("Get should not be derived for non-unary products") {
    compileErrors("Get[Z]")
    compileErrors("Get[(Int, Int)]")
    compileErrors("Get[S.type]")
  }: @nowarn("msg=.*pure expression does nothing in statement position.*")

}

final case class Foo(s: String)
final case class Bar(n: Int)


class GetDBSuite extends munit.FunSuite {

  import cats.effect.unsafe.implicits.global

  lazy val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  // Both of these will fail at runtime if called with a null value, we check that this is
  // avoided below.
  implicit def FooMeta: Get[Foo] = Get[String].map(s => Foo(s.toUpperCase))
  implicit def barMeta: Get[Bar] = Get[Int].temap(n => if (n == 0) Left("cannot be 0") else Right(Bar(n)))

  test("Get should not allow map to observe null on the read side (AnyRef)") {
    val x = sql"select null".query[Option[Foo]].unique.transact(xa).unsafeRunSync()
    assertEquals(x, None)
  }

  test("Get should read non-null value (AnyRef)") {
    val x = sql"select 'abc'".query[Foo].unique.transact(xa).unsafeRunSync()
    assertEquals(x, Foo("ABC"))
  }

  test("Get should error when reading a NULL into an unlifted Scala type (AnyRef)") {
    def x = sql"select null".query[Foo].unique.transact(xa).attempt.unsafeRunSync()
    assertEquals(x, Left(doobie.util.invariant.NonNullableColumnRead(1, Char)))
  }

  test("Get should not allow map to observe null on the read side (AnyVal)") {
    val x = sql"select null".query[Option[Bar]].unique.transact(xa).unsafeRunSync()
    assertEquals(x, None)
  }

  test("Get should read non-null value (AnyVal)") {
    val x = sql"select 1".query[Bar].unique.transact(xa).unsafeRunSync()
    assertEquals(x, Bar(1))
  }

  test("Get should error when reading a NULL into an unlifted Scala type (AnyVal)") {
    def x = sql"select null".query[Bar].unique.transact(xa).attempt.unsafeRunSync()
    assertEquals(x, Left(doobie.util.invariant.NonNullableColumnRead(1, Integer)))
  }

  test("Get should error when reading an incorrect value") {
    def x = sql"select 0".query[Bar].unique.transact(xa).attempt.unsafeRunSync()
    assertEquals(x, Left(doobie.util.invariant.InvalidValue[Int, Bar](0, "cannot be 0")))
  }

}
