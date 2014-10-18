package doobie.util

import doobie.enum.nullability._
import doobie.enum.parametermode._
import doobie.enum.jdbctype._
import doobie.util.scalatype._
import doobie.util.capture._

import scala.Predef._ // TODO: minimize
import scala.reflect.runtime.universe.TypeTag

import scalaz.{ \&/, \/, -\/, \/- }
import scalaz.\&/._

object analysis {

  /** Metadata for the JDBC end of a column/parameter mapping. */
  final case class ColumnMeta(jdbcType: JdbcType, vendorTypeName: String, nullability: Nullability, name: String)
  
  /** Metadata for the JDBC end of a column/parameter mapping. */
  final case class ParameterMeta(jdbcType: JdbcType, vendorTypeName: String, nullability: Nullability, mode: ParameterMode) 

  sealed trait AlignmentError {
    def tag: String
    def index: Int
    def msg: String
  }

  def typeName(t: ScalaType[_], n: NullabilityKnown): String = {
    val name = t.tag.tpe.toString match {
      case "java.lang.String"  => "String"
      case "scala.Array[Byte]" => "Array[Byte]"
      case s                   => s
    } 
    n match {
      case NoNulls  => name
      case Nullable => s"Option[${name}]"
    }
  }

  def formatNullability(n: Nullability): String =
    n match {
      case NoNulls         => "NOT NULL"
      case Nullable        => "NULL"
      case NullableUnknown => "«null?»"
    }

  case class ParameterMisalignment(index: Int, alignment: (ScalaType[_], NullabilityKnown) \/ ParameterMeta) extends AlignmentError {
    val tag = "P"
    def msg = this match {
      case ParameterMisalignment(i, -\/((st, n))) => 
        s"""|Interpolated value has no corresponding SQL parameter and likely appears inside a
            |comment or quoted string. This will result in a runtime failure; fix this by removing
            |the parameter.""".stripMargin.lines.mkString(" ")
      case ParameterMisalignment(i, \/-(pm)) => 
        s"""|${pm.jdbcType.toString.toUpperCase} parameter is not set; this will result in a runtime 
            |failure. Perhaps you used a literal ? rather than an interpolated value.""".stripMargin.lines.mkString(" ")
    }
  }

  case class ParameterTypeError(index: Int, scalaType: ScalaType[_], n: NullabilityKnown, jdbcType: JdbcType, vendorTypeName: String, nativeMap: Map[String, JdbcType]) extends AlignmentError {
    val tag = "P"
    def msg = 
      s"""|${typeName(scalaType, n)} is likely not coercible to ${jdbcType.toString.toUpperCase}.
          |Fix this by changing the schema type to ${scalaType.primaryTarget.toString.toUpperCase}, 
          |or the Scala type to ${typeName(ScalaType.forPrimaryTarget(jdbcType).get, n)}.""".stripMargin.lines.mkString(" ")
  }
  
  case class ColumnMisalignment(index: Int, alignment: (ScalaType[_], NullabilityKnown) \/ ColumnMeta) extends AlignmentError {
    val tag = "C"
    def msg = this match {
      case ColumnMisalignment(i, -\/((j, n))) => 
        s"""|Too few columns are selected, which will result in a runtime failure. Add a column or 
            |remove mapped ${typeName(j, n)} from the result type.""".stripMargin.lines.mkString(" ")
      case ColumnMisalignment(i, \/-(col)) => 
        s"""Column is unused. Remove it from the SELECT statement."""
    }
  }

  case class NullabilityMisalignment(index: Int, name: String, st: ScalaType[_], jdk: Nullability, jdbc: Nullability) extends AlignmentError {
    val tag = "C"
    def msg = this match {
      case NullabilityMisalignment(i, name, st, Nullable, NoNulls) => 
        s"""Non-nullable column ${name.toUpperCase} is unnecessarily mapped to an Option type."""
      case NullabilityMisalignment(i, name, st, NoNulls, Nullable) => 
        s"""|Reading a NULL value into ${typeName(st, NoNulls)} will result in a runtime failure. 
            |Fix this by making the schema type ${formatNullability(NoNulls)} or by changing the 
            |Scala type to ${typeName(st, Nullable)}""".stripMargin.lines.mkString(" ")
      case NullabilityMisalignment(i, name, st, NullableUnknown, NoNulls)  => 
        s"""|Column ${name.toUpperCase} may be nullable, but the driver doesn't know. You may want 
            |to map to an Option type; reading a NULL value will result in a runtime 
            |failure.""".stripMargin.lines.mkString(" ")
      case NullabilityMisalignment(i, name, st, NullableUnknown, Nullable) => 
        s"""|Column ${name.toUpperCase} may be nullable, but the driver doesn't know. You may not 
            |need the Option wrapper.""".stripMargin.lines.mkString(" ")
    }
  }

