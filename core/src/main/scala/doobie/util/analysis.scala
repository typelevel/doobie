package doobie.util

import doobie.enum.nullability._
import doobie.enum.parametermode._
import doobie.enum.jdbctype._
import doobie.util.meta._
import doobie.util.capture._
import doobie.util.pretty._
import doobie.util.meta._

import scala.Predef._ // TODO: minimize
import scala.reflect.runtime.universe.TypeTag

import scalaz._, Scalaz._
import scalaz.\&/._

/** Module defining a type for analyzing the type alignment of prepared statements. */
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

  case class ParameterMisalignment(index: Int, alignment: (Meta[_], NullabilityKnown) \/ ParameterMeta) extends AlignmentError {
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

  case class ParameterTypeError(index: Int, scalaType: Meta[_], n: NullabilityKnown, jdbcType: JdbcType, vendorTypeName: String, nativeMap: Map[String, JdbcType]) extends AlignmentError {
    val tag = "P"
    def msg = 
      s"""|${typeName(scalaType, n)} is not coercible to ${jdbcType.toString.toUpperCase}
          |(${vendorTypeName})
          |according to the JDBC specification.
          |Fix this by changing the schema type to ${scalaType.jdbcTarget.head.toString.toUpperCase}, 
          |or the Scala type to ${Meta.writersOf(jdbcType, vendorTypeName).toList.map(typeName(_, n)).mkString(" or ")}.""".stripMargin.lines.mkString(" ")
  }
  
  case class ColumnMisalignment(index: Int, alignment: (Meta[_], NullabilityKnown) \/ ColumnMeta) extends AlignmentError {
    val tag = "C"
    def msg = this match {
      case ColumnMisalignment(i, -\/((j, n))) => 
        s"""|Too few columns are selected, which will result in a runtime failure. Add a column or 
            |remove mapped ${typeName(j, n)} from the result type.""".stripMargin.lines.mkString(" ")
      case ColumnMisalignment(i, \/-(col)) => 
        s"""Column is unused. Remove it from the SELECT statement."""
    }
  }

  case class NullabilityMisalignment(index: Int, name: String, st: Meta[_], jdk: NullabilityKnown, jdbc: NullabilityKnown) extends AlignmentError {
    val tag = "C"
    def msg = this match {
      // https://github.com/tpolecat/doobie/issues/164 ... NoNulls means "maybe no nulls"  :-\
      // case NullabilityMisalignment(i, name, st, NoNulls, Nullable) =>
      //   s"""Non-nullable column ${name.toUpperCase} is unnecessarily mapped to an Option type."""
      case NullabilityMisalignment(i, name, st, Nullable, NoNulls) => 
        s"""|Reading a NULL value into ${typeName(st, NoNulls)} will result in a runtime failure. 
            |Fix this by making the schema type ${formatNullability(NoNulls)} or by changing the 
            |Scala type to ${typeName(st, Nullable)}""".stripMargin.lines.mkString(" ")
    }
  }

  case class ColumnTypeError(index: Int, jdk: Meta[_], n: NullabilityKnown, schema: ColumnMeta) extends AlignmentError {
    val tag = "C"
    def msg =
      Meta.readersOf(schema.jdbcType, schema.vendorTypeName).toList.map(typeName(_, n)) match {
        case Nil =>
          s"""|${schema.jdbcType.toString.toUpperCase} (${schema.vendorTypeName}) is not 
              |coercible to ${typeName(jdk, n)} according to the JDBC specification or any defined
              |mapping. 
              |Fix this by changing the schema type to 
              |${jdk.jdbcSource.list.map(_.toString.toUpperCase).toList.mkString(" or ") }; or the
              |Scala type to an appropriate ${if (schema.jdbcType == Array) "array" else "object"} 
              |type.
              |""".stripMargin.lines.mkString(" ")
        case ss => 
          s"""|${schema.jdbcType.toString.toUpperCase} (${schema.vendorTypeName}) is not 
              |coercible to ${typeName(jdk, n)} according to the JDBC specification or any defined
              |mapping. 
              |Fix this by changing the schema type to 
              |${jdk.jdbcSource.list.map(_.toString.toUpperCase).toList.mkString(" or ") }, or the
              |Scala type to ${ss.mkString(" or ")}.
              |""".stripMargin.lines.mkString(" ")
      }
  }

  case class ColumnTypeWarning(index: Int, jdk: Meta[_], n: NullabilityKnown, schema: ColumnMeta) extends AlignmentError {
    val tag = "C"
    def msg =
      s"""|${schema.jdbcType.toString.toUpperCase} (${schema.vendorTypeName}) is ostensibly 
          |coercible to ${typeName(jdk, n)}
          |according to the JDBC specification but is not a recommended target type. Fix this by 
          |changing the schema type to 
          |${jdk.jdbcSource.list.map(_.toString.toUpperCase).toList.mkString(" or ") }; or the
          |Scala type to ${Meta.readersOf(schema.jdbcType, schema.vendorTypeName).toList.map(typeName(_, n)).mkString(" or ")}.
          |""".stripMargin.lines.mkString(" ")
  }

  /** Compatibility analysis for the given statement and aligned mappings. */
  final case class Analysis(
    sql:                String,
    nativeMap:          Map[String, JdbcType],
    parameterAlignment: List[(Meta[_], NullabilityKnown) \&/ ParameterMeta],
    columnAlignment:    List[(Meta[_], NullabilityKnown) \&/ ColumnMeta]) {

    def parameterMisalignments: List[ParameterMisalignment] =
      parameterAlignment.zipWithIndex.collect {
        case (This(j), n) => ParameterMisalignment(n + 1, -\/(j))
        case (That(p), n) => ParameterMisalignment(n + 1, \/-(p))
      }

    def parameterTypeErrors: List[ParameterTypeError] =
      parameterAlignment.zipWithIndex.collect {
        case (Both((j, n1), p), n) if !j.jdbcTarget.element(p.jdbcType) =>
          ParameterTypeError(n + 1, j, n1, p.jdbcType, p.vendorTypeName, nativeMap)
      }

    def columnMisalignments: List[ColumnMisalignment] =
      columnAlignment.zipWithIndex.collect {
        case (This(j), n) => ColumnMisalignment(n + 1, -\/(j))
        case (That(p), n) => ColumnMisalignment(n + 1, \/-(p))
      }

    def columnTypeErrors: List[ColumnTypeError] =
      columnAlignment.zipWithIndex.collect {
        case (Both((j, n1), p), n) if !(j.jdbcSource.list.toList ++ j.fold(_.jdbcSourceSecondary.toList, _ => Nil)).element(p.jdbcType) =>
          ColumnTypeError(n + 1, j, n1, p)
        case (Both((j, n1), p), n) if (p.jdbcType === JavaObject || p.jdbcType == Other) && !j.fold(_ => None, a => Some(a.schemaTypes.head)).element(p.vendorTypeName) =>
          ColumnTypeError(n + 1, j, n1, p)
      }

    def columnTypeWarnings: List[ColumnTypeWarning] =
      columnAlignment.zipWithIndex.collect {
        case (Both((j, n1), p), n) if j.fold(_.jdbcSourceSecondary.toList, _ => Nil).element(p.jdbcType) =>
          ColumnTypeWarning(n + 1, j, n1, p)
      }

    def nullabilityMisalignments: List[NullabilityMisalignment] =
      columnAlignment.zipWithIndex.collect {
        // We can't do anything helpful with NoNulls .. it means "might not be nullable"
        // case (Both((st, Nullable), ColumnMeta(_, _, NoNulls, col)), n) => NullabilityMisalignment(n + 1, col, st, NoNulls, Nullable)
        case (Both((st, NoNulls), ColumnMeta(_, _, Nullable, col)), n) => NullabilityMisalignment(n + 1, col, st, Nullable, NoNulls)
        // N.B. if we had a warning mechanism we could issue a warning for NullableUnknown
      }

    lazy val parameterAlignmentErrors = 
      parameterMisalignments ++ parameterTypeErrors

    lazy val columnAlignmentErrors = 
      columnMisalignments ++ columnTypeErrors ++ columnTypeWarnings ++ nullabilityMisalignments

    def alignmentErrors = 
      (parameterAlignmentErrors).sortBy(m => (m.index, m.msg)) ++ 
      (columnAlignmentErrors).sortBy(m => (m.index, m.msg))

    /** Description of each parameter, paired with its errors. */
    lazy val paramDescriptions: List[(String, List[AlignmentError])] = {
      val params: Block = 
        parameterAlignment.zipWithIndex.map { 
          case (Both((j1, n1), ParameterMeta(j2, s2, n2, m)), i) => List(f"P${i+1}%02d", s"${typeName(j1, n1)}", " → ", j2.toString.toUpperCase, s"($s2)")
          case (This((j1, n1)),                               i) => List(f"P${i+1}%02d", s"${typeName(j1, n1)}", " → ", "", "")
          case (That(          ParameterMeta(j2, s2, n2, m)), i) => List(f"P${i+1}%02d", "",                     " → ", j2.toString.toUpperCase, s"($s2)")
        } .transpose.map(Block(_)).foldLeft(Block(Nil))(_ leftOf1 _).trimLeft(1)
      params.toString.lines.toList.zipWithIndex.map { case (s, n) =>
        (s, parameterAlignmentErrors.filter(_.index == n + 1))
      }
    }

    /** Description of each parameter, paird with its errors. */
    lazy val columnDescriptions: List[(String, List[AlignmentError])] = {
      import pretty._
      import scalaz._, Scalaz._
      val cols: Block = 
        columnAlignment.zipWithIndex.map { 
          case (Both((j1, n1), ColumnMeta(j2, s2, n2, m)), i) => List(f"C${i+1}%02d", m, j2.toString.toUpperCase, s"(${s2.toString})", formatNullability(n2), " → ", typeName(j1, n1))            
          case (This((j1, n1)),                            i) => List(f"C${i+1}%02d", "",          "", "",                       "",                    " → ", typeName(j1, n1))    
          case (That(          ColumnMeta(j2, s2, n2, m)), i) => List(f"C${i+1}%02d", m, j2.toString.toUpperCase, s"(${s2.toString})", formatNullability(n2), " → ", "")        
        } .transpose.map(Block(_)).foldLeft(Block(Nil))(_ leftOf1 _).trimLeft(1)
      cols.toString.lines.toList.zipWithIndex.map { case (s, n) =>
        (s, columnAlignmentErrors.filter(_.index == n + 1))
      }
    }

  }

  // Some stringy helpers

  private val packagePrefix = "\\b[a-z]+\\.".r

  private def typeName(t: Meta[_], n: NullabilityKnown): String = {
    val name = packagePrefix.replaceAllIn(t.scalaType, "")
    n match {
      case NoNulls  => name
      case Nullable => s"Option[${name}]"
    }
  }

  private def formatNullability(n: Nullability): String =
    n match {
      case NoNulls         => "NOT NULL"
      case Nullable        => "NULL"
      case NullableUnknown => "NULL?"
    }


}










