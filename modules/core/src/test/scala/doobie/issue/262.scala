package doobie.issue

import cats.Monad
import cats.implicits._
import cats.effect.{ Async, IO }
import doobie._, doobie.implicits._
import org.specs2.mutable.Specification
import Predef._

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object `262` extends Specification {

  // an interpreter that returns null when we ask for statement metadata
  object Interp extends KleisliInterpreter[IO] {
    val M = implicitly[Async[IO]]

    override lazy val PreparedStatementInterpreter =
      new PreparedStatementInterpreter {
        override def getMetaData = primitive(_ => null)
      }

  }

  val baseXa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  // A transactor that uses our interpreter above
  val xa: Transactor[IO] =
    Transactor.interpret.set(baseXa, Interp.ConnectionInterpreter)

  "getColumnJdbcMeta" should {
    "handle null metadata" in {
      val prog = HC.prepareStatement("select 1")(HPS.getColumnJdbcMeta)
      prog.transact(xa).unsafeRunSync must_== Nil
    }
  }

}
