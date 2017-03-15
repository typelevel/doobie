package doobie.util

import doobie.enum.jdbctype.{ Array => JdbcArray, Boolean => JdbcBoolean, _ }
import doobie.free.{ connection => C, resultset => RS, preparedstatement => PS, statement => S }
import doobie.util.invariant._

import java.sql.ResultSet

import scala.annotation.implicitNotFound
import scala.collection.immutable.TreeSet
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.ClassTag
import scala.Predef._

#+scalaz
import scalaz._, Scalaz._
import scalaz.NonEmptyList.{ apply => NonEmptyListOf }
#-scalaz
#+cats
import cats._, cats.data.NonEmptyList, cats.free.Coyoneda
import cats.data.NonEmptyList.{ of => NonEmptyListOf }
import cats.implicits._
import scala.util. { Either => \/, Left => -\/, Right => \/- }
#-cats

import shapeless._
import shapeless.ops.hlist.IsHCons

/** Module defining the lowest level of column mapping. */
object meta {

  /**
   * Metadata defining the column-level mapping to and from Scala type `A`. A given Scala type might
   * be read from or written to columns with a variety of JDBC and/or vendor-specific types,
   * depending on supported coercions and luck.
   *
   * Reading and writing values to JDBC is asymmetric with respect to `null`, is complicated by
   * unboxed types, and is not consistent with idiomatic Scala; so some discussion is required.
   * Scala values should never be `null`. Setting a `NULL` JDBC value is accomplished via the
   * `setNull` operation. Similarly when getting a JDBC value we must subsequently ask `.wasNull`
   * on the JDBC resource and decide how to handle the value. The `Atom` typeclass takes care of
   * mapping nullable values to `Option` so these issues should not be a concern for casual users.
   */
  @implicitNotFound("Could not find an instance of Meta[${A}]; you can construct one based on a primitive instance via `xmap`.")
  sealed trait Meta[A] {

    /**
     * Get operation, split into the underlying column read (which must subsequently be checked for
     * null) followed by the accumulated output map. Because the output map may be undefined for the
     * null value of the underlying column type, it may not be possible to yield an `A`. Logic to
     * handle this case correctly is provided in `Atom`.
     */
    def coyo: Coyoneda[(ResultSet, Int) => ?, A]

    /**
     * Name of the Scala type, for diagnostic purposes. Smart constructors require a `TypeTag` to
     * guarantee this value is correct.
     */
    def scalaType: String

    /** Destination JDBC types to which values of type `A` can be written. */
    def jdbcTarget: NonEmptyList[JdbcType]

    /** Source JDBC types from which values of type `A` can be read. */
    def jdbcSource: NonEmptyList[JdbcType]

    /** Switch on the flavor of this `Meta`. */
    def fold[B](f: BasicMeta[A] => B, g: AdvancedMeta[A] => B): B

    /** Constructor for a `setXXX` operation for a given `A` at a given index. */
    val set: (Int, A) => PS.PreparedStatementIO[Unit]

    /** Constructor for an `updateXXX` operation for a given `A` at a given index. */
    val update: (Int, A) => RS.ResultSetIO[Unit]

    /** Constructor for a `setNull` operation for the primary JDBC type, at a given index. */
    val setNull: Int => PS.PreparedStatementIO[Unit]

    /** Invariant map. */
    def xmap[B: TypeTag](f: A => B, g: B => A): Meta[B]

    /**
     * Invariant map with `null` handling, for `A, B >: Null`; the functions `f` and `g` will
     * never be passed a `null` value.
     */
    @deprecated("Null is no longer observable here; just use xmap.", "0.4.2")
    def nxmap[B >: Null : TypeTag](f: A => B, g: B => A)(implicit ev: Null <:< A): Meta[B] =
      xmap(f, g)

  }

  /**
   * `Meta` for "basic" JDBC types as defined by the specification. These include the basic numeric
   * and text types with distinct `get/setXXX` methods and fixed mappings that ostensibly work for
   * all compliant drivers. These types defined both "recommended" source types (`jdbcSource` here)
   * and "supported" types (`jdbcSourceSecondary`) which drivers must not reject outright, although
   * in many cases coercion failures are likely (reading an `Int` from a `VarChar` for instance) so
   * these mappings should be viewed with suspicion.
   */
  sealed trait BasicMeta[A] extends Meta[A]  { outer =>

    /** Supported but non-recommended source JDBC sources (see trait description above). */
    val jdbcSourceSecondary: List[JdbcType]

