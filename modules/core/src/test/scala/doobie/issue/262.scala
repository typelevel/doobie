// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.issue

import cats.effect.{ Async, Blocker, IO }
import doobie._, doobie.implicits._
import scala.concurrent.ExecutionContext
import Predef._


object `262` extends H2Spec {

  // an interpreter that returns null when we ask for statement metadata
  object Interp extends KleisliInterpreter[IO] {
    val asyncM = Async[IO]
    val blocker = Blocker.liftExecutionContext(ExecutionContext.global)
    val contextShiftM = contextShift

    val M = implicitly[Async[IO]]

    override lazy val PreparedStatementInterpreter =
      new PreparedStatementInterpreter {
        override def getMetaData = primitive(_ => null)
      }

  }

  // A transactor that uses our interpreter above
  val instrumented: Transactor[IO] =
    Transactor.interpret.set(xa, Interp.ConnectionInterpreter)

  "getColumnJdbcMeta" should {
    "handle null metadata" in {
      val prog = HC.prepareStatement("select 1")(HPS.getColumnJdbcMeta)
      prog.transact(instrumented).unsafeRunSync must_== Nil
    }
  }

}
