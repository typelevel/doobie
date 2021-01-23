// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.{ ContextShift, Effect, IO }
import cats.effect.syntax.effect._
import cats.syntax.applicativeError._
import doobie._, doobie.implicits._
import doobie.enumerated.JdbcType.{ Array => _, _ }
import scala.concurrent.ExecutionContext

class GetSuite extends munit.FunSuite with GetSuitePlatform {

  case class Yes1(x: Int)
  case class Yes2(x: Yes1)

  case class No1(i: Int, s: String)
  object No2
  case class Y private (x: String)
  class No3(val x: Int)
  class No4(x: Int)

  test("Get should exist for primitive types") {
    Get[Int]
    Get[String]
  }

  test("Get should be derived for unary products") {
    Get[Yes1]
    Get[Yes2]
  }

  test("Get should not be derived for non-unary products") {
    compileErrors("Get[(Int, Int)]")
    compileErrors("Get[No1]")
    compileErrors("Get[No1.type]")
    compileErrors("Get[No2.type]")
    compileErrors("Get[No3.type]")
    compileErrors("Get[No4.type]")
  }
}

final case class Foo(s: String)
final case class Bar(n: Int)


class GetDBSuiteIO extends GetDBSuite[IO] {
  implicit val E: Effect[IO] = IO.ioEffect
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
}

trait GetDBSuite[F[_]] extends munit.FunSuite {

  implicit def E: Effect[F]
  implicit def contextShift: ContextShift[F]

  lazy val xa = Transactor.fromDriverManager[F](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  // Both of these will fail at runtime if called with a null value, we check that this is
  // avoided below.
  implicit def FooMeta: Get[Foo] = Get[String].map(s => Foo(s.toUpperCase))
  implicit def barMeta: Get[Bar] = Get[Int].temap(n => if (n == 0) Left("cannot be 0") else Right(Bar(n)))

  test("Get should not allow map to observe null on the read side (AnyRef)") {
    val x = sql"select null".query[Option[Foo]].unique.transact(xa).toIO.unsafeRunSync()
    assertEquals(x, None)
  }

  test("Get should read non-null value (AnyRef)") {
    val x = sql"select 'abc'".query[Foo].unique.transact(xa).toIO.unsafeRunSync()
    assertEquals(x, Foo("ABC"))
  }

  test("Get should error when reading a NULL into an unlifted Scala type (AnyRef)") {
    def x = sql"select null".query[Foo].unique.transact(xa).attempt.toIO.unsafeRunSync()
    assertEquals(x, Left(doobie.util.invariant.NonNullableColumnRead(1, Char)))
  }

  test("Get should not allow map to observe null on the read side (AnyVal)") {
    val x = sql"select null".query[Option[Bar]].unique.transact(xa).toIO.unsafeRunSync()
    assertEquals(x, None)
  }

  test("Get should read non-null value (AnyVal)") {
    val x = sql"select 1".query[Bar].unique.transact(xa).toIO.unsafeRunSync()
    assertEquals(x, Bar(1))
  }

  test("Get should error when reading a NULL into an unlifted Scala type (AnyVal)") {
    def x = sql"select null".query[Bar].unique.transact(xa).attempt.toIO.unsafeRunSync()
    assertEquals(x, Left(doobie.util.invariant.NonNullableColumnRead(1, Integer)))
  }

  test("Get should error when reading an incorrect value") {
    def x = sql"select 0".query[Bar].unique.transact(xa).attempt.toIO.unsafeRunSync()
    assertEquals(x, Left(doobie.util.invariant.InvalidValue[Int, Bar](0, "cannot be 0")))
  }

}
