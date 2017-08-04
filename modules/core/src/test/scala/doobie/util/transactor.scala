package doobie.util

import fs2.Task
import fs2.util.{ Catchable, Suspendable }
import fs2.interop.cats._
import scala.util.{ Left => -\/, Right => \/- }
import doobie.imports._
import org.specs2.mutable.Specification
import Predef._

object transactorspec extends Specification {

  val q = sql"select 42".query[Int].unique

  def xa[A[_]: Catchable: Suspendable] = DriverManagerTransactor[A](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  "transactor" should {

    "support doobie.util.IOLite" in {
      q.transact(xa[IOLite]).unsafePerformIO must_=== 42
    }


    "support fs2.Task" in {
      q.transact(xa[Task]).unsafeRunSync must_=== \/-(42)
    }
  }

}
