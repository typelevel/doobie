// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect.IO
import doobie._, doobie.implicits._
import doobie.postgres.enums._
import org.specs2.mutable.Specification


class pgcheck extends Specification {

  import cats.effect.unsafe.implicits.global

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  "pgEnumString" should {

    "check ok for read" in {
      val a = sql"select 'foo' :: myenum".query[MyEnum].analysis.transact(xa).unsafeRunSync()
      a.columnTypeErrors must_== Nil
    }

    "check ok for write" in {
      val a = sql"select ${MyEnum.Foo : MyEnum} :: myenum".query[MyEnum].analysis.transact(xa).unsafeRunSync()
      a.parameterTypeErrors must_== Nil
    }

  }

}
