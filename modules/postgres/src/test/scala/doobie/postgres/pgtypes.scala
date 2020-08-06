// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import java.math.{BigDecimal => JBigDecimal}
import java.net.InetAddress
import java.sql.Timestamp
import java.time.temporal.ChronoField.NANO_OF_SECOND
import java.time.{LocalDate, ZoneOffset}
import java.util.UUID

import cats.effect.{ContextShift, IO}
import com.github.ghik.silencer.silent
import doobie._
import doobie.implicits._
import doobie.postgres.enums._
import doobie.postgres.implicits._
import doobie.implicits.javasql._
import doobie.implicits.javatime.{JavaTimeInstantMeta => _, JavaTimeLocalDateMeta => NewJavaTimeLocalDateMeta,_}
import doobie.implicits.legacy.instant._
import doobie.implicits.legacy.localdate.{JavaTimeLocalDateMeta => LegacyLocalDateMeta}
import doobie.postgres.pgisimplicits._
import doobie.util.arbitraries.SQLArbitraries._
import doobie.util.arbitraries.StringArbitraries._
import doobie.util.arbitraries.TimeArbitraries._
import org.postgis._
import org.postgresql.geometric._
import org.postgresql.util._
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext


// Establish that we can write and read various types.

class pgtypesspec extends Specification with ScalaCheck {

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  def inOut[A: Get : Put](col: String, a: A): ConnectionIO[A] =
    for {
      _ <- Update0(s"CREATE TEMPORARY TABLE TEST (value $col)", None).run
      a0 <- Update[A](s"INSERT INTO TEST VALUES (?)", None).withUniqueGeneratedKeys[A]("value")(a)
    } yield a0

  def inOutOpt[A: Get : Put](col: String, a: Option[A]): ConnectionIO[Option[A]] =
    for {
      _ <- Update0(s"CREATE TEMPORARY TABLE TEST (value $col)", None).run
      a0 <- Update[Option[A]](s"INSERT INTO TEST VALUES (?)", None).withUniqueGeneratedKeys[Option[A]]("value")(a)
    } yield a0

  @SuppressWarnings(Array("org.wartremover.warts.StringPlusAny"))
  def testInOut[A](col: String)(implicit m: Get[A], p: Put[A], arbitrary: Arbitrary[A]) =
    s"Mapping for $col as ${m.typeStack}" >> {
      s"write+read $col as ${m.typeStack}" ! forAll { x: A =>
        inOut(col, x).transact(xa).attempt.unsafeRunSync() must_== Right(x)
      }
      s"write+read $col as Option[${m.typeStack}] (Some)" ! forAll { x: A =>
        inOutOpt[A](col, Some(x)).transact(xa).attempt.unsafeRunSync() must_== Right(Some(x))
      }
      s"write+read $col as Option[${m.typeStack}] (None)" in {
        inOutOpt[A](col, None).transact(xa).attempt.unsafeRunSync() must_== Right(None)
      }
    }

  @SuppressWarnings(Array("org.wartremover.warts.StringPlusAny"))
  def testInOut[A](col: String, a: A)(implicit m: Get[A], p: Put[A]) =
    s"Mapping for $col as ${m.typeStack}" >> {
      s"write+read $col as ${m.typeStack}" in {
        inOut(col, a).transact(xa).attempt.unsafeRunSync() must_== Right(a)
      }
      s"write+read $col as Option[${m.typeStack}] (Some)" in {
        inOutOpt[A](col, Some(a)).transact(xa).attempt.unsafeRunSync() must_== Right(Some(a))
      }
      s"write+read $col as Option[${m.typeStack}] (None)" in {
        inOutOpt[A](col, None).transact(xa).attempt.unsafeRunSync() must_== Right(None)
      }
    }

  @SuppressWarnings(Array("org.wartremover.warts.StringPlusAny"))
  def testInOutWithCustomTransform[A](col: String)(t: A => A)(implicit m: Get[A], p: Put[A], arbitrary: Arbitrary[A]) =
    s"Mapping for $col as ${m.typeStack}" >> {
      s"write+read $col as ${m.typeStack}" ! forAll { x: A =>
        inOut(col, t(x)).transact(xa).attempt.unsafeRunSync() must_== Right(t(x))
      }
      s"write+read $col as Option[${m.typeStack}] (Some)" ! forAll { x: A =>
        inOutOpt[A](col, Some(t(x))).transact(xa).attempt.unsafeRunSync() must_== Right(Some(t(x)))
      }
      s"write+read $col as Option[${m.typeStack}] (None)" in {
        inOutOpt[A](col, None).transact(xa).attempt.unsafeRunSync() must_== Right(None)
      }
    }

