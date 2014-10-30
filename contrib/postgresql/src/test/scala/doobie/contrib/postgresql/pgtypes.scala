package doobie.contrib.postgresql

import doobie.hi.connection.prepareStatement
import doobie.hi.preparedstatement.executeQuery
import doobie.hi.resultset.getUnique
import doobie.util.transactor.DriverManagerTransactor
import doobie.util.composite.Composite
import doobie.contrib.postgresql.pgtypes._

import java.net.InetAddress
import java.util.UUID

import org.postgresql.util._
import org.postgresql.geometric._
import org.specs2.mutable.Specification

import scalaz.concurrent.Task

object pgtypesspec extends Specification {

  val xa = DriverManagerTransactor[Task](
    "org.postgresql.Driver", 
    "jdbc:postgresql:world", 
    "rnorris", ""
  )

  implicit class CrazyStringOps(e: String) {
    def as[A: Composite]: A =
      xa.transact(prepareStatement(s"SELECT $e")(executeQuery(getUnique[A]))).run
  }

  "Geometric Types" >> {
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

  "Monetary Types" >> {
    "money   → PGmoney" in { 
      skipped("* seems not to work; comes back a DOUBLE ")
    }
  }

  "UUID Type" >> {
    "uuid    → UUID" in {
      "'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid".as[UUID] must_== UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    }
  }

  "Network Address Types" >>  {
    "inet    → InetAddress" in {
      "'123.45.67.8'::inet".as[InetAddress] must_== InetAddress.getByName("123.45.67.8")
    }
    "inet    → ???" in skipped("* No suitable JDK Type")
    "macaddr → ???" in skipped("* No suitable JDK Type")
  }

  "Array Types" >> {
    "bit[]              → Array[Boolean]" in {
      "'{1, 0}'::bit[]".as[List[Boolean]] must_== List[Boolean](true, false)
    }
    "???                → Array[Byte] " in skipped(" * No byte type; use a binary type.")
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

}