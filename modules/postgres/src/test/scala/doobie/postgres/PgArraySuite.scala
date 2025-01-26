// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect.IO
import doobie.Transactor
import doobie.postgres.enums.{MyEnum, OtherEnum}
import doobie.postgres.implicits.*
import doobie.syntax.all.*
import doobie.util.analysis.{ColumnTypeError, ParameterTypeError}

class PgArraySuite extends munit.CatsEffectSuite {

  val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql:world",
    user = "postgres",
    password = "password",
    logHandler = None
  )

  private val listOfMyEnums: List[MyEnum] = List(MyEnum.Foo, MyEnum.Bar)

  private val listOfOtherEnums: List[OtherEnum] = List(OtherEnum.A, OtherEnum.B)

  test("array of custom string type: read correctly and typechecks") {
    val q = sql"select array['foo', 'bar'] :: myenum[]".query[List[MyEnum]]
    (for {
      _ <- q.analysis
        .map(ana => assertEquals(ana.columnAlignmentErrors, List.empty))

      _ <- q.unique.map(assertEquals(_, listOfMyEnums))

      _ <- sql"select array['foo', 'bar']".query[List[MyEnum]].analysis.map(_.columnAlignmentErrors)
        .map {
          case List(e: ColumnTypeError) => assertEquals(e.schema.vendorTypeName, "_text")
          case other                    => fail(s"Unexpected typecheck result: $other")
        }
    } yield ())
      .transact(transactor)
  }

  test("array of custom string type: writes correctly and typechecks") {
    val q = sql"insert into temp_myenum (arr) values ($listOfMyEnums)".update
    (for {
      _ <- sql"drop table if exists temp_myenum".update.run
      _ <- sql"create table temp_myenum(arr myenum[] not null)".update.run
      _ <- q.analysis.map(_.columnAlignmentErrors).map(ana => assertEquals(ana, List.empty))
      _ <- q.run
      _ <- sql"select arr from temp_myenum".query[List[MyEnum]].unique
        .map(assertEquals(_, listOfMyEnums))

      _ <- sql"insert into temp_myenum (arr) values (${List("foo")})".update.analysis
        .map(_.parameterAlignmentErrors)
        .map {
          case List(e: ParameterTypeError) => assertEquals(e.vendorTypeName, "_myenum")
          case other                       => fail(s"Unexpected typecheck result: $other")
        }
    } yield ())
      .transact(transactor)
  }

  test("array of custom type in another schema: read correctly and typechecks") {
    val q = sql"select array['a', 'b'] :: other_schema.other_enum[]".query[List[OtherEnum]]
    (for {
      _ <- q.analysis
        .map(ana => assertEquals(ana.columnAlignmentErrors, List.empty))

      _ <- q.unique.map(assertEquals(_, listOfOtherEnums))

      _ <- sql"select array['a', 'b']".query[List[OtherEnum]].analysis.map(_.columnAlignmentErrors)
        .map {
          case List(e: ColumnTypeError) => assertEquals(e.schema.vendorTypeName, "_text")
          case other                    => fail(s"Unexpected typecheck result: $other")
        }

      _ <- sql"select array['a', 'b'] :: other_schema.other_enum[]".query[List[String]].analysis.map(
        _.columnAlignmentErrors)
        .map {
          case List(e: ColumnTypeError) => assertEquals(e.schema.vendorTypeName, """"other_schema"."_other_enum"""")
          case other                    => fail(s"Unexpected typecheck result: $other")
        }
    } yield ())
      .transact(transactor)
  }

  test("array of custom type in another schema: writes correctly and typechecks") {
    val q = sql"insert into temp_otherenum (arr) values ($listOfOtherEnums)".update
    (for {
      _ <- sql"drop table if exists temp_otherenum".update.run
      _ <- sql"create table temp_otherenum(arr other_schema.other_enum[] not null)".update.run
      _ <- q.analysis.map(_.parameterAlignmentErrors).map(ana => assertEquals(ana, List.empty))
      _ <- q.run
      _ <- sql"select arr from temp_otherenum".query[List[OtherEnum]].to[List]
        .map(assertEquals(_, List(listOfOtherEnums)))

      _ <- sql"insert into temp_otherenum (arr) values (${List("a")})".update.analysis
        .map(_.parameterAlignmentErrors)
        .map {
          case List(e: ParameterTypeError) => {
            // pgjdbc is a bit crazy. If you have inserted into the table already then it'll report the parameter type as
            // _other_enum, or otherwise "other_schema"."_other_enum"..
            assertEquals(e.vendorTypeName, "_other_enum")
            // assertEquals(e.vendorTypeName, s""""other_schema"."_other_enum"""")
          }
          case other => fail(s"Unexpected typecheck result: $other")
        }
    } yield ())
      .transact(transactor)
  }

}
