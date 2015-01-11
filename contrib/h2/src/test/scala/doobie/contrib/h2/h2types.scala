package doobie.contrib.h2

import doobie.hi.connection.prepareStatement
import doobie.hi.preparedstatement.executeQuery
import doobie.hi.resultset.getUnique
import doobie.util.transactor.DriverManagerTransactor
import doobie.util.composite.Composite
import doobie.syntax.connectionio._
import doobie.contrib.h2.h2types._

import java.net.InetAddress
import java.util.UUID

import org.specs2.mutable.Specification

import scalaz.concurrent.Task

// Establish that we can read various types. It's not very comprehensive as a test, bit it's a start.
object h2typesspec extends Specification {

  val xa = DriverManagerTransactor[Task](
    "org.h2.Driver",                     
    "jdbc:h2:mem:ch3;DB_CLOSE_DELAY=-1",  
    "sa", ""                              
  )

  implicit class CrazyStringOps(e: String) {
    def as[A: Composite]: A =
      prepareStatement(s"SELECT $e")(executeQuery(getUnique[A])).transact(xa).run
  }

  "Data Types" >> {
    "INT       → Int"          in { "123::INT".as[Int] must_== 123 }
    "BOOLEAN   → Boolean"      in { "TRUE".as[Boolean] must_== true }
    "TINYINT   → Byte"         in { "123".as[Byte] must_== (123 : Byte) }
    "SMALLINT  → Short"        in { "123::SMALLINT".as[Short] must_== (123 : Short) }
    "BIGINT    → Long"         in { "123::BIGINT".as[Long] must_== 123L }
    "DECIMAL   → BigDecimal"   in { "123.456::DECIMAL".as[BigDecimal] must_== BigDecimal("123.456") }
    "TIME      → Time"         in { skipped }
    "DATE      → Date"         in { skipped }
    "TIMESTAMP → Timestamp"    in { skipped }
    "BINARY    → Array[Byte]"  in { skipped }
    "OTHER     → Object"       in { skipped }
    "VARCHAR   → String"       in { "'abc'".as[String] must_== "abc" }
    "CHAR      → String"       in { "'abc'::CHAR(3)".as[String] must_== "abc" }
    "BLOB      → Blob"         in { skipped }
    "CLOB      → Clob"         in { skipped }
    "UUID      → UUID"         in { "'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::UUID".as[UUID] must_== UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11") }
    "ARRAY     → List[Int]"    in { "(1,2,3)".as[List[Integer]] must_== List(1,2,3)  }
    "ARRAY     → List[String]" in { "('foo', 'bar')".as[List[String]] must_== List("foo", "bar")  }
    "GEOMETRY  →"      in { skipped }
  }

}