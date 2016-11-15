package doobie.util

#+scalaz
import scalaz._
import scalaz.concurrent._
import Scalaz._
#-scalaz
#+cats
import scala.util.{ Left => -\/, Right => \/- }
#-cats
import doobie.imports._
import doobie.util.log.{ LogEvent, Success, ProcessingFailure, ExecFailure }
import org.specs2.mutable.Specification
import org.specs2.matcher.MatchResult
import shapeless._


object logspec extends Specification {

  val xa = DriverManagerTransactor[IOLite](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  def eventForUniqueQuery[A: Composite](sql: String, arg: A = HNil : HNil): LogEvent[A] = {
    var result  = null : LogEvent[A]
    val handler = LogHandler[A](result = _)
    val cio     = Query[A, HNil](sql, None, handler).unique(arg)
    cio.transact(xa).attempt.unsafePerformIO
    result
  }

  "query" >> {

    "zero-arg success" in {
      val Sql = "select 1"
      eventForUniqueQuery(Sql) must beLike {
        case Success(Sql, HNil, _, _) => ok
      }
    }

    "n-arg success" in {
      val Sql = "select 1 where ? = ?"
      val Arg = 1 :: 1 :: HNil
      eventForUniqueQuery(Sql, Arg) must beLike {
        case Success(Sql, Arg, _, _) => ok
      }
    }

    "zero-arg processing failure" in {
      val Sql = "select 1 where 1 = 2"
      eventForUniqueQuery(Sql) must beLike {
        case ProcessingFailure(Sql, HNil, _, _, _) => ok
      }
    }

    "n-arg processing failure" in {
      val Sql = "select 1 where ? = ?"
      val Arg = 1 :: 2 :: HNil
      eventForUniqueQuery(Sql, Arg) must beLike {
        case ProcessingFailure(Sql, Arg, _, _, _) => ok
      }
    }

  }
}
