package doobie.util

import doobie.enum.jdbctype.{ Array => JdbcArray, _ }
import doobie.free.{ connection => C, resultset => RS, preparedstatement => PS, statement => S }
import doobie.util.invariant._

import scala.annotation.implicitNotFound
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.ClassTag
import scala.Predef._

import scalaz._, Scalaz._

object meta {

  /** 
   * Metadata defining the column-level mapping to and from Scala type `A`. A given Scala type might
   * be read from or written to columns with a variety of JDBC and/or vendor-specific types,
   * depending on supported coercions and luck.
   */
  @implicitNotFound("Could not find an instance of Meta[${A}]; you can construct one based on a primitive instance via `xmap`.")
  sealed trait Meta[A] {

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

    /** Constructor for a `getXXX` operation for type `A` at a given index. */
    val get: Int => RS.ResultSetIO[A] 

    /** Constructor for a `setXXX` operation for a given `A` at a given index. */
    val set: (Int, A) => PS.PreparedStatementIO[Unit] 

    /** Constructor for an `updateXXX` operation for a given `A` at a given index. */
    val update: (Int, A) => RS.ResultSetIO[Unit] 

    /** Constructor for a `setNull` operation for the primary JDBC type, at a given index. */
    val setNull: Int => PS.PreparedStatementIO[Unit] = i =>
      PS.setNull(i, jdbcTarget.head.toInt)

    // Not quite an invariant functor because of the tag constraint, but I think it's worth the
    // sacrifice because we get much better diagnostic information as a result.
    def xmap[B: TypeTag](f: A => B, g: B => A): Meta[B]

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

  }

  /**
   * `Meta` for "advanced" JDBC types as defined by the specification. These include `Array`,
   * `JavaObject`, `Struct`, and other types that require driver, schema, or vendor-specific 
   * knowledge and are unlikely to be portable between vendors (or indeed between applications).
   * These mappings require (in addition to matching JDBC types) matching driver, schema, or 
   * vendor-specific data types, sadly given as `String`s in JDBC.
   */  
  sealed trait AdvancedMeta[A] extends Meta[A] {

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

  }

  /** Constructors, accessors, and typeclass instances. */
  object Meta extends {

    // See note on trait Meta above
    private var instances: ISet[Meta[_]] = ISet.empty

