// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.issue

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import munit.CatsEffectSuite

class `262` extends CatsEffectSuite {
  
  // an interpreter that returns null when we ask for statement metadata
  object Interp extends KleisliInterpreter[IO](LogHandler.noop) {
    override lazy val PreparedStatementInterpreter: PreparedStatementInterpreter =
      new PreparedStatementInterpreter {
        override def getMetaData = primitive(_ => null)
      }

  }

  val baseXa = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  // A transactor that uses our interpreter above
  val xa: Transactor[IO] =
    Transactor.interpret.set(baseXa, Interp.ConnectionInterpreter)

  test("getColumnJdbcMeta should handle null metadata") {
    val prog = HC.prepareStatementPrimitive("select 1")(HPS.getColumnJdbcMeta)
    prog.transact(xa).assertEquals(Nil)
  }

}
