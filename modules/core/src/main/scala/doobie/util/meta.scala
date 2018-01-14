// // Copyright (c) 2013-2017 Rob Norris
// // This software is licensed under the MIT License (MIT).
// // For more information see LICENSE or https://opensource.org/licenses/MIT

// package doobie.util

// import cats._
// import cats.data.NonEmptyList
// import cats.implicits._

// import doobie.enum.JdbcType
// import doobie.enum.JdbcType.{ Array => JdbcArray, Boolean => JdbcBoolean, _ }
// import doobie.util.invariant.{ NonNullableColumnRead, NonNullableColumnUpdate, InvalidObjectMapping }
// import doobie.util.kernel.Kernel

// import java.sql.{ PreparedStatement, ResultSet }

// import scala.annotation.implicitNotFound
// import scala.collection.immutable.TreeSet
// import scala.reflect.runtime.universe.TypeTag
// import scala.reflect.ClassTag
// import scala.Predef._

// import shapeless._
// import shapeless.ops.hlist.IsHCons

// /** Module defining the lowest level of column mapping. */
// object meta {

//   /**
//    * Metadata defining the column-level mapping to and from Scala type `A`. A given Scala type might
//    * be read from or written to columns with a variety of JDBC and/or vendor-specific types,
//    * depending on supported coercions and luck.
//    *
//    * Reading and writing values to JDBC is asymmetric with respect to `null`, is complicated by
//    * unboxed types, and is not consistent with idiomatic Scala; so some discussion is required.
//    * Scala values should never be `null`. Setting a `NULL` JDBC value is accomplished via the
//    * `setNull` operation. Similarly when getting a JDBC value we must subsequently ask `.wasNull`
//    * on the JDBC resource and decide how to handle the value. The `Atom` typeclass takes care of
//    * mapping nullable values to `Option` so these issues should not be a concern for casual users.
//    */
//   @implicitNotFound("Could not find an instance of Meta[${A}]; you can construct one based on a primitive instance via `xmap`.")
//   sealed abstract class Meta[A] {

//     private[util] val kernel: Kernel[A]
//     import kernel._

//     final def unsafeGetNonNullable(rs: ResultSet, n: Int): A = {
//       val i = get(rs, n)
//       if (rs.wasNull) throw NonNullableColumnRead(n, jdbcSource.head)
//       else ia(i)
//     }

//     final def unsafeGetNullable(rs: ResultSet, n: Int): Option[A] = {
//       val i = get(rs, n)
//       if (rs.wasNull) None else Some(ia(i))
//     }

//     @SuppressWarnings(Array("org.wartremover.warts.Equals"))
//     final def unsafeSetNonNullable(ps: PreparedStatement, n: Int, a: A): Unit =
//       if (a == null) throw NonNullableColumnUpdate(n, jdbcTarget.head)
//       else set(ps, n, ai(a))

//     final def unsafeSetNullable(ps: PreparedStatement, n: Int, oa: Option[A]): Unit =
//       oa match {
//         case None    => setNull(ps, n)
//         case Some(a) => unsafeSetNonNullable(ps, n, a)
//       }

//     @SuppressWarnings(Array("org.wartremover.warts.Equals"))
//     final def unsafeUpdateNonNullable(rs: ResultSet, n: Int, a: A): Unit =
//       if (a == null) throw NonNullableColumnUpdate(n, jdbcTarget.head)
//       else update(rs, n, ai(a))

//     final def unsafeUpdateNullable(rs: ResultSet, n: Int, oa: Option[A]): Unit =
//       oa match {
//         case None    => rs.updateNull(n)
//         case Some(a) => unsafeUpdateNonNullable(rs, n, a)
//       }

//     final def unsafeSetNull(ps: PreparedStatement, n: Int): Unit =
//       setNull(ps, n)

//     /**
//      * Name of the Scala type, for diagnostic purposes. Smart constructors require a `TypeTag` to
//      * guarantee this value is correct.
//      */
//     def scalaType: String

//     /** Destination JDBC types to which values of type `A` can be written. */
//     def jdbcTarget: NonEmptyList[JdbcType]

