// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import doobie.implicits._
import doobie.postgres.enums._

object pgcheck extends PgSpec {

  "pgEnumString" should {

    "check ok for read" in {
      val a = sql"select 'foo' :: myenum".query[MyEnum].analysis.transact(xa).unsafeRunSync
      a.columnTypeErrors must_== Nil
    }

    "check ok for write" in {
      val a = sql"select ${MyEnum.Foo : MyEnum} :: myenum".query[MyEnum].analysis.transact(xa).unsafeRunSync
      a.parameterTypeErrors must_== Nil
    }

  }

}