    /** True if `A` can be written to a column or 'in' parameter with the specified `JdbcType`. */
    def canWriteTo(jdbc: JdbcType): Boolean =
      jdbcTarget.element(jdbc)

    /** True if `A` can be read from a column or 'out' parameter with the specified `JdbcType`. */
    def canReadFrom(jdbc: JdbcType): Boolean =
      jdbcSource.element(jdbc)

    /**
     * True if `A` might be readable from a column or 'out' parameter with the specified `JdbcType`,
     * taking into account non-recommended source types specified in `jdbcSourceSecondary`.
     */
    def mightReadFrom(jdbc: JdbcType): Boolean =
      jdbcSource.element(jdbc) || jdbcSourceSecondary.element(jdbc)

    /** Constructor for a `setNull` operation for the primary JDBC type, at a given index. */
    val setNull: Int => PS.PreparedStatementIO[Unit] = i =>
      PS.setNull(i, jdbcTarget.head.toInt)

    def fold[B](f: BasicMeta[A] => B, g: AdvancedMeta[A] => B): B =
      f(this)

    def xmap[B](f: A => B, g: B => A)(implicit ev: TypeTag[B]): Meta[B] =
      new BasicMeta[B] {
        val coyo                = outer.coyo.map(f)
        val jdbcSource          = outer.jdbcSource
        val jdbcTarget          = outer.jdbcTarget
        val scalaType           = ev.tpe.toString
        val set                 = (n: Int, b: B) => outer.set(n, g(b))
        val update              = (n: Int, b: B) => outer.update(n, g(b))
        val jdbcSourceSecondary = outer.jdbcSourceSecondary
      } <| Meta.reg

  }


  /**
   * `Meta` for "advanced" JDBC types as defined by the specification. These include `Array`,
   * `JavaObject`, `Struct`, and other types that require driver, schema, or vendor-specific
   * knowledge and are unlikely to be portable between vendors (or indeed between applications).
   * These mappings require (in addition to matching JDBC types) matching driver, schema, or
   * vendor-specific data types, sadly given as `String`s in JDBC.
   */
  sealed trait AdvancedMeta[A] extends Meta[A] { outer =>

    /**
     * List of schema types to which values of type `A` can be written and from which they can be
     * read. Databases will often have several names for the same type, and the JDBC driver may
     * report an alias that doesn't appear in the schema or indeed in the database documentation.
     * This field is therefore a list.
     */
    val schemaTypes: NonEmptyList[String]

    /**
     * True if `A` can be written to a column or 'in' parameter with the specified `JdbcType` and
     * schema types.
     */
    def canWriteTo(jdbc: JdbcType, schema: String): Boolean =
      schemaTypes.element(schema) && jdbcTarget.element(jdbc)

    /**
     * True if `A` can be read from a column or 'out' parameter with the specified `JdbcType` and
     * schema types.
     */
    def canReadFrom(jdbc: JdbcType, schema: String): Boolean =
      schemaTypes.element(schema) && jdbcSource.element(jdbc)

    /** Constructor for a `setNull` operation for the primary JDBC type, at a given index. */
    val setNull: Int => PS.PreparedStatementIO[Unit] = i =>
      PS.setNull(i, jdbcTarget.head.toInt, schemaTypes.head)

    def fold[B](f: BasicMeta[A] => B, g: AdvancedMeta[A] => B): B =
      g(this)

    def xmap[B](f: A => B, g: B => A)(implicit ev: TypeTag[B]): Meta[B] =
      new AdvancedMeta[B] {
        val coyo        = outer.coyo.map(f)
        val jdbcSource  = outer.jdbcSource
        val jdbcTarget  = outer.jdbcTarget
        val scalaType   = ev.tpe.toString
        val set         = (n: Int, b: B) => outer.set(n, g(b))
        val update      = (n: Int, b: B) => outer.update(n, g(b))
        val schemaTypes = outer.schemaTypes
      } <| Meta.reg

  }


  /** Constructors, accessors, and typeclass instances. */
  object Meta extends {

