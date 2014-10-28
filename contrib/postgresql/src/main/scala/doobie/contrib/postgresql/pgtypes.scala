package doobie.contrib.postgresql

import doobie.util.scalatype.ScalaType

import org.postgresql.util._
import org.postgresql.geometric._

import scala.Predef._
import scala.reflect.ClassTag

/** `ScalaType` instances for PostgreSQL types. */
object pgtypes {

  // The PG* types map nicely via getObject; nothing special is needed here.
  implicit val PGboxType      = ScalaType.objectType[PGbox]
  implicit val PGcircleType   = ScalaType.objectType[PGcircle]
  implicit val PGIntervalType = ScalaType.objectType[PGInterval]
  implicit val PGlineType     = ScalaType.objectType[PGline]
  implicit val PGlsegType     = ScalaType.objectType[PGlseg]
  implicit val PGmoneyType    = ScalaType.objectType[PGmoney]
  implicit val PGpathType     = ScalaType.objectType[PGpath]
  implicit val PGpointType    = ScalaType.objectType[PGpoint]
  implicit val PGpolygonType  = ScalaType.objectType[PGpolygon]

  // java.sql.Array::getArray returns an Object that may be of primitive type or of boxed type,
  // depending on the driver, so we can't really abstract over it. Also there's no telling what 
  // happens with multi-dimensional arrays since most databases don't support them. So anyway here 
  // we go with PostgreSQL support:
  //
  // PostgreSQL arrays show up as Array[AnyRef] with `null` for NULL, so that's mostly sensible;
  // there would be no way to distinguish 0 from NULL otherwise for an int[], for example. So,
  // these arrays can be multi-dimensional and can have NULL cells, but cannot have NULL slices;
  // i.e., {{1,2,3}, {4,5,NULL}} is ok but {{1,2,3}, NULL} is not. So this means we only have to 
  // worry about Array[Array[...[A]]] and Array[Array[...[Option[A]]]] in our mappings.

  // Construct a pair of ScalaType instances for arrays of lifted (nullable) and unlifted (non-
  // nullable) reference types (as noted above, PostgreSQL doesn't ship arrays of primitives).
  def boxedPair[A >: Null <: AnyRef: ClassTag]: (ScalaType[Array[A]], ScalaType[Array[Option[A]]]) = {
    val raw = ScalaType.arrayType[A]
    (raw, // TODO: ban nulls
     raw.xmap[Array[Option[A]]](_.map(Option(_)), _.map(_.get).toArray)
    )
  }

  // Arrays of lifted (nullable) and unlifted (non-nullable) Java wrapped primitives
  implicit val (unliftedBooleanArrayType, liftedBooleanArrayType) = boxedPair[java.lang.Boolean]
  implicit val (unliftedByteArrayType,    liftedByteArrayType)    = boxedPair[java.lang.Byte]
  implicit val (unliftedShortArrayType,   liftedShortArrayType)   = boxedPair[java.lang.Short]
  implicit val (unliftedIntegerArrayType, liftedIntegerArrayType) = boxedPair[java.lang.Integer]
  implicit val (unliftedLongArrayType,    liftedLongArrayType)    = boxedPair[java.lang.Long]
  implicit val (unliftedFloatArrayType,   liftedFloatArrayType)   = boxedPair[java.lang.Float]
  implicit val (unliftedDoubleArrayType,  liftedDoubleArrayType)  = boxedPair[java.lang.Double]
  implicit val (unliftedStringArrayType,  liftedStringArrayType)  = boxedPair[java.lang.String]

  // Construct a pair of ScalaType instances for arrays of lifted (nullable) and unlifted (non-
  // nullable) types xmapped to a desitination type B. This is a more general form of `boxedPair`
  // above, but distinct to avoid extra array copying in the base case.
  def unboxedPair[A >: Null <: AnyRef: ClassTag, B: ClassTag](f: A => B, g: B => A): (ScalaType[Array[B]], ScalaType[Array[Option[B]]]) = {
    val raw = ScalaType.arrayType[A]
    (raw.xmap(_.map(f), _.map(g)), // TODO: ban nulls
     raw.xmap(_.map(a => Option(f(a))), _.map(_.map(g).get).toArray)
    )  
  }

  // Arrays of lifted (nullable) and unlifted (non-nullable) AnyVals
  implicit val (unliftedUnboxedBooleanArrayType, liftedUnboxedBooleanArrayType) = unboxedPair[java.lang.Boolean, scala.Boolean](_.booleanValue, java.lang.Boolean.valueOf)
  implicit val (unliftedUnboxedByteArrayType,    liftedUnboxedByteArrayType)    = unboxedPair[java.lang.Byte,    scala.Byte](   _.byteValue,    java.lang.Byte.valueOf)
  implicit val (unliftedUnboxedShortArrayType,   liftedUnboxedShortArrayType)   = unboxedPair[java.lang.Short,   scala.Short](  _.shortValue,   java.lang.Short.valueOf)
  implicit val (unliftedUnboxedIntegerArrayType, liftedUnboxedIntegerArrayType) = unboxedPair[java.lang.Integer, scala.Int](    _.intValue,     java.lang.Integer.valueOf)
  implicit val (unliftedUnboxedLongArrayType,    liftedUnboxedLongArrayType)    = unboxedPair[java.lang.Long,    scala.Long](   _.longValue,    java.lang.Long.valueOf)
  implicit val (unliftedUnboxedFloatArrayType,   liftedUnboxedFloatArrayType)   = unboxedPair[java.lang.Float,   scala.Float](  _.floatValue,   java.lang.Float.valueOf)
  implicit val (unliftedUnboxedDoubleArrayType,  liftedUnboxedDoubleArrayType)  = unboxedPair[java.lang.Double,  scala.Double]( _.doubleValue,  java.lang.Double.valueOf)

  // So, it turns out that arrays of structs don't work because something is missing from the
  // implementation. So this means we will only be able to support primitive types for arrays.
  // 
  // java.sql.SQLFeatureNotSupportedException: Method org.postgresql.jdbc4.Jdbc4Array.getArrayImpl(long,int,Map) is not yet implemented.
  //   at org.postgresql.Driver.notImplemented(Driver.java:729)
  //   at org.postgresql.jdbc2.AbstractJdbc2Array.buildArray(AbstractJdbc2Array.java:771)
  //   at org.postgresql.jdbc2.AbstractJdbc2Array.getArrayImpl(AbstractJdbc2Array.java:171)
  //   at org.postgresql.jdbc2.AbstractJdbc2Array.getArray(AbstractJdbc2Array.java:128)

  // TODO: multidimensional arrays; in the worst case it's just copy/paste of everything above but
  // we can certainly do better than that.

}
