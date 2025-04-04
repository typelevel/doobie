// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.postgres.enums.*
import doobie.util.invariant.*

class ReadErrorSuite extends munit.CatsEffectSuite {
  import PostgresTestTransactor.xa

  implicit val MyEnumMetaOpt: Meta[MyEnum] = pgEnumStringOpt(
    "myenum",
    {
      case "foo" => Some(MyEnum.Foo)
      case "bar" => Some(MyEnum.Bar)
      case _     => None
    },
    {
      case MyEnum.Foo => "foo"
      case MyEnum.Bar => "bar"
    })
  implicit val MyScalaEnumMeta: Meta[MyScalaEnum.Value] = pgEnum(MyScalaEnum, "myenum")
  implicit val MyJavaEnumMeta: Meta[MyJavaEnum] = pgJavaEnum[MyJavaEnum]("myenum")

  test("pgEnumStringOpt") {
    sql"select 'invalid'".query[MyEnum].unique.transact(xa).attempt
      .assertEquals(Left(InvalidEnum[MyEnum]("invalid")))
  }

  test("pgEnum") {
    sql"select 'invalid' :: myenum".query[MyScalaEnum.Value].unique.transact(xa).attempt
      .assertEquals(Left(InvalidEnum[MyScalaEnum.Value]("invalid")))
  }

  test("pgJavaEnum") {
    sql"select 'invalid' :: myenum".query[MyJavaEnum].unique.transact(xa).attempt
      .assertEquals(Left(InvalidEnum[MyJavaEnum]("invalid")))
  }

}
