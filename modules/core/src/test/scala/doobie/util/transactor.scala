package doobie.util

import cats.effect.{ Async, IO }
import doobie.imports._
import org.specs2.mutable.Specification
import Predef._

object transactorspec extends Specification {

  val q = sql"select 42".query[Int].unique

  def xa[A[_]: Async] = Transactor.fromDriverManager[A](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  "transactor" should {

    "support cats.effect.IO" in {
      q.transact(xa[IO]).unsafeRunSync must_=== 42
    }

  }

}