  case class ColumnTypeError(index: Int, jdk: ScalaType[_], n: NullabilityKnown, schema: ColumnMeta) extends AlignmentError {
    val tag = "C"
    def msg =
      s"""|${schema.jdbcType.toString.toUpperCase} is likely not coercible to ${typeName(jdk, n)}. 
          |Fix this by changing the schema type to 
          |${jdk.primarySources.list.map(_.toString.toUpperCase).mkString(" or ") }; or the
          |Scala type to ${ScalaType.forPrimarySource(schema.jdbcType).map(typeName(_, n)).get}.
          |""".stripMargin.lines.mkString(" ")
  }

  case class ColumnTypeWarning(index: Int, jdk: ScalaType[_], n: NullabilityKnown, schema: ColumnMeta) extends AlignmentError {
    val tag = "C"
    def msg =
      s"""|${schema.jdbcType.toString.toUpperCase} is ostensibly coercible to ${typeName(jdk, n)}
          |but is not a recommended target type. Fix this by changing the schema type to 
          |${jdk.primarySources.list.map(_.toString.toUpperCase).mkString(" or ") }; or the
          |Scala type to ${ScalaType.forPrimarySource(schema.jdbcType).map(typeName(_, n)).get}.
          |""".stripMargin.lines.mkString(" ")
  }

