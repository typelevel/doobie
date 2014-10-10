package doobie.util

import doobie.enum.jdbctype._
import doobie.free.{ resultset => RS, preparedstatement => PS }

import scala.annotation.implicitNotFound
import scala.reflect.runtime.universe.TypeTag

import scalaz.NonEmptyList
import scalaz.syntax.equal._

object scalatype {

  /**
   * A Scala type together with its mapping to primitive JDBC operations and types. This type is
   * the basis for construction of `Atom` instances, which add logic for handling `NULL` and
   * compose to form generic mappings over product types.
   */
  @implicitNotFound("Could not find an instance of ScalaType[${A}]; you can construct one based on a primitive instance via `xmap`.")
  sealed trait ScalaType[A] { outer =>
    
    /** 
     * Tag for the underlying Scala type, used exclusively for error reporting. Note that `ScalaType`
     * is an invariant functor, so this tag may not be for `A`; it may be for an underlying type
     * that has been mapped over. This may simply turn into a String, so don't try anything fancy.
     */
    val tag: TypeTag[_]

    /** 
     * The primary target JDBC type for `set` operations. The JDBC spec defines exactly one such 
     * type, although coercion to wider secondary target types are allowed in some cases; when
     * available these are given in `secondaryTargets`.
     */
    val primaryTarget: JdbcType

    /** 
     * Secondary target JDBC types for `set` operations. In most cases this list will be empty, but
     * for example in the case of `String/VARCHAR` the secondary type `LONGVARCHAR` is allowed.
     */
    val secondaryTargets: List[JdbcType]

    /** Constructor for a `getXXX` operation for type `A` at a given index. */
    val get: Int => RS.ResultSetIO[A]

    /** Constructor for a `setXXX` operation for a given `A` at a given index. */
    val set: (Int, A) => PS.PreparedStatementIO[Unit]

    /** Constructor for an `updateXXX` operation for a given `A` at a given index. */
    val update: (Int, A) => RS.ResultSetIO[Unit]

    /** Constructor for a `setNull` operation for the primary JDBC type, at a given index. */
    val setNull: Int => PS.PreparedStatementIO[Unit] = i =>
      PS.setNull(i, primaryTarget.toInt)

    /** 
     * Primary JDBC types for which all JDBC drivers must provide suitable coercions to `A`. All
     * such coercions are likely to succeed, modulo differences in precision for some numeric types.
     */
    val primarySources: NonEmptyList[JdbcType]

    /**
     * Secondary JDBC types for which all JDBC drivers must provide suitable coercions to `A`. The
     * coercions here are in some cases highly optimistic (`VARCHAR` to `Int` for example) and
     * should be viewed with some degree of skepticism.
     */
    val secondarySources: List[JdbcType]

    /** Invariant map to an isomorphic type `B`. */
    def xmap[B](f: A => B, g: B => A): ScalaType[B] =
      new ScalaType[B] {
        val tag = outer.tag
        val primaryTarget = outer.primaryTarget
        val secondaryTargets = outer.secondaryTargets
        val get = (n: Int) => outer.get(n).map(f)
        val set = (n: Int, b: B) => outer.set(n, g(b))
        val update = (n: Int, b: B) => outer.update(n, g(b))
        val primarySources = outer.primarySources
        val secondarySources = outer.secondarySources
      }

  }

  object ScalaType {

    def apply[A](implicit A: ScalaType[A]): ScalaType[A] = A

    /** The primitive instances. */
    lazy val instances: List[ScalaType[_]] = 
      List(ByteType, ShortType, IntType, LongType, FloatType, DoubleType, BigDecimalType, 
        BooleanType, StringType, ByteArrayType, DateType, TimeType, TimestampType)

    def forPrimaryTarget(t: JdbcType): Option[ScalaType[_]] =
      instances.find(_.primaryTarget === t)

