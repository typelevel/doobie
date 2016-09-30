package doobie.util

#+scalaz
import scalaz.{ Catchable, Monad }
import scalaz.concurrent.Task
#-scalaz
#+fs2
import fs2.Task
import fs2.util.{ Catchable, Suspendable }
import fs2.interop.cats._
import scala.util.{ Left => -\/, Right => \/- }
#-fs2
import doobie.imports._
import org.specs2.mutable.Specification
import Predef._

object transactorspec extends Specification {

  val q = sql"select 42".query[Int].unique

#+scalaz
  def xa[A[_]: Monad: Catchable: Capture] = DriverManagerTransactor[A](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )
#-scalaz
#+fs2
  def xa[A[_]: Catchable: Suspendable] = DriverManagerTransactor[A](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )
#-fs2

  "transactor" should {

    "support doobie.util.IOLite" in {
      q.transact(xa[IOLite]).unsafePerformIO must_=== 42
    }

#+scalaz
    "support scalaz.concurrent.Task" in {
      q.transact(xa[Task]).unsafePerformSync must_=== 42
    }
#-scalaz

#+fs2
    "support fs2.Task" in {
      q.transact(xa[Task]).unsafeRunSync must_=== \/-(42)
    }
#-fs2
  }

}