    /** @group Typeclass Instances */
    implicit val MetaOrder: Order[Meta[_]] =
      Order.orderBy(_.fold(
        b => -\/((b.scalaType, b.jdbcTarget, b.jdbcSource, b.jdbcSourceSecondary)),
        a => \/-((a.scalaType, a.jdbcTarget, a.jdbcSource, a.schemaTypes))))
  
  } with MetaInstances {

    // sorry
    private def reg(m: Meta[_]): Unit = 
      synchronized { instances = instances.insert(m) }

    def apply[A](implicit A: Meta[A]): Meta[A] = A

    /** 
     * Computes the set of know `Meta`s that support reading the indicated schema type. 
     * @group Accessors
     */
    def readersOf(jdbc: JdbcType, schema: String): ISet[Meta[_]] =
      instances.filter(_.fold(_.canReadFrom(jdbc), _.canReadFrom(jdbc, schema)))

    /** 
     * Computes the set of know `Meta`s that support writing the indicated schema type. 
     * @group Accessors
     */
    def writersOf(jdbc: JdbcType, schema: String): ISet[Meta[_]] =
      instances.filter(_.fold(_.canWriteTo(jdbc), _.canWriteTo(jdbc, schema)))

    /**
     * Construct a `BasicMeta` for the given type.
     * @group Constructors     
     */
    def basic[A](
      jdbcTarget0: NonEmptyList[JdbcType],
      jdbcSource0: NonEmptyList[JdbcType],
      jdbcSourceSecondary0: List[JdbcType],
      get0: Int => RS.ResultSetIO[A],
      set0: (Int, A) => PS.PreparedStatementIO[Unit],
      update0: (Int, A) => RS.ResultSetIO[Unit] 
    )(implicit ev: TypeTag[A]): BasicMeta[A] =
      new BasicMeta[A] {
        val scalaType = ev.tpe.toString
        val jdbcTarget = jdbcTarget0
        val jdbcSource = jdbcSource0
        val jdbcSourceSecondary = jdbcSourceSecondary0
        def fold[B](f: BasicMeta[A] => B, g: AdvancedMeta[A] => B) = f(this)
        val (get, set, update) = (get0, set0, update0)
        def xmap[B: TypeTag](f: A => B, g: B => A): Meta[B] = 
          basic[B](jdbcTarget, jdbcSource, jdbcSourceSecondary, n => get(n).map(f), 
            (n, b) => set(n, g(b)), (n, b) => update(n, g(b)))
      } <| reg

    /**
     * Construct a `BasicMeta` for the given type, with symmetric primary mappings.
     * @group Constructors     
     */
    def basic1[A](
      jdbcType: JdbcType,
      jdbcSourceSecondary0: List[JdbcType],
      get0: Int => RS.ResultSetIO[A],
      set0: (Int, A) => PS.PreparedStatementIO[Unit],
      update0: (Int, A) => RS.ResultSetIO[Unit] 
    )(implicit ev: TypeTag[A]): BasicMeta[A] =
      basic(NonEmptyList(jdbcType), NonEmptyList(jdbcType), jdbcSourceSecondary0, get0, set0, update0)

    /**
     * Construct an `AdvancedMeta` for the given type.
     * @group Constructors     
     */
    def advanced[A](
      jdbcTypes: NonEmptyList[JdbcType],
      schemaTypes0: NonEmptyList[String],
      get0: Int => RS.ResultSetIO[A],
      set0: (Int, A) => PS.PreparedStatementIO[Unit],
      update0: (Int, A) => RS.ResultSetIO[Unit]
    )(implicit ev: TypeTag[A]): AdvancedMeta[A] =
      new AdvancedMeta[A] {
        val scalaType = ev.tpe.toString
        val jdbcTarget = jdbcTypes
        val jdbcSource = jdbcTypes
        val schemaTypes = schemaTypes0
        def fold[B](f: BasicMeta[A] => B, g: AdvancedMeta[A] => B) = g(this)
        val (get, set, update) = (get0, set0, update0)
        def xmap[B: TypeTag](f: A => B, g: B => A): Meta[B] = 
          advanced[B](jdbcTypes, schemaTypes, n => get(n).map(f), (n, b) => set(n, g(b)), 
            (n, b) => update(n, g(b)))
      } <| reg

    /**
     * Construct an `AdvancedMeta` for the given type, mapped as JDBC `Array`.
     * @group Constructors     
     */
    def array[A >: Null <: AnyRef: TypeTag](elementType: String, schemaH: String, schemaT: String*): AdvancedMeta[Array[A]] =
      advanced[Array[A]](NonEmptyList(JdbcArray), NonEmptyList(schemaH, schemaT : _*),
        RS.getArray(_: Int).map(a => (if (a == null) null else a.getArray).asInstanceOf[Array[A]]),
        (n, a) =>
          for {
            conn <- PS.getConnection
            arr  <- PS.liftConnection(conn, C.createArrayOf(elementType, a.asInstanceOf[Array[AnyRef]]))
            _    <- PS.setArray(n, arr)
          } yield (),
        (n, a) => 
          for {
            stmt <- RS.getStatement // somewhat irritating; no getConnection on ResultSet
            conn <- RS.liftStatement(stmt, S.getConnection)
            arr  <- RS.liftConnection(conn, C.createArrayOf(elementType, a.asInstanceOf[Array[AnyRef]]))
            _    <- RS.updateArray(n, arr)
          } yield ()
        )

    /**
     * Construct an `AdvancedMeta` for the given type, mapped as JDBC `JavaObject/Other`.
     * @group Constructors     
     */
    def other[A >: Null <: AnyRef: TypeTag](schemaH: String, schemaT: String*)(implicit A: ClassTag[A]): AdvancedMeta[A] =
      advanced[A](NonEmptyList(JavaObject, Other), NonEmptyList(schemaH, schemaT : _*),
        RS.getObject(_: Int).map { 
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
    //   advanced[A](NonEmptyList(Struct), NonEmptyList(schemaH, schemaT : _*))

    /** @group Instances */
    implicit def ArrayTypeAsListMeta[A: ClassTag: TypeTag](implicit ev: Meta[Array[A]]): Meta[List[A]] =
      ev.xmap(a => if (a == null) null else a.toList, a => if (a == null) null else a.toArray)

    /** @group Instances */
    implicit def ArrayTypeAsVectorMeta[A: ClassTag: TypeTag](implicit ev: Meta[Array[A]]): Meta[Vector[A]] =
      ev.xmap(a => if (a == null) null else a.toVector  , a => if (a == null) null else a.toArray)

  }

  // Instances for basic types, according to the JDBC spec
  trait MetaInstances {

    /** @group Instances */
    implicit val ByteMeta = Meta.basic1[Byte](
      TinyInt, 
      List(SmallInt, Integer, BigInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar, 
        LongVarChar),
      RS.getByte, PS.setByte, RS.updateByte)

    /** @group Instances */
    implicit val ShortMeta = Meta.basic1[Short](
      SmallInt, 
      List(TinyInt, Integer, BigInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar, 
        LongVarChar),
      RS.getShort, PS.setShort, RS.updateShort)

    /** @group Instances */
    implicit val IntMeta = Meta.basic1[Int](
      Integer, 
      List(TinyInt, SmallInt, BigInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar, 
        LongVarChar),
      RS.getInt, PS.setInt, RS.updateInt)

    /** @group Instances */
    implicit val LongMeta = Meta.basic1[Long](
      BigInt, 
      List(TinyInt, Integer, SmallInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar, 
        LongVarChar),
      RS.getLong, PS.setLong, RS.updateLong)

    /** @group Instances */
    implicit val FloatMeta = Meta.basic1[Float](
      Real, 
      List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Decimal, Numeric, Bit, Char, VarChar, 
        LongVarChar),
      RS.getFloat, PS.setFloat, RS.updateFloat)

    /** @group Instances */
    implicit val DoubleMeta = Meta.basic[Double](
      NonEmptyList(Double), 
      NonEmptyList(Float, Double),
      List(TinyInt, Integer, SmallInt, BigInt, Float, Real, Decimal, Numeric, Bit, Char, VarChar, 
        LongVarChar),
      RS.getDouble, PS.setDouble, RS.updateDouble)

    /** @group Instances */
    implicit val BigDecimalMeta = Meta.basic[java.math.BigDecimal](
      NonEmptyList(Numeric), 
      NonEmptyList(Decimal, Numeric), 
      List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Bit, Char, VarChar, 
        LongVarChar),
      RS.getBigDecimal, PS.setBigDecimal, RS.updateBigDecimal)

    /** @group Instances */
    implicit val BooleanMeta = Meta.basic1[Boolean](
      Bit, 
      List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Decimal, Numeric, Char, VarChar, 
        LongVarChar),
      RS.getBoolean, PS.setBoolean, RS.updateBoolean)

