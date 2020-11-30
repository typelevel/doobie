// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.{ ContextShift, Effect, IO }
import cats.effect.syntax.effect._
import cats.syntax.applicativeError._
import doobie._, doobie.implicits._
import doobie.enum.JdbcType.{ Array => _, _ }
import scala.concurrent.ExecutionContext
import shapeless.test._

object GetSuite {
  final case class Y(x: String) extends AnyVal
  final case class P(x: Int) extends AnyVal
}

class GetSuite extends munit.FunSuite {
  import GetSuite._

  case class X(x: Int)
  case class Q(x: String)

  case class Z(i: Int, s: String)
  object S

  test("Get should exist for primitive types") {
    Get[Int]
    Get[String]
    true
  }

  test("Get should be derived for unary products") {
    Get[X]
    Get[Y]
    Get[P]
    Get[Q]
    true
  }

  test("Get should not be derived for non-unary products") {
    illTyped("Get[Z]")
    illTyped("Get[(Int, Int)]")
    illTyped("Get[S.type]")
    true
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