  /** Compatibility analysis for the given statement and aligned mappings. */
  final case class Analysis(
    sql: String,
    nativeMap: Map[String, JdbcType],
    parameterAlignment: List[(ScalaType[_], NullabilityKnown) \&/ ParameterMeta],
    columnAlignment:    List[(ScalaType[_], NullabilityKnown) \&/ ColumnMeta]) {

    def parameterMisalignments: List[ParameterMisalignment] =
      parameterAlignment.zipWithIndex.collect {
        case (This(j), n) => ParameterMisalignment(n + 1, -\/(j))
        case (That(p), n) => ParameterMisalignment(n + 1, \/-(p))
      }

    def parameterTypeErrors: List[ParameterTypeError] =
      parameterAlignment.zipWithIndex.collect {
        case (Both((j, n1), p), n) if !(j.primaryTarget :: j.secondaryTargets).contains(p.jdbcType) =>
          ParameterTypeError(n + 1, j, n1, p.jdbcType, p.vendorTypeName, nativeMap)
      }

    def columnMisalignments: List[ColumnMisalignment] =
      columnAlignment.zipWithIndex.collect {
        case (This(j), n) => ColumnMisalignment(n + 1, -\/(j))
        case (That(p), n) => ColumnMisalignment(n + 1, \/-(p))
      }

    def columnTypeErrors: List[ColumnTypeError] =
      columnAlignment.zipWithIndex.collect {
        case (Both((j, n1), p), n) if !(j.primarySources.list ++ j.secondarySources).contains(p.jdbcType) =>
          ColumnTypeError(n + 1, j, n1, p)
      }

    def columnTypeWarnings: List[ColumnTypeWarning] =
      columnAlignment.zipWithIndex.collect {
        case (Both((j, n1), p), n) if j.secondarySources.contains(p.jdbcType) =>
          ColumnTypeWarning(n + 1, j, n1, p)
      }

    def nullabilityMisalignments: List[NullabilityMisalignment] =
      columnAlignment.zipWithIndex.collect {
        case (Both((st, na), ColumnMeta(_, _, nb, col)), n) if na != nb => 
          NullabilityMisalignment(n + 1, col, st, na, nb)
      }

    lazy val parameterAlignmentErrors = 
      parameterMisalignments ++ parameterTypeErrors

    lazy val columnAlignmentErrors = 
      columnMisalignments ++ columnTypeErrors ++ columnTypeWarnings ++ nullabilityMisalignments

    def alignmentErrors = 
      (parameterAlignmentErrors).sortBy(m => (m.index, m.msg)) ++ 
      (columnAlignmentErrors).sortBy(m => (m.index, m.msg))

    lazy val paramDescriptions: List[(String, List[AlignmentError])] = {
      import pretty._
      import scalaz._, Scalaz._
 
      val none = "«none»"

      val params: Block = 
        parameterAlignment.zipWithIndex.map { 
          case (Both((j1, n1), ParameterMeta(j2, s2, n2, m)), i) => List(f"P${i+1}%02d", s"${typeName(j1, n1)}", " → ", j2.toString.toUpperCase)
          case (This((j1, n1)), i)                               => List(f"P${i+1}%02d", s"${typeName(j1, n1)}", " → ", none)
          case (That(ParameterMeta(j2, s2, n2, m)), i)           => List(f"P${i+1}%02d", none,                   " → ", j2.toString.toUpperCase)
        } .transpose.map(Block(_)).foldLeft(Block(Nil))(_ leftOf1 _) // TODO: fix this

      params.toString.lines.toList.zipWithIndex.map { case (s, n) =>
        (s, parameterAlignmentErrors.filter(_.index == n + 1))
      }

    }

    lazy val columnDescriptions: List[(String, List[AlignmentError])] = {
      import pretty._
      import scalaz._, Scalaz._
 
      val none = "«none»"

      val cols: Block = 
        columnAlignment.zipWithIndex.map { 
          case (Both((j1, n1), ColumnMeta(j2, s2, n2, m)), i) => List(f"C${i+1}%02d", m.toUpperCase, j2.toString.toUpperCase, formatNullability(n2), " → ", typeName(j1, n1))            
          case (This((j1, n1)), i)                            => List(f"C${i+1}%02d", none,          "",                      "",                    " → ", typeName(j1, n1))    
          case (That(ColumnMeta(j2, s2, n2, m)), i)           => List(f"C${i+1}%02d", m.toUpperCase, j2.toString.toUpperCase, formatNullability(n2), " → ", none)        
        } .transpose.map(Block(_)).foldLeft(Block(Nil))(_ leftOf1 _)

      cols.toString.lines.toList.zipWithIndex.map { case (s, n) =>
        (s, columnAlignmentErrors.filter(_.index == n + 1))
      }

    }


    def summary: String = {
      import pretty._
      import scalaz._, Scalaz._
 
      val none = "«none»"
      val sqlBlock = Block(sql.lines.map(_.trim).filterNot(_.isEmpty).toList)

      val params: Block = 
        parameterAlignment.zipWithIndex.map { 
          case (Both((j1, n1), ParameterMeta(j2, s2, n2, m)), i) => List(f"P${i+1}%02d", s"${typeName(j1, n1)}", " → ", j2.toString.toUpperCase)
          case (This((j1, n1)), i)                               => List(f"P${i+1}%02d", s"${typeName(j1, n1)}", " → ", none)
          case (That(ParameterMeta(j2, s2, n2, m)), i)           => List(f"P${i+1}%02d", none,                   " → ", j2.toString.toUpperCase)
        } .transpose.map(Block(_)).reduceLeft(_ leftOf1 _)

      val cols: Block = 
        columnAlignment.zipWithIndex.map { 
          case (Both((j1, n1), ColumnMeta(j2, s2, n2, m)), i) => List(f"C${i+1}%02d", m.toUpperCase, j2.toString.toUpperCase, formatNullability(n2), " → ", typeName(j1, n1))            
          case (This((j1, n1)), i)                            => List(f"C${i+1}%02d", none,          "",                      "",                    " → ", typeName(j1, n1))    
          case (That(ColumnMeta(j2, s2, n2, m)), i)           => List(f"C${i+1}%02d", m.toUpperCase, j2.toString.toUpperCase, formatNullability(n2), " → ", none)        
        } .transpose.map(Block(_)).reduceLeft(_ leftOf1 _)

      val x = Block(Nil) leftOf2 (sqlBlock above1 Block(List("PARAMETERS")) above params above1 Block(List("COLUMNS")) above cols above1
          alignmentErrors.map { a => 
            Block(List(f"${a.tag}${a.index}%02d")) leftOf1 Block(wrap(90)(a.msg))
          }.foldLeft(Block(List("WARNINGS")))(_ above _)) 

      s"\n$x\n"
    }

  }

}










