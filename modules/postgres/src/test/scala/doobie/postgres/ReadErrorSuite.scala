// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect.{ Effect, IO }
import cats.effect.syntax.effect._
import cats.syntax.applicativeError._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.postgres.enums._
import doobie.util.invariant._
import scala.concurrent.ExecutionContext

class ReadErrorSuiteIO extends ReadErrorSuite[IO] {
  implicit val E: Effect[IO] = IO.ioEffect
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
}

trait ReadErrorSuite[F[_]] extends munit.FunSuite {

  implicit def E: Effect[F]
  implicit def contextShift: ContextShift[F]

  lazy val xa = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  implicit val MyEnumMetaOpt: Meta[MyEnum] = pgEnumStringOpt("myenum", {
    case "foo" => Some(MyEnum.Foo)
    case "bar" => Some(MyEnum.Bar)
    case _ => None
  }, {
    case MyEnum.Foo => "foo"
    case MyEnum.Bar => "bar"
  })
  implicit val MyScalaEnumMeta: Meta[MyScalaEnum.Value] = pgEnum(MyScalaEnum, "myenum")
  implicit val MyJavaEnumMeta: Meta[MyJavaEnum] = pgJavaEnum[MyJavaEnum]("myenum")

  test("pgEnumStringOpt") {
    val r = sql"select 'invalid'".query[MyEnum].unique.transact(xa).attempt.toIO.unsafeRunSync()
    assertEquals(r, Left(InvalidEnum[MyEnum]("invalid")))
  }

  test("pgEnum") {
    val r = sql"select 'invalid' :: myenum".query[MyScalaEnum.Value].unique.transact(xa).attempt.toIO.unsafeRunSync()
    assertEquals(r, Left(InvalidEnum[MyScalaEnum.Value]("invalid")))
  }

  test("pgJavaEnum") {
    val r = sql"select 'invalid' :: myenum".query[MyJavaEnum].unique.transact(xa).attempt.toIO.unsafeRunSync()
    assertEquals(r, Left(InvalidEnum[MyJavaEnum]("invalid")))
  }

}