    implicit val ByteType = new ScalaType[Byte] {
      val tag = Predef.implicitly[TypeTag[Byte]]
      val primaryTarget = TinyInt
      val secondaryTargets = Nil
      val get = RS.getByte(_: Int)
      val set = PS.setByte(_: Int, _: Byte)
      val update = RS.updateByte(_: Int, _:Byte)
      val primarySources = NonEmptyList(TinyInt)
      val secondarySources = List(SmallInt, Integer, BigInt, Real, Float, Double, Decimal, Numeric, 
        Bit, Char, VarChar, LongVarChar)
    }

    implicit val ShortType = new ScalaType[Short] {
      val tag = Predef.implicitly[TypeTag[Short]]
      val primaryTarget = SmallInt
      val secondaryTargets = Nil
      val get = RS.getShort(_: Int)
      val set = PS.setShort(_: Int, _: Short)
      val update = RS.updateShort(_: Int, _:Short)
      val primarySources = NonEmptyList(SmallInt)
      val secondarySources = List(TinyInt, Integer, BigInt, Real, Float, Double, Decimal, Numeric, 
        Bit, Char, VarChar, LongVarChar)
    }

    implicit val IntType = new ScalaType[Int] {
      val tag = Predef.implicitly[TypeTag[Int]]
      val primaryTarget = Integer
      val secondaryTargets = Nil
      val get = RS.getInt(_: Int)
      val set = PS.setInt(_: Int, _: Int)
      val update = RS.updateInt(_: Int, _:Int)
      val primarySources = NonEmptyList(Integer)
      val secondarySources = List(TinyInt, SmallInt, BigInt, Real, Float, Double, Decimal, Numeric, 
        Bit, Char, VarChar, LongVarChar)
    }

    implicit val LongType = new ScalaType[Long] {
      val tag = Predef.implicitly[TypeTag[Long]]
      val primaryTarget = BigInt
      val secondaryTargets = Nil
      val get = RS.getLong(_: Int)
      val set = PS.setLong(_: Int, _: Long)
      val update = RS.updateLong(_: Int, _:Long)
      val primarySources = NonEmptyList(BigInt)
      val secondarySources = List(TinyInt, Integer, SmallInt, Real, Float, Double, Decimal, Numeric, 
        Bit, Char, VarChar, LongVarChar)
    }

    implicit val FloatType = new ScalaType[Float] {
      val tag = Predef.implicitly[TypeTag[Float]]
      val primaryTarget = Real
      val secondaryTargets = Nil
      val get = RS.getFloat(_: Int)
      val set = PS.setFloat(_: Int, _: Float)
      val update = RS.updateFloat(_: Int, _:Float)
      val primarySources = NonEmptyList(Real)
      val secondarySources = List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Decimal, 
        Numeric, Bit, Char, VarChar, LongVarChar)
    }

    implicit val DoubleType = new ScalaType[Double] {
      val tag = Predef.implicitly[TypeTag[Double]]
      val primaryTarget = Double
      val secondaryTargets = Nil
      val get = RS.getDouble(_: Int)
      val set = PS.setDouble(_: Int, _: Double)
      val update = RS.updateDouble(_: Int, _:Double)
      val primarySources = NonEmptyList(Float, Double)
      val secondarySources = List(TinyInt, Integer, SmallInt, BigInt, Float, Real, Decimal, Numeric, 
        Bit, Char, VarChar, LongVarChar)
    }