//     /** Source JDBC types from which values of type `A` can be read. */
//     def jdbcSource: NonEmptyList[JdbcType]

//     /** Switch on the flavor of this `Meta`. */
//     def fold[B](f: BasicMeta[A] => B, g: AdvancedMeta[A] => B): B

//     /** Invariant map. */
//     def xmap[B: TypeTag](f: A => B, g: B => A): Meta[B]

//     /**
//      * Invariant map with `null` handling, for `A, B >: Null`; the functions `f` and `g` will
//      * never be passed a `null` value.
//      */
//     @deprecated("Null is no longer observable here; just use xmap.", "0.4.2")
//     def nxmap[B >: Null : TypeTag](f: A => B, g: B => A)(implicit ev: Null <:< A): Meta[B] =
//       xmap(f, g)

//   }

//   /**
//    * `Meta` for "basic" JDBC types as defined by the specification. These include the basic numeric
//    * and text types with distinct `get/setXXX` methods and fixed mappings that ostensibly work for
//    * all compliant drivers. These types defined both "recommended" source types (`jdbcSource` here)
//    * and "supported" types (`jdbcSourceSecondary`) which drivers must not reject outright, although
//    * in many cases coercion failures are likely (reading an `Int` from a `VarChar` for instance) so
//    * these mappings should be viewed with suspicion.
//    */
//   sealed trait BasicMeta[A] extends Meta[A]  { outer =>

//     /** Supported but non-recommended source JDBC sources (see trait description above). */
//     val jdbcSourceSecondary: List[JdbcType]

//     /** True if `A` can be written to a column or 'in' parameter with the specified `JdbcType`. */
//     def canWriteTo(jdbc: JdbcType): Boolean =
//       jdbcTarget.element(jdbc)

//     /** True if `A` can be read from a column or 'out' parameter with the specified `JdbcType`. */
//     def canReadFrom(jdbc: JdbcType): Boolean =
//       jdbcSource.element(jdbc)

//     /**
//      * True if `A` might be readable from a column or 'out' parameter with the specified `JdbcType`,
//      * taking into account non-recommended source types specified in `jdbcSourceSecondary`.
//      */
//     def mightReadFrom(jdbc: JdbcType): Boolean =
//       jdbcSource.element(jdbc) || jdbcSourceSecondary.element(jdbc)

//     def fold[B](f: BasicMeta[A] => B, g: AdvancedMeta[A] => B): B =
//       f(this)

//     @SuppressWarnings(Array("org.wartremover.warts.ToString"))
//     def xmap[B](f: A => B, g: B => A)(implicit ev: TypeTag[B]): Meta[B] =
//       Meta.reg(new BasicMeta[B] {
//         val kernel              = outer.kernel.imap(f)(g)
//         val jdbcSource          = outer.jdbcSource
//         val jdbcTarget          = outer.jdbcTarget
//         val scalaType           = ev.tpe.toString
//         val jdbcSourceSecondary = outer.jdbcSourceSecondary
//       })

//   }


//   /**
//    * `Meta` for "advanced" JDBC types as defined by the specification. These include `Array`,
//    * `JavaObject`, `Struct`, and other types that require driver, schema, or vendor-specific
//    * knowledge and are unlikely to be portable between vendors (or indeed between applications).
//    * These mappings require (in addition to matching JDBC types) matching driver, schema, or
//    * vendor-specific data types, sadly given as `String`s in JDBC.
//    */
//   sealed trait AdvancedMeta[A] extends Meta[A] { outer =>

//     /**
//      * List of schema types to which values of type `A` can be written and from which they can be
//      * read. Databases will often have several names for the same type, and the JDBC driver may
//      * report an alias that doesn't appear in the schema or indeed in the database documentation.
//      * This field is therefore a list.
//      */
//     val schemaTypes: NonEmptyList[String]

//     /**
//      * True if `A` can be written to a column or 'in' parameter with the specified `JdbcType` and
//      * schema types.
//      */
//     def canWriteTo(jdbc: JdbcType, schema: String): Boolean =
//       schemaTypes.element(schema) && jdbcTarget.element(jdbc)