    /** @group Typeclass Instances */
    implicit val MetaOrder: Order[Meta[_]] =
#+scalaz
      Order.orderBy(_.fold(
#-scalaz
#+cats
      // Type argument necessary to avoid spurious "illegal cyclic reference involving object Meta"
      // only in Scala 2.11, and only with cats for whatever reason. Confidence high!
      Order.by[Meta[_], (String, NonEmptyList[JdbcType], NonEmptyList[JdbcType], List[JdbcType]) \/ (String, NonEmptyList[JdbcType], NonEmptyList[JdbcType], NonEmptyList[String])](_.fold(
#-cats
        b => -\/((b.scalaType, b.jdbcTarget, b.jdbcSource, b.jdbcSourceSecondary)),
        a => \/-((a.scalaType, a.jdbcTarget, a.jdbcSource, a.schemaTypes))))

    /** @group Typeclass Instances */
    implicit val MetaOrdering: scala.Ordering[Meta[_]] =
#+scalaz
      MetaOrder.toScalaOrdering
#-scalaz
#+cats
      MetaOrder.toOrdering
#-cats

    // See note on trait Meta above
    private var instances: TreeSet[Meta[_]] = TreeSet.empty // scalastyle:ignore

  } with LowPriorityImplicits with MetaInstances {

    // sorry
    def reg(m: Meta[_]): Unit =
      synchronized { instances = instances + m }

    implicit lazy val JdbcTypeMeta: Meta[doobie.enum.jdbctype.JdbcType] =
      IntMeta.xmap(doobie.enum.jdbctype.JdbcType.unsafeFromInt, _.toInt)

    def apply[A](implicit A: Meta[A]): Meta[A] = A

    /**
     * Computes the set of know `Meta`s that support reading the indicated schema type.
     * @group Accessors
     */
    def readersOf(jdbc: JdbcType, schema: String): TreeSet[Meta[_]] =
      instances.filter(_.fold(_.canReadFrom(jdbc), _.canReadFrom(jdbc, schema)))

    /**
     * Computes the set of know `Meta`s that support writing the indicated schema type.
     * @group Accessors
     */
    def writersOf(jdbc: JdbcType, schema: String): TreeSet[Meta[_]] =
      instances.filter(_.fold(_.canWriteTo(jdbc), _.canWriteTo(jdbc, schema)))

    /**
     * Construct a `BasicMeta` for the given type.
     * @group Constructors
     */
    def basic[A](
      jdbcTarget0: NonEmptyList[JdbcType],
      jdbcSource0: NonEmptyList[JdbcType],
      jdbcSourceSecondary0: List[JdbcType],
      get0: (ResultSet, Int) => A,
      set0: (Int, A) => PS.PreparedStatementIO[Unit],
      update0: (Int, A) => RS.ResultSetIO[Unit]
    )(implicit ev: TypeTag[A]): BasicMeta[A] =
      new BasicMeta[A] {
        val scalaType           = ev.tpe.toString
        val jdbcTarget          = jdbcTarget0
        val jdbcSource          = jdbcSource0
        val jdbcSourceSecondary = jdbcSourceSecondary0
        val coyo                = Coyoneda.lift[(ResultSet, Int) => ?, A](get0)
        val set                 = set0
        val update              = update0
      } <| reg

    /**
     * Construct a `BasicMeta` for the given type, with symmetric primary mappings.
     * @group Constructors
     */
    def basic1[A](
      jdbcType: JdbcType,
      jdbcSourceSecondary0: List[JdbcType],
      get0: (ResultSet, Int) => A,
      set0: (Int, A) => PS.PreparedStatementIO[Unit],
      update0: (Int, A) => RS.ResultSetIO[Unit]
    )(implicit ev: TypeTag[A]): BasicMeta[A] =
      basic(NonEmptyListOf(jdbcType), NonEmptyListOf(jdbcType), jdbcSourceSecondary0, get0, set0, update0)

    /**
     * Construct an `AdvancedMeta` for the given type.
     * @group Constructors
     */
    def advanced[A](
      jdbcTypes: NonEmptyList[JdbcType],
      schemaTypes0: NonEmptyList[String],
      get: (ResultSet, Int) => A,
      set0: (Int, A) => PS.PreparedStatementIO[Unit],
      update0: (Int, A) => RS.ResultSetIO[Unit]
    )(implicit ev: TypeTag[A]): AdvancedMeta[A] =
      new AdvancedMeta[A] {
        val scalaType   = ev.tpe.toString
        val jdbcTarget  = jdbcTypes
        val jdbcSource  = jdbcTypes
        val schemaTypes = schemaTypes0
        val coyo        = Coyoneda.lift[(ResultSet, Int) => ?, A](get)
        val set         = set0
        val update      = update0
      } <| reg

    /**
     * Construct an `AdvancedMeta` for the given type, mapped as JDBC `Array`.
     * @group Constructors
     */
    def array[A >: Null <: AnyRef: TypeTag](elementType: String, schemaH: String, schemaT: String*): AdvancedMeta[Array[A]] =
      advanced[Array[A]](NonEmptyListOf(JdbcArray), NonEmptyListOf(schemaH, schemaT : _*),
        { (r, n) =>
          val a = r.getArray(n)
          (if (a == null) null else a.getArray).asInstanceOf[Array[A]]
        },
        (n, a) =>
          for {
            conn <- PS.getConnection
            arr  <- PS.lift(conn, C.createArrayOf(elementType, a.asInstanceOf[Array[AnyRef]]))
            _    <- PS.setArray(n, arr)
          } yield (),
        (n, a) =>
          for {
            stmt <- RS.getStatement // somewhat irritating; no getConnection on ResultSet
            conn <- RS.lift(stmt, S.getConnection)
            arr  <- RS.lift(conn, C.createArrayOf(elementType, a.asInstanceOf[Array[AnyRef]]))
            _    <- RS.updateArray(n, arr)
          } yield ()
        )

    /**
     * Construct an `AdvancedMeta` for the given type, mapped as JDBC `Other,JavaObject`.
     * @group Constructors
     */
    def other[A >: Null <: AnyRef: TypeTag](schemaH: String, schemaT: String*)(implicit A: ClassTag[A]): AdvancedMeta[A] =
      advanced[A](NonEmptyListOf(Other, JavaObject), NonEmptyListOf(schemaH, schemaT : _*),
        _.getObject(_) match {
          case null => null
          case a    =>
            // force the cast here rather than letting a potentially ill-typed value escape
            try A.runtimeClass.cast(a).asInstanceOf[A]
            catch {
              case _: ClassCastException => throw InvalidObjectMapping(A.runtimeClass, a.getClass)
            }
        },
        PS.setObject(_: Int, _: A),
        RS.updateObject(_: Int, _: A))

    // /**
    //  * Construct an `AdvancedMeta` for the given type, mapped as JDBC `Struct`.
    //  * @group Constructors
    //  */
    // def struct[A: TypeTag](schemaH: String, schemaT: String*): AdvancedMeta[A] =
    //   advanced[A](NonEmptyListOf(Struct), NonEmptyListOf(schemaH, schemaT : _*))

    /** @group Instances */
    implicit def ArrayTypeAsListMeta[A: ClassTag: TypeTag](implicit ev: Meta[Array[A]]): Meta[List[A]] =
      ev.xmap(a => if (a == null) null else a.toList, a => if (a == null) null else a.toArray)

    /** @group Instances */
    implicit def ArrayTypeAsVectorMeta[A: ClassTag: TypeTag](implicit ev: Meta[Array[A]]): Meta[Vector[A]] =
      ev.xmap(a => if (a == null) null else a.toVector  , a => if (a == null) null else a.toArray)

    /**
      * Derive Meta for nullable unary product types.
      * A - type for which instance is derived
      * L - HList representation of type A
      * H - type of the head of L (this is the only type in L)
      * T - type of the tail of L (unused)
      * @group Instances
      */
    implicit def unaryProductMetaNullable[A: TypeTag, L <: HList, H >: Null, T <: HList](
      // representation (L) for type A
      implicit gen: Generic.Aux[A, L],
      // head (H) and tail (T) type of representation (L)
      c: IsHCons.Aux[L, H, T],
      // Meta instance for the head (the only element in representation)
      hmeta: Lazy[Meta[H]],
      // provide evidence that representation (L) and singleton hlist with
      // the only element of type H are the same type
      ev: =:=[H :: HNil, L]
    ): Meta[A] = hmeta.value.xmap[A](
      // `from` converts representation L to A, but there is only H here,
      // but provided evidence `=:=[H :: HNil, L]` we can construct L from H
      // and A from L (using `from`)
      h => gen.from(h :: HNil),
      // `to` converts A to representation L, it's Meta[H], so H is required.
      // H is just a head of representation
      a => gen.to(a).head
    )

  }

  trait LowPriorityImplicits {
    /**
      * Same as `unaryProductMetaNullable` for non-nullable unary products
      * @group Instances
      */
    implicit def unaryProductMetaNonNullable[A : TypeTag, L <: HList, H, T <: HList](
      implicit gen: Generic.Aux[A, L],
      c: IsHCons.Aux[L, H, T],
      hmeta: Lazy[Meta[H]],
      ev: =:=[H :: HNil, L]
    ): Meta[A] = hmeta.value.xmap[A](h => gen.from(h :: HNil), a => gen.to(a).head)
  }

  // Instances for basic types, according to the JDBC spec
  trait MetaInstances {

    /** @group Instances */
    implicit val ByteMeta = Meta.basic1[Byte](
      TinyInt,
      List(SmallInt, Integer, BigInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
        LongVarChar),
      _.getByte(_), PS.setByte, RS.updateByte)

    /** @group Instances */
    implicit val ShortMeta = Meta.basic1[Short](
      SmallInt,
      List(TinyInt, Integer, BigInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
        LongVarChar),
      _.getShort(_), PS.setShort, RS.updateShort)

    /** @group Instances */
    implicit val IntMeta = Meta.basic1[Int](
      Integer,
      List(TinyInt, SmallInt, BigInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
        LongVarChar),
      _.getInt(_), PS.setInt, RS.updateInt)

    /** @group Instances */
    implicit val LongMeta = Meta.basic1[Long](
      BigInt,
      List(TinyInt, Integer, SmallInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
        LongVarChar),
      _.getLong(_), PS.setLong, RS.updateLong)

    /** @group Instances */
    implicit val FloatMeta = Meta.basic1[Float](
      Real,
      List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
        LongVarChar),
      _.getFloat(_), PS.setFloat, RS.updateFloat)

    /** @group Instances */
    implicit val DoubleMeta = Meta.basic[Double](
      NonEmptyListOf(Double),
      NonEmptyListOf(Float, Double),
      List(TinyInt, Integer, SmallInt, BigInt, Float, Real, Decimal, Numeric, Bit, Char, VarChar,
        LongVarChar),
      _.getDouble(_), PS.setDouble, RS.updateDouble)

    /** @group Instances */
    implicit val BigDecimalMeta = Meta.basic[java.math.BigDecimal](
      NonEmptyListOf(Numeric),
      NonEmptyListOf(Decimal, Numeric),
      List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Bit, Char, VarChar,
        LongVarChar),
      _.getBigDecimal(_), PS.setBigDecimal, RS.updateBigDecimal)

