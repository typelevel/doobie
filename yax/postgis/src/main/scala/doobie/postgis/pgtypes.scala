package doobie.postgres

import doobie.enum.jdbctype
import doobie.imports._
import doobie.util.invariant._

import java.util.UUID
import java.net.InetAddress

import org.postgis._
import org.postgresql.util._
import org.postgresql.geometric._

import scala.Predef._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

/** `Meta` and `Atom` instances for Postgis types. */
object pgtypes {

  // PostGIS outer types
  implicit val PGgeometryType = Meta.other[PGgeometry]("geometry")
  implicit val PGbox3dType    = Meta.other[PGbox3d]("box3d")
  implicit val PGbox2dType    = Meta.other[PGbox2d]("box2d")

  // Constructor for geometry types via the `Geometry` member of PGgeometry
  private def geometryType[A >: Null <: Geometry: TypeTag](implicit A: ClassTag[A]): Meta[A] =
    PGgeometryType.nxmap[A](g =>
      try A.runtimeClass.cast(g.getGeometry).asInstanceOf[A]
      catch {
        case _: ClassCastException => throw InvalidObjectMapping(A.runtimeClass, g.getGeometry.getClass)
      }, new PGgeometry(_))

  // PostGIS Geometry Types
  implicit val GeometryType           = geometryType[Geometry]
  implicit val ComposedGeomType       = geometryType[ComposedGeom]
  implicit val GeometryCollectionType = geometryType[GeometryCollection]
  implicit val MultiLineStringType    = geometryType[MultiLineString]
  implicit val MultiPolygonType       = geometryType[MultiPolygon]
  implicit val PointComposedGeomType  = geometryType[PointComposedGeom]
  implicit val LineStringType         = geometryType[LineString]
  implicit val MultiPointType         = geometryType[MultiPoint]
  implicit val PolygonType            = geometryType[Polygon]
  implicit val PointType              = geometryType[Point]
}




