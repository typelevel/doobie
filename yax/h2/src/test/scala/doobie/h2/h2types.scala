package doobie.h2

import doobie.h2.imports._
import doobie.imports._


import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import org.specs2.mutable.Specification

#+scalaz
import scalaz.{ Maybe, \/- }
#-scalaz
#+cats
import scala.util.{ Left => -\/, Right => \/- }
import fs2.interop.cats._
#-cats

// Establish that we can read various types. It's not very comprehensive as a test, bit it's a start.
object h2typesspec extends Specification {

  val xa = DriverManagerTransactor[IOLite](
    "org.h2.Driver",
    "jdbc:h2:mem:ch3;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  def inOut[A: Atom](col: String, a: A) =
    for {
      _  <- Update0(s"CREATE LOCAL TEMPORARY TABLE TEST (value $col)", None).run
      _  <- sql"INSERT INTO TEST VALUES ($a)".update.run
      a0 <- sql"SELECT value FROM TEST".query[A].unique
    } yield (a0)

  def testInOut[A](col: String, a: A)(implicit m: Meta[A]) =
    s"Mapping for $col as ${m.scalaType}" >> {
      s"write+read $col as ${m.scalaType}" in {
        inOut(col, a).transact(xa).attempt.unsafePerformIO must_== \/-(a)
      }
      s"write+read $col as Option[${m.scalaType}] (Some)" in {
        inOut[Option[A]](col, Some(a)).transact(xa).attempt.unsafePerformIO must_== \/-(Some(a))
      }
      s"write+read $col as Option[${m.scalaType}] (None)" in {
        inOut[Option[A]](col, None).transact(xa).attempt.unsafePerformIO must_== \/-(None)
      }
#+scalaz
      s"write+read $col as Maybe[${m.scalaType}] (Just)" in {
        inOut[Maybe[A]](col, Maybe.just(a)).transact(xa).attempt.unsafePerformIO must_== \/-(Maybe.Just(a))
      }
      s"write+read $col as Maybe[${m.scalaType}] (Empty)" in {
        inOut[Maybe[A]](col, Maybe.empty[A]).transact(xa).attempt.unsafePerformIO must_== \/-(Maybe.Empty())
      }
#-scalaz
    }

  def skip(col: String, msg: String = "not yet implemented") =
    s"Mapping for $col" >> {
      "PENDING:" in pending(msg)
    }

  testInOut[Int]("INT", 123)
  testInOut[Boolean]("BOOLEAN", true)
  testInOut[Byte]("TINYINT",  123)
  testInOut[Short]("SMALLINT", 123)
  testInOut[Long]("BIGINT", 123)
  testInOut[BigDecimal]("DECIMAL", 123.45)
  testInOut[java.sql.Time]("TIME", new java.sql.Time(3,4,5))
  testInOut[java.sql.Date]("DATE", new java.sql.Date(4,5,6))
  testInOut[java.time.LocalDate]("DATE", java.time.LocalDate.of(4,5,6))
  testInOut[java.sql.Timestamp]("TIMESTAMP", new java.sql.Timestamp(System.currentTimeMillis))
  testInOut[java.time.Instant]("TIMESTAMP", java.time.Instant.now)
  testInOut[List[Byte]]("BINARY", BigInt("DEADBEEF",16).toByteArray.toList)
  skip("OTHER")
  testInOut[String]("VARCHAR", "abc")
  testInOut[String]("CHAR(3)", "abc")
  skip("BLOB")
  skip("CLOB")
  testInOut[UUID]("UUID", UUID.randomUUID)
  testInOut[List[Int]]("ARRAY", List(1,2,3))
  testInOut[List[String]]("ARRAY", List("foo", "bar"))
  skip("GEOMETRY")

  "Mapping for Boolean" should {
    "pass query analysis for unascribed 'true'" in {
      val a = sql"select true".query[Boolean].analysis.transact(xa).unsafePerformIO
      a.alignmentErrors must_== Nil
    }
    "pass query analysis for ascribed BIT" in {
      val a = sql"select true::BIT".query[Boolean].analysis.transact(xa).unsafePerformIO
      a.alignmentErrors must_== Nil
    }
    "pass query analysis for ascribed BOOLEAN" in {
      val a = sql"select true::BIT".query[Boolean].analysis.transact(xa).unsafePerformIO
      a.alignmentErrors must_== Nil
    }
  }

}
