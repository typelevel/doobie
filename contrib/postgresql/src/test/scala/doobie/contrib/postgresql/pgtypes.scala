package doobie.contrib.postgresql

import doobie.hi.connection.prepareStatement
import doobie.hi.preparedstatement.executeQuery
import doobie.hi.resultset.getUnique
import doobie.util.transactor.DriverManagerTransactor
import doobie.util.composite.Composite
import doobie.syntax.connectionio._
import doobie.contrib.postgresql.pgtypes._

import java.net.InetAddress
import java.util.UUID

import org.postgresql.util._
import org.postgresql.geometric._
import org.specs2.mutable.Specification

import scalaz.concurrent.Task

// Establish that we can read various types. It's not very comprehensive as a test, bit it's a start.
object pgtypesspec extends Specification {

  val xa = DriverManagerTransactor[Task](
    "org.postgresql.Driver", 
    "jdbc:postgresql:world", 
    "postgres", ""
  )

  implicit class CrazyStringOps(e: String) {
    def as[A: Composite]: A =
      prepareStatement(s"SELECT $e")(executeQuery(getUnique[A])).transact(xa).run
  }

  "8.1 Numeric Types" >> {
    "smallint         → Short"      in { "123::smallint".as[Short] must_== 123 }
    "integer          → Int"        in { "123::integer".as[Int] must_== 123 }
    "bigint           → Long"       in { "123::bigint".as[Long] must_== 123 }
    "decimal          → BigDecimal" in { "123.45::decimal".as[BigDecimal] must_== BigDecimal(123.45) }
    "numeric          → BigDecimal" in { "123.456::numeric".as[BigDecimal] must_== BigDecimal(123.456) }
    "real             → Float"      in { "123.456::real".as[Float] must_== 123.456f }
    "double precision → Double"     in { "123.456::double precision".as[Double] must_== 123.456 }
  }

  "8.2 Monetary Types" >> {
    "money             → PGmoney" in { 
      skipped("* seems not to work; comes back a DOUBLE ")
    }
  }

  "8.3 Character Types" >> {
    "character varying → String" in { "'abcdef'::character varying".as[String] must_== "abcdef"}
    "varchar           → String" in { "'abcdef'::varchar".as[String] must_== "abcdef"}
    "character         → String" in { "'abcdef'::character(6)".as[String] must_== "abcdef"}
    "char              → String" in { "'abcdef'::char(6)".as[String] must_== "abcdef"}
    "text              → String" in { "'abcdef'::text".as[String] must_== "abcdef"}
  }

  "8.4 Binary Types" >> {
    "bytea             → Array[Byte]" in { 
      """E'\\xDEADBEEF'::bytea""".as[List[Byte]] must_== 
        BigInt("DEADBEEF",16).toByteArray.dropWhile(_ == 0).toList 
    }
  }

  "8.5 Date/Time Types" >> {
    "timestamp               " in pending
    "timestamp with time zone" in pending
    "date                    " in pending
    "time                    " in pending
    "time with time zone     " in pending
    "interval                " in pending
  }

  "8.6 Boolean Type" >> {
    "boolean" in pending
  }

  "8.7 Enumerated Types" >> {
    "«example»" in pending
  }

  "8.8 Geometric Types" >> {
    "box     → PGbox" in { 
      "'((1,2),(3,4))'::box".as[PGbox] must_== new PGbox(new PGpoint(1, 2), new PGpoint(3, 4)) 
    }
    "circle  → PGcircle" in { 
      "'<(1,2),3>'::circle".as[PGcircle] must_== new PGcircle(new PGpoint(1, 2), 3) 
    }
    "lseg    → PGlseg" in { 
      "'((1,2),(3,4))'::lseg".as[PGlseg] must_== new PGlseg(new PGpoint(1, 2), new PGpoint(3, 4)) 
    }
    "path    → PGpath (c)" in { 
      "'((1,2),(3,4))'::path".as[PGpath] must_== new PGpath(Array(new PGpoint(1, 2), new PGpoint(3, 4)), false) 
    }
    "path    → PGpath (o)" in { 
      "'[(1,2),(3,4)]'::path".as[PGpath] must_== new PGpath(Array(new PGpoint(1, 2), new PGpoint(3, 4)), true) 
    }
    "point   → PGpoint" in { 
      "'(1,2)'::point".as[PGpoint] must_== new PGpoint(1, 2) 
    }
    "polygon → PGpolygon" in { 
      "'((1,2),(3,4))'::polygon".as[PGpolygon] must_==  new PGpolygon(Array(new PGpoint(1, 2), new PGpoint(3, 4))) 
    }
    "line    → PGline" in skipped("* doc says: \"not fully implemented\"")
  }

  "8.9 Network Address Types" >> {
    "inet    → InetAddress" in {
      "'123.45.67.8'::inet".as[InetAddress] must_== InetAddress.getByName("123.45.67.8")
    }
    "inet    → ???" in skipped("* No suitable JDK Type")
    "macaddr → ???" in skipped("* No suitable JDK Type")
  }

  "8.10 Bit String Types" >> {
    "bit        " in pending
    "bit varying" in pending
  }

  "8.11 Text Search Types" >> {
    "tsvector" in pending
    "tsquery " in pending
  }

  "8.12 UUID Type" >> {
    "uuid    → UUID" in {
      "'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid".as[UUID] must_== UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    }
  }

  "8.13 XML Type" >> {
    "xml" in pending
  }

  "8.14 JSON Type" >> {
    "json" in pending
  }

  "8.15 Arrays" >> {
    "bit[]              → Array[Boolean]" in {
      "'{1, 0}'::bit[]".as[List[Boolean]] must_== List[Boolean](true, false)
    }
    "???                → Array[Byte]  " in skipped("* No byte type; bytea.")
    "smallint[]         → Array[Short] " in skipped("* Oops always comes back as Array[Int]")
    "integer[]          → Array[Int]" in {
      "'{1,2}'::integer[]".as[List[Int]] must_== List[Int](1,2)
    }
    "bigint[]           → Array[Long]" in {
      "'{1,2}'::bigint[]".as[List[Long]] must_== List[Long](1,2)
    } 
    "real[]             → Array[Float]" in {
      "'{1.2,3.4}'::real[]".as[List[Float]] must_== List[Float](1.2f, 3.4f)
    }
    "double precision[] → Array[Double]" in {
      "'{1.2,3.4}'::double precision[]".as[List[Double]] must_== List[Double](1.2, 3.4)
    }
    "varchar[]          → Array[String]" in {
      "ARRAY['foo', 'bar']::varchar[]".as[List[String]] must_== List[String]("foo", "bar")
    } 
  }

  "8.16 Composite Types" >> {
    "composite" in pending
  }

  "8.17 Range Types" >> {
    "int4range" in pending
    "int8range" in pending
    "numrange " in pending
    "tsrange  " in pending
    "tstzrange" in pending
    "daterange" in pending
    "custom   " in pending
  }

  "8.18 Object Identifier Types" >> {
    "n/a" in skipped
  }

  "8.19 Pseudo-Types" >> {
    "n/a" in skipped
  }

}