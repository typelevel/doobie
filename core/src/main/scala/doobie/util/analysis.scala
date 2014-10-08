package doobie.util

import doobie.enum.nullability._
import doobie.enum.parametermode._
import doobie.enum.jdbctype._
import doobie.enum.scalatype._
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

  case class ParameterMisalignment(index: Int, alignment: (ScalaType[_], NullabilityKnown) \/ ParameterMeta) extends AlignmentError {
    val tag = "P"
    def msg = this match {

      // todo: typeName(n: Nullability)

      case ParameterMisalignment(i, -\/((st, n))) => 
        s"""|Parameter $index (Scala ${st.typeName}, JDBC ${st.primaryTarget.toString.toUpperCase})
            |has no corresponding SQL parameter and will result in a runtime failure when set. Check 
            |the SQL statement; the interpolated value may appear inside a comment or quoted 
            |string.""".stripMargin.lines.mkString(" ")

      case ParameterMisalignment(i, \/-(pm)) => 
        s"""|Parameter $index (JDBC ${pm.jdbcType.toString.toUpperCase}, native ${pm.vendorTypeName.toUpperCase})
            |is not set; this will result in a runtime failure. Perhaps you used a literal ? rather
            |than an interpolated value.""".stripMargin.lines.mkString(" ")
    }
  }

  case class ParameterTypeError(index: Int, scalaType: ScalaType[_], jdbcType: JdbcType, vendorTypeName: String, nativeMap: Map[String, JdbcType]) extends AlignmentError {
    val tag = "P"
    def msg = 
      s"""|Parameter $index (Scala ${scalaType.typeName}, JDBC ${scalaType.primaryTarget.toString.toUpperCase}) 
          |may not be coercible to the schema type (JDBC ${jdbcType.toString.toUpperCase}, native 
          |${vendorTypeName.toUpperCase}). Possible remedies: (1) change schema type to JDBC 
          |${scalaType.primaryTarget.toString.toUpperCase}, native 
          |${nativeMap.filter(_._2 == scalaType.primaryTarget).keys.map(_.toUpperCase).mkString(" or ")}; 
          |or (2) change the parameter type to Scala ${ScalaType.forPrimaryTarget(jdbcType).get.typeName}, JDBC
          |${jdbcType.toString.toUpperCase}.""".stripMargin.lines.mkString(" ")
  }
  
  case class ColumnMisalignment(index: Int, alignment: (ScalaType[_], NullabilityKnown) \/ ColumnMeta) extends AlignmentError {
    val tag = "C"
    def msg = this match {

      case ColumnMisalignment(i, -\/((j, n))) => 
        s"""|Too few columns are selected, which will result in a runtime failure. Add a column or 
            |remove mapped ${j.typeName} from the result type.""".stripMargin.lines.mkString(" ")

      case ColumnMisalignment(i, \/-(col)) => 
        s"""Column ${col.name.toUpperCase} is unused. Remove it from the SELECT statement."""

    }
  }

  case class NullabilityMisalignment(index: Int, name: String, jdk: Nullability, jdbc: Nullability) extends AlignmentError {
    val tag = "C"
    def msg = this match {

      case NullabilityMisalignment(i, name, Nullable, NoNulls) => 
        s"""Non-nullable column ${name.toUpperCase} is unnecessarily mapped to an Option type."""
      
      case NullabilityMisalignment(i, name, NoNulls, Nullable) => 
        s"""|Nullable column ${name.toUpperCase} should be mapped to an Option type; reading a NULL 
            |value will result in a runtime failure.""".stripMargin.lines.mkString(" ")
      
      case NullabilityMisalignment(i, name, NullableUnknown, NoNulls)  => 
        s"""|Column ${name.toUpperCase} may be nullable, but the driver doesn't know. You may want 
            |to map to an Option type; reading a NULL value will result in a runtime 
            |failure.""".stripMargin.lines.mkString(" ")
      
      case NullabilityMisalignment(i, name, NullableUnknown, Nullable) => 
        s"""|Column ${name.toUpperCase} may be nullable, but the driver doesn't know. You may not 
            |need the Option wrapper.""".stripMargin.lines.mkString(" ")
    
    }
  }

  // case class ColumnTypeError(index: Int, name: String, jdk: (ScalaType[_], NullabilityKnown), schema: ColumnMeta) extends AlignmentError {
  //   val tag = "C"
  //   def msg =
  //     s"""|Column $name (JDBC ${schema.jdbcType.toString.toUpperCase}, native 
  //         |${schema.vendorTypeName.toUpperCase}) may not be coercible to Scala ${jdk.scalaType}.
  //         |Possible remedies: (1) change the schema type to JDBC ...
  //         |""".stripMargin.lines.mkString(" ")
  // }

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
        case (Both((j, _), p), n) if j.primaryTarget != p.jdbcType && 
                               !j.secondaryTargets.contains(p.jdbcType) =>
          ParameterTypeError(n + 1, j, p.jdbcType, p.vendorTypeName, nativeMap)
      }

    def columnMisalignments: List[ColumnMisalignment] =
      columnAlignment.zipWithIndex.collect {
        case (This(j), n) => ColumnMisalignment(n + 1, -\/(j))
        case (That(p), n) => ColumnMisalignment(n + 1, \/-(p))
      }

    def nullabilityMisalignments: List[NullabilityMisalignment] =
      columnAlignment.zipWithIndex.collect {
        case (Both((_, na), ColumnMeta(_, _, nb, col)), n) if na != nb => 
          NullabilityMisalignment(n + 1, col, na, nb)
      }

    def alignmentErrors = 
      (parameterMisalignments ++ parameterTypeErrors).sortBy(m => (m.index, m.msg)) ++ 
      (columnMisalignments ++ nullabilityMisalignments).sortBy(m => (m.index, m.msg))

    def print[M[_]](implicit M: Capture[M]): M[Unit] =
      M.apply {
        import Predef._
        import Console._
        import pretty._
        import scalaz._, Scalaz._

        def color(esc: String): String => String = 
          s => esc + s + RESET

        val gray  = color("\033[0;37m")
        val black = color(BLACK)
        val red   = color(RED)
        val none  = red("«none»")

        def formatNullability(n: Nullability): String =
          n match {
            case NoNulls         => "NOT NULL"
            case Nullable        => "NULL"
            case NullableUnknown => "«null?»"
          }

        val sqlBlock = Block(sql.lines.map(_.trim).filterNot(_.isEmpty).map(s => s"$BLUE$s$RESET").toList)

        val params: Block = 
          parameterAlignment.zipWithIndex.map { 

            case (Both((j1, n1), ParameterMeta(j2, s2, n2, m)), i) => 
              val jdbcColor = if (j1.primaryTarget == j2) color(BLACK) else color(RED)
              List(f"P${i+1}%02d", 
                   s"${j1.typeName}", 
                   jdbcColor(j1.primaryTarget.toString.toUpperCase),
                   " → ", 
                   jdbcColor(j2.toString.toUpperCase),
                   gray(s2.toUpperCase))

            case (This((j1, n1)), i) => 
              List(f"P${i+1}%02d", 
                   s"${j1.typeName}", 
                   black(j1.primaryTarget.toString.toUpperCase),
                   " → ", 
                   none,
                   "")

            case (That(ParameterMeta(j2, s2, n2, m)), i) => 
              List(f"P${i+1}%02d", 
                   "",
                   none, 
                   " → ", 
                   black(j2.toString.toUpperCase),
                   gray(s2.toUpperCase))

          } .transpose.map(Block(_)).reduceLeft(_ leftOf1 _)

        val cols: Block = 
          columnAlignment.zipWithIndex.map { 

            case (Both((j1, n1), cm @ ColumnMeta(j2, s2, n2, m)), i) =>
              val nullColor = if (n1 == n2) color(BLACK) else color(RED)
              List(f"C${i+1}%02d", 
                   m.toUpperCase, 
                   j2.toString.toUpperCase, 
                   gray(s2.toUpperCase), 
                   nullColor(formatNullability(n2)),
                   " → ",
                   nullColor(j1.typeName))
            
            case (This((j1, n1)), i) =>
              List(f"C${i+1}%02d", none, "", "", "",  " → ", red(j1.typeName))
            
            case (That(ColumnMeta(j2, s2, n2, m)), i) =>
              List(f"C${i+1}%02d", m.toUpperCase, j2.toString.toUpperCase, gray(s2.toUpperCase), black(formatNullability(n2)), " → ", none)
          
          } .transpose.map(Block(_)).reduceLeft(_ leftOf1 _)

        val x = Block(Nil) leftOf2 (sqlBlock above1 Block(List("PARAMETERS")) above params above1 Block(List("COLUMNS")) above cols above1
            alignmentErrors.map { a => 
              Block(List(f"${a.tag}${a.index}%02d")) leftOf1 Block(wrap(90)(a.msg))
            }.foldLeft(Block(List("WARNINGS")))(_ above _)) 

        Console.println()
        Console.println(x)
        Console.println()

      }

  }

}