    /** @group Instances */
    implicit val StringMeta = Meta.basic[String](
      NonEmptyList(VarChar, Char, LongVarChar),
      NonEmptyList(Char, VarChar),
      List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Decimal, Numeric, Bit, 
        LongVarChar, Binary, VarBinary, LongVarBinary, Date, Time, Timestamp),
      RS.getString, PS.setString, RS.updateString)
  
    /** @group Instances */
    implicit val ByteArrayMeta = Meta.basic[Array[Byte]](
      NonEmptyList(Binary, VarBinary, LongVarBinary),
      NonEmptyList(Binary, VarBinary),
      List(LongVarBinary),
      RS.getBytes, PS.setBytes, RS.updateBytes)

    /** @group Instances */
    implicit val DateMeta = Meta.basic1[java.sql.Date](
      Date,
      List(Char, VarChar, LongVarChar, Timestamp),
      RS.getDate, PS.setDate, RS.updateDate)

    /** @group Instances */
    implicit val TimeMeta = Meta.basic1[java.sql.Time](
      Time,
      List(Char, VarChar, LongVarChar, Timestamp),
      RS.getTime, PS.setTime, RS.updateTime)

    /** @group Instances */
    implicit val TimestampMeta = Meta.basic1[java.sql.Timestamp](
      Timestamp,
      List(Char, VarChar, LongVarChar, Date, Time),
      RS.getTimestamp, PS.setTimestamp, RS.updateTimestamp)

    /** @group Instances */
    implicit val ScalaBigDecimalMeta: Meta[BigDecimal] =
      BigDecimalMeta.xmap(BigDecimal(_), _.bigDecimal)

    /** @group Instances */
    implicit val JavaUtilDateMeta: Meta[java.util.Date] =
      DateMeta.xmap(a => a, d => new java.sql.Date(d.getTime))

  }

}

