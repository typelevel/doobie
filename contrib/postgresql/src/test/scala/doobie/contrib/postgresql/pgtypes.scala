package doobie.contrib.postgresql

import doobie.imports._
import doobie.contrib.postgresql.pgtypes._
import doobie.util.update._
import doobie.util.query._

import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import org.postgresql.util._
import org.postgresql.geometric._
import org.specs2.mutable.Specification

import scalaz.concurrent.Task
import scalaz.\/-

// Establish that we can write and read various types.
object pgtypesspec extends Specification {

  val xa = DriverManagerTransactor[Task](
    "org.postgresql.Driver", 
    "jdbc:postgresql:world", 
    "postgres", ""
  )

  def inOut[A: Atom](col: String, a: A) =
    for {
      _  <- Update0(s"CREATE TEMPORARY TABLE TEST (value $col)", None).run
      a0 <- Update[A](s"INSERT INTO TEST VALUES (?)", None).withUniqueGeneratedKeys[A]("value")(a)
    } yield (a0)

  def testInOut[A](col: String, a: A)(implicit m: Meta[A]) = 
    s"Mapping for $col as ${m.scalaType}" >> {
      s"write+read $col as ${m.scalaType}" in { 
        inOut(col, a).transact(xa).attemptRun must_== \/-(a)
      }
      s"write+read $col as Option[${m.scalaType}] (Some)" in { 
        inOut[Option[A]](col, Some(a)).transact(xa).attemptRun must_== \/-(Some(a))
      }
      s"write+read $col as Option[${m.scalaType}] (None)" in { 
        inOut[Option[A]](col, None).transact(xa).attemptRun must_== \/-(None)
      }
    }

  def skip(col: String, msg: String = "not yet implemented") =
    s"Mapping for $col" >> {
      "PENDING:" in pending(msg)
    }

  // 8.1 Numeric Types
  testInOut[Short]("smallint", 123)
  testInOut[Int]("integer", 123)
  testInOut[Long]("bigint", 123) 
  testInOut[BigDecimal]("decimal", 123)      
  testInOut[BigDecimal]("numeric", 123)      
  testInOut[Float]("real", 123.45f)
  testInOut[Double]("double precision", 123.45)

  // 8.2 Monetary Types
  skip("pgmoney", "getObject returns Double")

  // 8.3 Character Types"
  testInOut("character varying", "abcdef")
  testInOut("varchar", "abcdef")
  testInOut("character(6)", "abcdef")
  testInOut("char(6)", "abcdef")
  testInOut("text", "abcdef")

  // 8.4 Binary Types
  testInOut[List[Byte]]  ("bytea", BigInt("DEADBEEF",16).toByteArray.toList) 
  testInOut[Vector[Byte]]("bytea", BigInt("DEADBEEF",16).toByteArray.toVector) 

  // 8.5 Date/Time Types"
  skip("timestamp")
  skip("timestamp with time zone")
  skip("date")
  skip("time")
  skip("time with time zone")
  skip("interval")
  
  // 8.6 Boolean Type
  testInOut("boolean", true)

  // 8.7 Enumerated Types
  skip("enum")

  // 8.8 Geometric Types
  testInOut("box", new PGbox(new PGpoint(1, 2), new PGpoint(3, 4)))
  testInOut("circle", new PGcircle(new PGpoint(1, 2), 3))
  testInOut("lseg", new PGlseg(new PGpoint(1, 2), new PGpoint(3, 4)))
  testInOut("path", new PGpath(Array(new PGpoint(1, 2), new PGpoint(3, 4)), false))
  testInOut("path", new PGpath(Array(new PGpoint(1, 2), new PGpoint(3, 4)), true))
  testInOut("point", new PGpoint(1, 2))
  testInOut("polygon", new PGpolygon(Array(new PGpoint(1, 2), new PGpoint(3, 4))))
  skip("line", "doc says \"not fully implemented\"")

  // 8.9 Network Address Types
  testInOut("inet", InetAddress.getByName("123.45.67.8"))
  skip("inet", "no suitable JDK type")
  skip("macaddr", "no suitable JDK type")

  // 8.10 Bit String Types
  skip("bit")
  skip("bit varying")

  // 8.11 Text Search Types
  skip("tsvector")
  skip("tsquery")

  // 8.12 UUID Type
  testInOut("uuid", UUID.randomUUID)

  // 8.13 XML Type
  skip("xml")

  // 8.14 JSON Type
  skip("json")

  // 8.15 Arrays
  skip("bit[]", "Requires a cast")
  skip("smallint[]", "always comes back as Array[Int]")
  testInOut("integer[]", List[Int](1,2))
  testInOut("bigint[]", List[Long](1,2))
  testInOut("real[]", List[Float](1.2f, 3.4f))
  testInOut("double precision[]", List[Double](1.2, 3.4))
  testInOut("varchar[]", List[String]("foo", "bar"))

  // 8.16 Composite Types
  skip("composite")

  // 8.17 Range Types
  skip("int4range")
  skip("int8range")
  skip("numrange")
  skip("tsrange")
  skip("tstzrange")
  skip("daterange")
  skip("custom")

}