//     /**
//      * True if `A` can be read from a column or 'out' parameter with the specified `JdbcType` and
//      * schema types.
//      */
//     def canReadFrom(jdbc: JdbcType, schema: String): Boolean =
//       schemaTypes.element(schema) && jdbcSource.element(jdbc)

//     def fold[B](f: BasicMeta[A] => B, g: AdvancedMeta[A] => B): B =
//       g(this)

//     @SuppressWarnings(Array("org.wartremover.warts.ToString"))
//     def xmap[B](f: A => B, g: B => A)(implicit ev: TypeTag[B]): Meta[B] =
//       Meta.reg(new AdvancedMeta[B] {
//         val kernel      = outer.kernel.imap(f)(g)
//         val jdbcSource  = outer.jdbcSource
//         val jdbcTarget  = outer.jdbcTarget
//         val scalaType   = ev.tpe.toString
//         val schemaTypes = outer.schemaTypes
//       })

//   }


//   /** Constructors, accessors, and typeclass instances. */
//   object Meta extends {

//     /** @group Typeclass Instances */
//     implicit val MetaOrder: Order[Meta[_]] =
//       // Type argument necessary to avoid spurious "illegal cyclic reference involving object Meta"
//       // only in Scala 2.11, and only with cats for whatever reason. Confidence high!
//       Order.by[Meta[_], Either[(String, NonEmptyList[JdbcType], NonEmptyList[JdbcType], List[JdbcType]), (String, NonEmptyList[JdbcType], NonEmptyList[JdbcType], NonEmptyList[String])]](_.fold(
//         b => Left((b.scalaType, b.jdbcTarget, b.jdbcSource, b.jdbcSourceSecondary)),
//         a => Right((a.scalaType, a.jdbcTarget, a.jdbcSource, a.schemaTypes))))

//     /** @group Typeclass Instances */
//     implicit val MetaOrdering: scala.Ordering[Meta[_]] =
//       MetaOrder.toOrdering

//     // See note on trait Meta above
//     @SuppressWarnings(Array("org.wartremover.warts.Var"))
//     private var instances: TreeSet[Meta[_]] = TreeSet.empty // scalastyle:ignore

//   } with LowPriorityImplicits with MetaInstances {

//     // sorry
//     private[meta] def reg[A](m: Meta[A]): m.type =
//       synchronized {
//         instances = instances + m
//         m
//       }

//     implicit lazy val JdbcTypeMeta: Meta[JdbcType] =
//       IntMeta.xmap(JdbcType.fromInt, _.toInt)

//     def apply[A](implicit A: Meta[A]): Meta[A] = A

//     /**
//      * Computes the set of know `Meta`s that support reading the indicated schema type.
//      * @group Accessors
//      */
//     def readersOf(jdbc: JdbcType, schema: String): TreeSet[Meta[_]] =
//       instances.filter(_.fold(_.canReadFrom(jdbc), _.canReadFrom(jdbc, schema)))

//     /**
//      * Computes the set of know `Meta`s that support writing the indicated schema type.
//      * @group Accessors
//      */
//     def writersOf(jdbc: JdbcType, schema: String): TreeSet[Meta[_]] =
//       instances.filter(_.fold(_.canWriteTo(jdbc), _.canWriteTo(jdbc, schema)))

//     /**
//      * Construct a `BasicMeta` for the given type.
//      * @group Constructors
//      */
//     @SuppressWarnings(Array("org.wartremover.warts.ToString"))
//     def basic[A](
//       jdbcTarget0: NonEmptyList[JdbcType],
//       jdbcSource0: NonEmptyList[JdbcType],
//       jdbcSourceSecondary0: List[JdbcType],
//       get0: (ResultSet, Int) => A,
//       set0: (PreparedStatement, Int, A) => Unit,
//       update0: (ResultSet, Int, A) => Unit
//     )(implicit ev: TypeTag[A]): BasicMeta[A] =
//       Meta.reg(new BasicMeta[A] {
//         val kernel = new Kernel[A] {
//           type I = A
//           val ai      = (a: I) => a
//           val ia      = (i: I) => i
//           val get     = get0
//           val set     = set0
//           val setNull = (ps: PreparedStatement, n: Int) => ps.setNull(n, jdbcTarget0.head.toInt)
//           val update  = update0
//           val width   = 1
//         }
//         val scalaType           = ev.tpe.toString
//         val jdbcTarget          = jdbcTarget0
//         val jdbcSource          = jdbcSource0
//         val jdbcSourceSecondary = jdbcSourceSecondary0
//       })

