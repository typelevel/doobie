// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect.{ ContextShift, IO }
import doobie._, doobie.implicits._
import doobie.postgres.enums._
import scala.concurrent.ExecutionContext


class CheckSuite extends munit.FunSuite {

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  test("pgEnumString check ok for read") {
    val a = sql"select 'foo' :: myenum".query[MyEnum].analysis.transact(xa).unsafeRunSync()
    assertEquals(a.columnTypeErrors, Nil)
  }

  test("pgEnumString check ok for write") {
    val a = sql"select ${MyEnum.Foo : MyEnum} :: myenum".query[MyEnum].analysis.transact(xa).unsafeRunSync()
    assertEquals(a.parameterTypeErrors, Nil)
  }

}
