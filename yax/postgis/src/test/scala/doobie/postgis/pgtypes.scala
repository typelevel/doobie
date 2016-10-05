package doobie.postgres

import doobie.imports._
import doobie.postgres.pgtypes._

import org.postgis._
import org.postgresql.util._
import org.postgresql.geometric._
import org.specs2.mutable.Specification

#+scalaz
import scalaz.{ Maybe, \/- }
#-scalaz
#+cats
import scala.util.{ Left => -\/, Right => \/- }
#-cats

// Establish that we can write and read various types.
object pgtypesspec extends Specification {

  val xa = DriverManagerTransactor[IOLite](
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

  // PostGIS geometry types

  // Random streams of geometry values
  lazy val rnd: Iterator[Double]     = Stream.continually(util.Random.nextDouble).iterator
  lazy val pts: Iterator[Point]      = Stream.continually(new Point(rnd.next, rnd.next)).iterator
  lazy val lss: Iterator[LineString] = Stream.continually(new LineString(Array(pts.next, pts.next, pts.next))).iterator
  lazy val lrs: Iterator[LinearRing] = Stream.continually(new LinearRing({ lazy val p = pts.next; Array(p, pts.next, pts.next, pts.next, p) })).iterator
  lazy val pls: Iterator[Polygon]    = Stream.continually(new Polygon(lras.next)).iterator

  // Streams of arrays of random geometry values
  lazy val ptas: Iterator[Array[Point]]      = Stream.continually(Array(pts.next, pts.next, pts.next)).iterator
  lazy val plas: Iterator[Array[Polygon]]    = Stream.continually(Array(pls.next, pls.next, pls.next)).iterator
  lazy val lsas: Iterator[Array[LineString]] = Stream.continually(Array(lss.next, lss.next, lss.next)).iterator
  lazy val lras: Iterator[Array[LinearRing]] = Stream.continually(Array(lrs.next, lrs.next, lrs.next)).iterator

  // All these types map to `geometry`
  def testInOutGeom[A <: Geometry: Meta](a: A) =
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

}