//     /**
//      * Construct a `BasicMeta` for the given type, with symmetric primary mappings.
//      * @group Constructors
//      */
//     def basic1[A](
//       jdbcType: JdbcType,
//       jdbcSourceSecondary0: List[JdbcType],
//       get0: (ResultSet, Int) => A,
//       set0: (PreparedStatement, Int, A) => Unit,
//       update0: (ResultSet, Int, A) => Unit
//     )(implicit ev: TypeTag[A]): BasicMeta[A] =
//       basic(NonEmptyList.of(jdbcType), NonEmptyList.of(jdbcType), jdbcSourceSecondary0, get0, set0, update0)

//     /**
//      * Construct an `AdvancedMeta` for the given type.
//      * @group Constructors
//      */
//     @SuppressWarnings(Array("org.wartremover.warts.ToString"))
//     def advanced[A](
//       jdbcTypes: NonEmptyList[JdbcType],
//       schemaTypes0: NonEmptyList[String],
//       get0: (ResultSet, Int) => A,
//       set0: (PreparedStatement, Int, A) => Unit,
//       update0: (ResultSet, Int, A) => Unit
//     )(implicit ev: TypeTag[A]): AdvancedMeta[A] =
//       Meta.reg(new AdvancedMeta[A] {
//         val kernel = new Kernel[A] {
//           type I = A
//           val ai      = (a: I) => a
//           val ia      = (i: I) => i
//           val get     = get0
//           val set     = set0
//           val setNull = (ps: PreparedStatement, n: Int) => ps.setNull(n, jdbcTypes.head.toInt, schemaTypes0.head)
//           val update  = update0
//           val width   = 1
//         }
//         val scalaType   = ev.tpe.toString
//         val jdbcTarget  = jdbcTypes
//         val jdbcSource  = jdbcTypes
//         val schemaTypes = schemaTypes0
//       })

//     /**
//      * Construct an `AdvancedMeta` for the given type, mapped as JDBC `Array`.
//      * @group Constructors
//      */
//     @SuppressWarnings(Array("org.wartremover.warts.Equals", "org.wartremover.warts.AsInstanceOf"))
//     def array[A >: Null <: AnyRef: TypeTag](elementType: String, schemaH: String, schemaT: String*): AdvancedMeta[Array[A]] =
//       advanced[Array[A]](
//         NonEmptyList.of(JdbcArray),
//         NonEmptyList.of(schemaH, schemaT : _*),
//         (r, n) => {
//           val a = r.getArray(n)
//           (if (a == null) null else a.getArray).asInstanceOf[Array[A]]
//         },
//         (ps, n, a) => {
//           val conn = ps.getConnection
//           val arr  = conn.createArrayOf(elementType, a.asInstanceOf[Array[AnyRef]])
//           ps.setArray(n, arr)
//         },
//         (rs, n, a) => {
//           val stmt = rs.getStatement
//           val conn = stmt.getConnection
//           val arr  = conn.createArrayOf(elementType, a.asInstanceOf[Array[AnyRef]])
//           rs.updateArray(n, arr)
//         }
//       )

//     /**
//      * Construct an `AdvancedMeta` for the given type, mapped as JDBC `Other,JavaObject`.
//      * @group Constructors
//      */
//     @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
//     def other[A >: Null <: AnyRef: TypeTag](schemaH: String, schemaT: String*)(implicit A: ClassTag[A]): AdvancedMeta[A] =
//       advanced[A](
//         NonEmptyList.of(Other, JavaObject),
//         NonEmptyList.of(schemaH, schemaT : _*),
//         (rs, n) => {
//           rs.getObject(n) match {
//             case null => null
//             case a    =>
//               // force the cast here rather than letting a potentially ill-typed value escape
//               try A.runtimeClass.cast(a).asInstanceOf[A]
//               catch {
//                 case _: ClassCastException => throw InvalidObjectMapping(A.runtimeClass, a.getClass)
//               }
//           }
//         },
//         (ps, n, a) => ps.setObject(n, a),
//         (rs, n, a) => rs.updateObject(n, a)
//       )

