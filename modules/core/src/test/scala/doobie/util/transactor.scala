package doobie.util

import scalaz.{ Catchable, Monad }
import scalaz.concurrent.Task
import doobie.imports._
import org.specs2.mutable.Specification
import Predef._

object transactorspec extends Specification {

  val q = sql"select 42".query[Int].unique

  def xa[A[_]: Monad: Catchable: Capture] = DriverManagerTransactor[A](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  "transactor" should {

    "support doobie.util.IOLite" in {
      q.transact(xa[IOLite]).unsafePerformIO must_=== 42
    }

    "support scalaz.concurrent.Task" in {
      q.transact(xa[Task]).unsafePerformSync must_=== 42
    }

  }

}