  @SuppressWarnings(Array("org.wartremover.warts.StringPlusAny"))
  def testInOutWithCustomTransformAndMatch[A, B](col: String)(tr: A => A)(mtch: A => B)(implicit m: Get[A], p: Put[A], arbitrary: Arbitrary[A]) =
    s"Mapping for $col as ${m.typeStack}" >> {
      s"write+read $col as ${m.typeStack}" ! forAll { x: A =>
        inOut(col, tr(x)).transact(xa).attempt.unsafeRunSync().map(mtch) must_== Right(tr(x)).map(mtch)
      }
      s"write+read $col as Option[${m.typeStack}] (Some)" ! forAll { x: A =>
        inOutOpt[A](col, Some(tr(x))).transact(xa).attempt.unsafeRunSync().map(_.map(mtch)) must_== Right(Some(tr(x))).map(_.map(mtch))
      }
      s"write+read $col as Option[${m.typeStack}] (None)" in {
        inOutOpt[A](col, None).transact(xa).attempt.unsafeRunSync() must_== Right(None)
      }
    }

  @SuppressWarnings(Array("org.wartremover.warts.StringPlusAny"))
  def testInOutWithCustomGen[A](col: String, gen: Gen[A])(implicit m: Get[A], p: Put[A]) =
    s"Mapping for $col as ${m.typeStack}" >> {
      s"write+read $col as ${m.typeStack}" ! forAll(gen) { t: A =>
        t match {
          case x: java.sql.Time => println(s"TIME: ${x.toString}")
          case _ =>
        }
        inOut(col, t).transact(xa).attempt.unsafeRunSync() must_== Right(t)
      }
      s"write+read $col as Option[${m.typeStack}] (Some)" ! forAll(gen) { t: A =>
        inOutOpt[A](col, Some(t)).transact(xa).attempt.unsafeRunSync() must_== Right(Some(t))
      }
      s"write+read $col as Option[${m.typeStack}] (None)" in {
        inOutOpt[A](col, None).transact(xa).attempt.unsafeRunSync() must_== Right(None)
      }
    }

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def skip(col: String, msg: String = "not yet implemented") =
    s"Mapping for $col" >> {
      "PENDING:" in pending(msg)
    }

  // 8.1 Numeric Types
  testInOut[Short]("smallint")
  testInOut[Int]("integer")
  testInOut[Long]("bigint")
  testInOut[BigDecimal]("decimal")
  testInOut[BigDecimal]("numeric")
  testInOut[Float]("real")
  testInOut[Double]("double precision")

  // 8.2 Monetary Types
  skip("pgmoney", "getObject returns Double")

  // 8.3 Character Types"
  testInOut[String]("character varying")
  testInOut[String]("varchar")
  testInOutWithCustomGen("character(6)", nLongString(6))
  testInOutWithCustomGen("char(6)", nLongString(6))
  testInOut[String]("text")

  // 8.4 Binary Types
  testInOut[List[Byte]]("bytea")
  testInOut[Vector[Byte]]("bytea")

  // 8.5 Date/Time Types"

  /*
      timestamp
      The allowed range of p is from 0 to 6 for the timestamp and interval types.
   */
  testInOutWithCustomTransform[java.sql.Timestamp]("timestamp") { ts => ts.setNanos(0); ts }
  testInOutWithCustomTransform[java.time.LocalDateTime]("timestamp")(_.withNano(0))
  testInOutWithCustomTransform[java.time.Instant]("timestamp")(_.`with`(NANO_OF_SECOND, 0))


  /*
      timestamp with time zone
      For the time types, the allowed range of p is from 0 to 6 when eight-byte integer storage is used, or from 0 to 10 when floating-point storage is used.
   */
  skip("timestamp with time zone", "something weird going on")
  // testInOutWithCustomTransformAndMatch[java.time.OffsetDateTime, java.sql.Timestamp]("timestamp with time zone")(_.withNano(0)) {
  //   dt =>
  //     Timestamp.valueOf(dt.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime)
  // }

  testInOut[java.sql.Date]("date")
  // TODO LocalDate.of(-500,1,1) is failing
  testInOut[java.time.LocalDate]("date", LocalDate.of(1, 1, 1))(NewJavaTimeLocalDateMeta.get, NewJavaTimeLocalDateMeta.put)
  testInOut[java.time.LocalDate]("date", LocalDate.of(1, 1, 1))(LegacyLocalDateMeta.get, LegacyLocalDateMeta.put)

