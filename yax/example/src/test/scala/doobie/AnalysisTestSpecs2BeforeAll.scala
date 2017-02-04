package doobie.example

import doobie.imports._
import doobie.h2.imports._
import doobie.specs2.analysisspec._
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAll

#+cats
import fs2.interop.cats._
#-cats

// Check that AnalysisSpec plays nice with Specs2 execution flow (issue #454)
object AnalysisTestSpecs2BeforeAll extends Specification
    with AnalysisSpec with BeforeAll {
  // Setup
  val initQ = sql"create table some_table (value varchar not null)".update

  val targetQ = sql"select value from some_table".query[String]

  val transactor = H2Transactor[IOLite](
    "jdbc:h2:mem:",
    "sa",
    ""
  ).unsafePerformIO

  // The test itself
  check(targetQ)

  // A hook for database initialization
  def beforeAll() = {
    initQ.run
      .transact(transactor)
      .unsafePerformIO
    ()
  }
}
