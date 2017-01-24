package doobie.refined

import org.specs2.mutable.Specification
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto._
import eu.timepit.refined._
import doobie.imports._

object refinedtypes extends Specification {

  val xa = DriverManagerTransactor[IOLite](
    "org.h2.Driver",
    "jdbc:h2:mem:refined;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  type PositiveInt = Int Refined Positive

  "Meta" should {
    "exist for refined types" in {
      Meta[PositiveInt]

      true
    }
  }

  "Query" should {
    "return a refined type when conversion is possible" in {
      val pInt: PositiveInt =
        Query[Unit,PositiveInt]("select 123", None).unique(()).transact(xa).unsafePerformIO

      true
    }
  }

}
