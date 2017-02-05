package doobie.postgres

import doobie.imports._

import org.specs2.mutable.Specification
import scala.concurrent.duration._

#+cats
import fs2.interop.cats._
#-cats

object manyrows extends Specification {

  val xa = DriverManagerTransactor[IOLite](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  "select" should {

    // TODO add timeout to test the server-side cursor
    "take consistent memory" in {
      val q = sql"""select a.name, b.name from city a, city b""".query[(String, String)]
      q.process.take(5).transact(xa).run.unsafePerformIO
      true
    }
  }

}