    /** @group Instances */
    implicit val BooleanMeta = Meta.basic[Boolean](
      NonEmptyListOf(Bit, JdbcBoolean),
      NonEmptyListOf(Bit, JdbcBoolean),
      List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Decimal, Numeric, Char, VarChar,
        LongVarChar),
      _.getBoolean(_), PS.setBoolean, RS.updateBoolean)

    /** @group Instances */
    implicit val StringMeta = Meta.basic[String](
      NonEmptyListOf(VarChar, Char, LongVarChar),
      NonEmptyListOf(Char, VarChar),
      List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Decimal, Numeric, Bit,
        LongVarChar, Binary, VarBinary, LongVarBinary, Date, Time, Timestamp),
      _.getString(_), PS.setString, RS.updateString)

    /** @group Instances */
    implicit val ByteArrayMeta = Meta.basic[Array[Byte]](
      NonEmptyListOf(Binary, VarBinary, LongVarBinary),
      NonEmptyListOf(Binary, VarBinary),
      List(LongVarBinary),
      _.getBytes(_), PS.setBytes, RS.updateBytes)

    /** @group Instances */
    implicit val DateMeta = Meta.basic1[java.sql.Date](
      Date,
      List(Char, VarChar, LongVarChar, Timestamp),
      _.getDate(_), PS.setDate, RS.updateDate)

    /** @group Instances */
    implicit val TimeMeta = Meta.basic1[java.sql.Time](
      Time,
      List(Char, VarChar, LongVarChar, Timestamp),
      _.getTime(_), PS.setTime, RS.updateTime)

    /** @group Instances */
    implicit val TimestampMeta = Meta.basic1[java.sql.Timestamp](
      Timestamp,
      List(Char, VarChar, LongVarChar, Date, Time),
      _.getTimestamp(_), PS.setTimestamp, RS.updateTimestamp)

    /** @group Instances */
    implicit val ScalaBigDecimalMeta: Meta[BigDecimal] =
      BigDecimalMeta.xmap(
        a => if (a == null) null else BigDecimal(a),
        a => if (a == null) null else a.bigDecimal)

    /** @group Instances */
    implicit val JavaUtilDateMeta: Meta[java.util.Date] =
      DateMeta.xmap(
        a => a,
        d => if (d == null) null else new java.sql.Date(d.getTime))

    /** @group Instances */
    implicit val JavaTimeInstantMeta: Meta[java.time.Instant] =
      TimestampMeta.xmap(
        a => if (a == null) null else a.toInstant,
        i => if (i == null) null else java.sql.Timestamp.from(i))

    /** @group Instances */
    implicit val JavaTimeLocalDateMeta: Meta[java.time.LocalDate] =
      DateMeta.xmap(
        a => if (a == null) null else a.toLocalDate,
        d => if (d == null) null else java.sql.Date.valueOf(d))
  }

}