    implicit val BigDecimalType = new ScalaType[java.math.BigDecimal] {
      val tag = Predef.implicitly[TypeTag[java.math.BigDecimal]]
      val primaryTarget = Numeric
      val secondaryTargets = Nil
      val get = RS.getBigDecimal(_: Int)
      val set = PS.setBigDecimal(_: Int, _: java.math.BigDecimal)
      val update = RS.updateBigDecimal(_: Int, _:java.math.BigDecimal)
      val primarySources = NonEmptyList(Decimal, Numeric)
      val secondarySources = List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Bit, 
        Char, VarChar, LongVarChar)
    }

    // N.B. derived; not included in the primitive instance list
    implicit val ScalaBigDecimalType: ScalaType[BigDecimal] =
      BigDecimalType.xmap(BigDecimal(_), _.bigDecimal)

    implicit val BooleanType = new ScalaType[Boolean] {
      val tag = Predef.implicitly[TypeTag[Boolean]]
      val primaryTarget = Bit
      val secondaryTargets = Nil
      val get = RS.getBoolean(_: Int)
      val set = PS.setBoolean(_: Int, _: Boolean)
      val update = RS.updateBoolean(_: Int, _:Boolean)
      val primarySources = NonEmptyList(Bit)
      val secondarySources = List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Decimal, 
        Numeric, Char, VarChar, LongVarChar)
    }

    implicit val StringType = new ScalaType[String] {
      val tag = Predef.implicitly[TypeTag[String]]
      val primaryTarget = VarChar
      val secondaryTargets = List(Char, LongVarChar)
      val get = RS.getString(_: Int)
      val set = PS.setString(_: Int, _: String)
      val update = RS.updateString(_: Int, _:String)
      val primarySources = NonEmptyList(Char, VarChar)
      val secondarySources = List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Decimal, 
        Numeric, Bit, LongVarChar, Binary, VarBinary, LongVarBinary, Date, Time, Timestamp)
    }
  
    implicit val ByteArrayType = new ScalaType[Array[Byte]] {
      val tag = Predef.implicitly[TypeTag[Array[Byte]]]
      val primaryTarget = Binary
      val secondaryTargets = List(VarBinary, LongVarBinary)
      val get = RS.getBytes(_: Int)
      val set = PS.setBytes(_: Int, _: Array[Byte])
      val update = RS.updateBytes(_: Int, _: Array[Byte])
      val primarySources = NonEmptyList(Binary, VarBinary)
      val secondarySources = List(LongVarBinary)
    }

    implicit val DateType = new ScalaType[java.sql.Date] {
      val tag = Predef.implicitly[TypeTag[java.sql.Date]]
      val primaryTarget = Date
      val secondaryTargets = List()
      val get = RS.getDate(_: Int)
      val set = PS.setDate(_: Int, _: java.sql.Date)
      val update = RS.updateDate(_: Int, _: java.sql.Date)
      val primarySources = NonEmptyList(Date)
      val secondarySources = List(Char, VarChar, LongVarChar, Timestamp)
    }

    // N.B. derived; not included in the primitive instance list
    implicit val JavaUtilDateType: ScalaType[java.util.Date] =
      DateType.xmap(Predef.conforms, d => new java.sql.Date(d.getTime))

    implicit val TimeType = new ScalaType[java.sql.Time] {
      val tag = Predef.implicitly[TypeTag[java.sql.Time]]
      val primaryTarget = Time
      val secondaryTargets = List()
      val get = RS.getTime(_: Int)
      val set = PS.setTime(_: Int, _: java.sql.Time)
      val update = RS.updateTime(_: Int, _: java.sql.Time)
      val primarySources = NonEmptyList(Time)
      val secondarySources = List(Char, VarChar, LongVarChar, Timestamp)
    }

    implicit val TimestampType = new ScalaType[java.sql.Timestamp] {
      val tag = Predef.implicitly[TypeTag[java.sql.Timestamp]]
      val primaryTarget = Timestamp
      val secondaryTargets = List()
      val get = RS.getTimestamp(_: Int)
      val set = PS.setTimestamp(_: Int, _: java.sql.Timestamp)
      val update = RS.updateTimestamp(_: Int, _: java.sql.Timestamp)
      val primarySources = NonEmptyList(Timestamp)
      val secondarySources = List(Char, VarChar, LongVarChar, Date, Time)
    }

  }



}
