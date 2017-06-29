package doobie.issue

#+scalaz
import scalaz._
import doobie.util.capture.Capture
#-scalaz
#+cats
import cats.Monad
import cats.implicits._
import fs2.util.{ Catchable, Suspendable => Capture }
import fs2.interop.cats._
#-cats
import doobie.imports._
import org.specs2.mutable.Specification
import Predef._

object `262` extends Specification {

  // an interpreter that returns null when we ask for statement metadata
  object Interp extends KleisliInterpreter[IOLite] {
    val M = implicitly[Monad[IOLite]]
    val C = implicitly[Capture[IOLite]]
    val K = implicitly[Catchable[IOLite]]

    override lazy val PreparedStatementInterpreter =
      new PreparedStatementInterpreter {
        override def getMetaData = primitive(_ => null)
      }

  }

  val baseXa = DriverManagerTransactor[IOLite](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  // A transactor that uses our interpreter above
  val xa: Transactor[IOLite, _] =
    Transactor.interpret.set(baseXa, Interp.ConnectionInterpreter)

  "getColumnJdbcMeta" should {
    "handle null metadata" in {
      val prog = HC.prepareStatement("select 1")(HPS.getColumnJdbcMeta)
      prog.transact(xa).unsafePerformIO must_== Nil
    }
  }

}
