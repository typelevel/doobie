package doobie.enum

import doobie.util.invariant._
import doobie.util.atom._
import doobie.util.scalatype.ScalaType

import java.sql.ResultSet._
import java.sql.Types._

import scalaz.Equal
import scalaz.std.anyVal.intInstance

object jdbctype {

  /** @group Implementation */
  sealed abstract class JdbcType(val toInt: Int) extends Product with Serializable

  /** @group Values */ case object Array         extends JdbcType(ARRAY)
  /** @group Values */ case object BigInt        extends JdbcType(BIGINT)
  /** @group Values */ case object Binary        extends JdbcType(BINARY)
  /** @group Values */ case object Bit           extends JdbcType(BIT)
  /** @group Values */ case object Blob          extends JdbcType(BLOB)
  /** @group Values */ case object Boolean       extends JdbcType(BOOLEAN)
  /** @group Values */ case object Char          extends JdbcType(CHAR)
  /** @group Values */ case object Clob          extends JdbcType(CLOB)
  /** @group Values */ case object DataLink      extends JdbcType(DATALINK)
  /** @group Values */ case object Date          extends JdbcType(DATE)
  /** @group Values */ case object Decimal       extends JdbcType(DECIMAL)
  /** @group Values */ case object Distinct      extends JdbcType(DISTINCT)
  /** @group Values */ case object Double        extends JdbcType(DOUBLE)
  /** @group Values */ case object Float         extends JdbcType(FLOAT)
  /** @group Values */ case object Integer       extends JdbcType(INTEGER)
  /** @group Values */ case object JavaObject    extends JdbcType(JAVA_OBJECT)
  /** @group Values */ case object LongnVarChar  extends JdbcType(LONGNVARCHAR)
  /** @group Values */ case object LongVarBinary extends JdbcType(LONGVARBINARY)
  /** @group Values */ case object LongVarChar   extends JdbcType(LONGVARCHAR)
  /** @group Values */ case object NChar         extends JdbcType(NCHAR)
  /** @group Values */ case object NClob         extends JdbcType(NCLOB)
  /** @group Values */ case object Null          extends JdbcType(NULL)
  /** @group Values */ case object Numeric       extends JdbcType(NUMERIC)
  /** @group Values */ case object NVarChar      extends JdbcType(NVARCHAR)
  /** @group Values */ case object Other         extends JdbcType(OTHER)
  /** @group Values */ case object Real          extends JdbcType(REAL)
  /** @group Values */ case object Ref           extends JdbcType(REF)
  /** @group Values */ case object RowId         extends JdbcType(ROWID)
  /** @group Values */ case object SmallInt      extends JdbcType(SMALLINT)
  /** @group Values */ case object SqlXml        extends JdbcType(SQLXML)
  /** @group Values */ case object Struct        extends JdbcType(STRUCT)
  /** @group Values */ case object Time          extends JdbcType(TIME)
  /** @group Values */ case object Timestamp     extends JdbcType(TIMESTAMP)
  /** @group Values */ case object TinyInt       extends JdbcType(TINYINT)
  /** @group Values */ case object VarBinary     extends JdbcType(VARBINARY)
  /** @group Values */ case object VarChar       extends JdbcType(VARCHAR)

  /** @group Implementation */
  object JdbcType {

    def fromInt(n:Int): Option[JdbcType] =
      Some(n) collect {
        case Array.toInt         => Array         
        case BigInt.toInt        => BigInt        
        case Binary.toInt        => Binary        
        case Bit.toInt           => Bit           
        case Blob.toInt          => Blob          
        case Boolean.toInt       => Boolean       
        case Char.toInt          => Char          
        case Clob.toInt          => Clob          
        case DataLink.toInt      => DataLink      
        case Date.toInt          => Date          
        case Decimal.toInt       => Decimal       
        case Distinct.toInt      => Distinct      
        case Double.toInt        => Double        
        case Float.toInt         => Float         
        case Integer.toInt       => Integer       
        case JavaObject.toInt    => JavaObject    
        case LongnVarChar.toInt  => LongnVarChar  
        case LongVarBinary.toInt => LongVarBinary 
        case LongVarChar.toInt   => LongVarChar   
        case NChar.toInt         => NChar         
        case NClob.toInt         => NClob         
        case Null.toInt          => Null          
        case Numeric.toInt       => Numeric       
        case NVarChar.toInt      => NVarChar      
        case Other.toInt         => Other         
        case Real.toInt          => Real          
        case Ref.toInt           => Ref           
        case RowId.toInt         => RowId         
        case SmallInt.toInt      => SmallInt      
        case SqlXml.toInt        => SqlXml        
        case Struct.toInt        => Struct        
        case Time.toInt          => Time          
        case Timestamp.toInt     => Timestamp     
        case TinyInt.toInt       => TinyInt       
        case VarBinary.toInt     => VarBinary     
        case VarChar.toInt       => VarChar       
      }

    def unsafeFromInt(n:Int): JdbcType =
      fromInt(n).getOrElse(throw InvalidOrdinal[JdbcType](n))

    implicit val EqualJdbcType: Equal[JdbcType] =
      Equal.equalBy(_.toInt)

    implicit val ScalaTypeJdbcType: ScalaType[JdbcType] =
      ScalaType[Int].xmap(unsafeFromInt, _.toInt)

  }

}