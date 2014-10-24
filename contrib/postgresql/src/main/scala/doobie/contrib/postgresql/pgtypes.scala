package doobie.contrib.postgresql

import doobie.util.scalatype.ScalaType

import org.postgresql.util._
import org.postgresql.geometric._

/** 
 * `ScalaType` instances for PostgreSQL types. These types are not normally used in domain models,
 * but can be mapped via `xmap`.
 */
object pgtypes {

  implicit val PGboxType      = ScalaType.objectType[PGbox]
  implicit val PGcircleType   = ScalaType.objectType[PGcircle]
  implicit val PGIntervalType = ScalaType.objectType[PGInterval]
  implicit val PGlineType     = ScalaType.objectType[PGline]
  implicit val PGlsegType     = ScalaType.objectType[PGlseg]
  implicit val PGmoneyType    = ScalaType.objectType[PGmoney]
  implicit val PGpathType     = ScalaType.objectType[PGpath]
  implicit val PGpointType    = ScalaType.objectType[PGpoint]
  implicit val PGpolygonType  = ScalaType.objectType[PGpolygon]

}