  /*
      time
      The allowed range of p is from 0 to 6 for the timestamp and interval types.
   */
  testInOut[java.sql.Time]("time")
  testInOutWithCustomTransform[java.time.LocalTime]("time")(_.withNano(0))

  skip("time with time zone")
  testInOut("interval", new PGInterval(1, 2, 3, 4, 5, 6.7))

  // 8.6 Boolean Type
  testInOut[Boolean]("boolean")

  // 8.7 Enumerated Types
  testInOut("myenum", MyEnum.Foo: MyEnum)

  // as scala.Enumeration
  implicit val MyEnumMeta: Meta[MyScalaEnum.Value] = pgEnum(MyScalaEnum, "myenum")
  testInOut("myenum", MyScalaEnum.foo)

  // as java.lang.Enum
  implicit val MyJavaEnumMeta: Meta[MyJavaEnum] = pgJavaEnum[MyJavaEnum]("myenum")
  testInOut("myenum", MyJavaEnum.bar)

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
  testInOut[UUID]("uuid")

  // 8.13 XML Type
  skip("xml")

  // 8.14 JSON Type
  skip("json")

  // 8.15 Arrays
  skip("bit[]", "Requires a cast")
  skip("smallint[]", "always comes back as Array[Int]")
  testInOut[List[Int]]("integer[]")
  testInOut[List[Long]]("bigint[]")
  testInOut[List[Float]]("real[]")
  testInOut[List[Double]]("double precision[]")
  testInOut[List[String]]("varchar[]")
  testInOut[List[UUID]]("uuid[]")
  testInOut("numeric[]", List[JBigDecimal](BigDecimal("3.14").bigDecimal, BigDecimal("42.0").bigDecimal))
  testInOut[List[BigDecimal]]("numeric[]", List[BigDecimal](BigDecimal("3.14"), BigDecimal("42.0")))

  // 8.16 Structs
  skip("structs")

  // 8.17 Range Types
  skip("int4range")
  skip("int8range")
  skip("numrange")
  skip("tsrange")
  skip("tstzrange")
  skip("daterange")
  skip("custom")

  // PostGIS geometry types

  // Random streams of geometry values
  lazy val rnd: Iterator[Double] = Stream.continually(scala.util.Random.nextDouble).iterator: @silent("deprecated")
  lazy val pts: Iterator[Point] = Stream.continually(new Point(rnd.next, rnd.next)).iterator: @silent("deprecated")
  lazy val lss: Iterator[LineString] = Stream.continually(new LineString(Array(pts.next, pts.next, pts.next))).iterator: @silent("deprecated")
  lazy val lrs: Iterator[LinearRing] = Stream.continually(new LinearRing({
    lazy val p = pts.next;
    Array(p, pts.next, pts.next, pts.next, p)
  })).iterator: @silent("deprecated")
  lazy val pls: Iterator[Polygon] = Stream.continually(new Polygon(lras.next)).iterator: @silent("deprecated")

  // Streams of arrays of random geometry values
  lazy val ptas: Iterator[Array[Point]] = Stream.continually(Array(pts.next, pts.next, pts.next)).iterator: @silent("deprecated")
  lazy val plas: Iterator[Array[Polygon]] = Stream.continually(Array(pls.next, pls.next, pls.next)).iterator: @silent("deprecated")
  lazy val lsas: Iterator[Array[LineString]] = Stream.continually(Array(lss.next, lss.next, lss.next)).iterator: @silent("deprecated")
  lazy val lras: Iterator[Array[LinearRing]] = Stream.continually(Array(lrs.next, lrs.next, lrs.next)).iterator: @silent("deprecated")

  // All these types map to `geometry`
  def testInOutGeom[A <: Geometry : Meta](a: A) =
    testInOut[A]("geometry", a)

  testInOutGeom[Geometry](pts.next)
  testInOutGeom[ComposedGeom](new MultiLineString(lsas.next))
  testInOutGeom[GeometryCollection](new GeometryCollection(Array(pts.next, lss.next)))
  testInOutGeom[MultiLineString](new MultiLineString(lsas.next))
  testInOutGeom[MultiPolygon](new MultiPolygon(plas.next))
  testInOutGeom[PointComposedGeom](lss.next)
  testInOutGeom[LineString](lss.next)
  testInOutGeom[MultiPoint](new MultiPoint(ptas.next))
  testInOutGeom[Polygon](pls.next)
  testInOutGeom[Point](pts.next)

  // hstore
  testInOut[Map[String, String]]("hstore")

}