//     /** @group Instances */
//     implicit def ArrayTypeAsListMeta[A: ClassTag: TypeTag](implicit ev: Meta[Array[A]]): Meta[List[A]] =
//       ev.xmap(_.toList, _.toArray)

//     /** @group Instances */
//     implicit def ArrayTypeAsVectorMeta[A: ClassTag: TypeTag](implicit ev: Meta[Array[A]]): Meta[Vector[A]] =
//       ev.xmap(_.toVector, _.toArray)

//     /**
//       * Derive Meta for nullable unary product types.
//       * A - type for which instance is derived
//       * L - HList representation of type A
//       * H - type of the head of L (this is the only type in L)
//       * T - type of the tail of L (unused)
//       * @group Instances
//       */
//     implicit def unaryProductMetaNullable[A: TypeTag, L <: HList, H >: Null, T <: HList](
//       // representation (L) for type A
//       implicit gen: Generic.Aux[A, L],
//       // head (H) and tail (T) type of representation (L)
//       c: IsHCons.Aux[L, H, T],
//       // Meta instance for the head (the only element in representation)
//       hmeta: Lazy[Meta[H]],
//       // provide evidence that representation (L) and singleton hlist with
//       // the only element of type H are the same type
//       ev: =:=[H :: HNil, L]
//     ): Meta[A] = hmeta.value.xmap[A](
//       // `from` converts representation L to A, but there is only H here,
//       // but provided evidence `=:=[H :: HNil, L]` we can construct L from H
//       // and A from L (using `from`)
//       h => gen.from(h :: HNil),
//       // `to` converts A to representation L, it's Meta[H], so H is required.
//       // H is just a head of representation
//       a => gen.to(a).head
//     )

//   }

//   trait LowPriorityImplicits {
//     /**
//       * Same as `unaryProductMetaNullable` for non-nullable unary products
//       * @group Instances
//       */
//     implicit def unaryProductMetaNonNullable[A : TypeTag, L <: HList, H, T <: HList](
//       implicit gen: Generic.Aux[A, L],
//       c: IsHCons.Aux[L, H, T],
//       hmeta: Lazy[Meta[H]],
//       ev: =:=[H :: HNil, L]
//     ): Meta[A] = hmeta.value.xmap[A](h => gen.from(h :: HNil), a => gen.to(a).head)
//   }

//   // Instances for basic types, according to the JDBC spec
//   trait MetaInstances {

//     /** @group Instances */
//     implicit val ByteMeta: Meta[Byte] =
//       Meta.basic1[Byte](
//         TinyInt,
//         List(SmallInt, Integer, BigInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
//           LongVarChar),
//         _.getByte(_), _.setByte(_, _), _.updateByte(_, _))

//     /** @group Instances */
//     implicit val ShortMeta: Meta[Short] =
//       Meta.basic1[Short](
//         SmallInt,
//         List(TinyInt, Integer, BigInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
//           LongVarChar),
//         _.getShort(_), _.setShort(_, _), _.updateShort(_, _))

//     /** @group Instances */
//     implicit val IntMeta: Meta[Int] =
//       Meta.basic1[Int](
//         Integer,
//         List(TinyInt, SmallInt, BigInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
//           LongVarChar),
//         _.getInt(_), _.setInt(_, _), _.updateInt(_, _))

//     /** @group Instances */
//     implicit val LongMeta: Meta[Long] =
//       Meta.basic1[Long](
//         BigInt,
//         List(TinyInt, Integer, SmallInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
//           LongVarChar),
//         _.getLong(_), _.setLong(_, _), _.updateLong(_, _))

//     /** @group Instances */
//     implicit val FloatMeta: Meta[Float] =
//       Meta.basic1[Float](
//         Real,
//         List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
//           LongVarChar),
//         _.getFloat(_), _.setFloat(_, _), _.updateFloat(_, _))

//     /** @group Instances */
//     implicit val DoubleMeta: Meta[Double] =
//       Meta.basic[Double](
//         NonEmptyList.of(Double),
//         NonEmptyList.of(Float, Double),
//         List(TinyInt, Integer, SmallInt, BigInt, Float, Real, Decimal, Numeric, Bit, Char, VarChar,
//           LongVarChar),
//         _.getDouble(_), _.setDouble(_, _), _.updateDouble(_, _))

//     /** @group Instances */
//     implicit val BigDecimalMeta: Meta[java.math.BigDecimal] =
//       Meta.basic[java.math.BigDecimal](
//         NonEmptyList.of(Numeric),
//         NonEmptyList.of(Decimal, Numeric),
//         List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Bit, Char, VarChar,
//           LongVarChar),
//         _.getBigDecimal(_), _.setBigDecimal(_, _), _.updateBigDecimal(_, _))

//     /** @group Instances */
//     implicit val BooleanMeta: Meta[Boolean] =
//       Meta.basic[Boolean](
//         NonEmptyList.of(Bit, JdbcBoolean),
//         NonEmptyList.of(Bit, JdbcBoolean),
//         List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Decimal, Numeric, Char, VarChar,
//           LongVarChar),
//         _.getBoolean(_), _.setBoolean(_, _), _.updateBoolean(_, _))

//     /** @group Instances */
//     implicit val StringMeta: Meta[String] =
//       Meta.basic[String](
//         NonEmptyList.of(VarChar, Char, LongVarChar, NChar, NVarChar, LongnVarChar),
//         NonEmptyList.of(Char, VarChar, LongVarChar, NChar, NVarChar, LongnVarChar),
//         List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Decimal, Numeric, Bit,
//           Binary, VarBinary, LongVarBinary, Date, Time, Timestamp),
//         _.getString(_), _.setString(_, _), _.updateString(_, _))

//     /** @group Instances */
//     implicit val ByteArrayMeta: Meta[Array[Byte]] =
//       Meta.basic[Array[Byte]](
//         NonEmptyList.of(Binary, VarBinary, LongVarBinary),
//         NonEmptyList.of(Binary, VarBinary),
//         List(LongVarBinary),
//         _.getBytes(_), _.setBytes(_, _), _.updateBytes(_, _))

//     /** @group Instances */
//     implicit val DateMeta: Meta[java.sql.Date] =
//       Meta.basic1[java.sql.Date](
//         Date,
//         List(Char, VarChar, LongVarChar, Timestamp),
//         _.getDate(_), _.setDate(_, _), _.updateDate(_, _))

//     /** @group Instances */
//     implicit val TimeMeta: Meta[java.sql.Time] =
//       Meta.basic1[java.sql.Time](
//         Time,
//         List(Char, VarChar, LongVarChar, Timestamp),
//         _.getTime(_), _.setTime(_, _), _.updateTime(_, _))

//     /** @group Instances */
//     implicit val TimestampMeta: Meta[java.sql.Timestamp] =
//       Meta.basic1[java.sql.Timestamp](
//         Timestamp,
//         List(Char, VarChar, LongVarChar, Date, Time),
//         _.getTimestamp(_), _.setTimestamp(_, _), _.updateTimestamp(_, _))

//     /** @group Instances */
//     implicit val ScalaBigDecimalMeta: Meta[BigDecimal] =
//       BigDecimalMeta.xmap(BigDecimal.apply, _.bigDecimal)

//     /** @group Instances */
//     implicit val JavaUtilDateMeta: Meta[java.util.Date] =
//       DateMeta.xmap(identity, d => new java.sql.Date(d.getTime))

//     /** @group Instances */
//     implicit val JavaTimeInstantMeta: Meta[java.time.Instant] =
//       TimestampMeta.xmap(_.toInstant, java.sql.Timestamp.from)

//     /** @group Instances */
//     implicit val JavaTimeLocalDateMeta: Meta[java.time.LocalDate] =
//       DateMeta.xmap(_.toLocalDate, java.sql.Date.valueOf)

//   }

// }
