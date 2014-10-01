package doobie.util

import doobie.enum.nullability._
import doobie.enum.parametermode._
import doobie.enum.jdbctype._
import doobie.util.capture._
import doobie.util.atom.JdbcMapping

import scala.reflect.runtime.universe.TypeTag

import scalaz.{ \&/, \/, -\/, \/- }
import scalaz.\&/._

object analysis {

  /** Metadata for the JDBC end of a column/parameter mapping. */
  final case class ColumnMeta(jdbcType: JdbcType, vendorTypeName: String, nullability: Nullability, name: String)
  
  /** Metadata for the JDBC end of a column/parameter mapping. */
  final case class ParameterMeta(jdbcType: JdbcType, vendorTypeName: String, nullability: Nullability, mode: ParameterMode) 

  /** Metadata for the JDK end of a column/parameter mapping. */
  final case class JdkMeta(jdbcMapping: JdbcMapping, nullability: NullabilityKnown, jdkType: TypeTag[_]) {
    import Predef._
    def simpleType: String = jdkType.tpe.toString.split("\\.").last
    def scalaType: String =
      nullability match {
        case Nullable => s"Option[$simpleType]"
        case NoNulls  => simpleType
      }
  }

  /** Compatibility analysis for the given statement and aligned mappings. */
  final case class Analysis(
    sql: String,
    parameterAlignment: List[JdkMeta \&/ ParameterMeta],
    columnAlignment:    List[JdkMeta \&/ ColumnMeta]) {

    def parameterMisalignments: List[ParameterMisalignment] =
      parameterAlignment.zipWithIndex.collect {
        case (This(j), n) => ParameterMisalignment(n + 1, -\/(j))
        case (That(p), n) => ParameterMisalignment(n + 1, \/-(p))
      }

    def parameterTypeErrors: List[ParameterTypeError] =
      parameterAlignment.zipWithIndex.collect {
        case (Both(j, p), n) if j.jdbcMapping.primaryTarget != p.jdbcType && 
                               !j.jdbcMapping.secondaryTargets.contains(p.jdbcType) =>
          ParameterTypeError(n + 1, j.scalaType, j.jdbcMapping, p.jdbcType, p.vendorTypeName)
      }

    def columnMisalignments: List[ColumnMisalignment] =
      columnAlignment.zipWithIndex.collect {
        case (This(j), n) => ColumnMisalignment(n + 1, -\/(j))
        case (That(p), n) => ColumnMisalignment(n + 1, \/-(p))
      }

    def nullabilityMisalignments: List[NullabilityMisalignment] =
      columnAlignment.zipWithIndex.collect {
        case (Both(JdkMeta(_, na, _), ColumnMeta(_, _, nb, col)), n) if na != nb => 
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

            case (Both(j @ JdkMeta(j1, n1, t), ParameterMeta(j2, s2, n2, m)), i) => 
              val jdbcColor = if (j1.primaryTarget == j2) color(BLACK) else color(RED)
              List(f"P${i+1}%02d", 
                   s"${j.scalaType}", 
                   jdbcColor(j1.primaryTarget.toString.toUpperCase),
                   " → ", 
                   jdbcColor(j2.toString.toUpperCase),
                   gray(s2.toUpperCase))

            case (This(j @ JdkMeta(j1, n1, t)), i) => 
              List(f"P${i+1}%02d", 
                   s"${j.scalaType}", 
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

            case (Both(j @ JdkMeta(j1, n1, t), cm @ ColumnMeta(j2, s2, n2, m)), i) =>
              val nullColor = if (n1 == n2) color(BLACK) else color(RED)
              List(f"C${i+1}%02d", 
                   m.toUpperCase, 
                   j2.toString.toUpperCase, 
                   gray(s2.toUpperCase), 
                   nullColor(formatNullability(n2)),
                   " → ",
                   nullColor(j.scalaType.toString))
            
            case (This(j @ JdkMeta(j1, n1, t)), i) =>
              List(f"C${i+1}%02d", none, "", "", "",  " → ", red(j.scalaType.toString))
            
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

  sealed trait AlignmentError {
    def tag: String
    def index: Int
    def msg: String
  }

  case class ParameterMisalignment(index: Int, alignment: JdkMeta \/ ParameterMeta) extends AlignmentError {
    val tag = "P"
    def msg = this match {
      case ParameterMisalignment(i, -\/(jdk)) => s"${jdk.scalaType} parameter $index has no corresponding SQL parameter and will result in a runtime failure when set. Check the SQL statement; the interpolated value may appear inside a comment or quoted string."
      case ParameterMisalignment(i, \/-(pm))  => s"${pm.jdbcType.toString.toUpperCase} parameter $index (native type ${pm.vendorTypeName.toUpperCase}) is not set; this will result in a runtime failure. Perhaps you used a literal ? rather than an interpolated value."
    }
  }

  case class ParameterTypeError(index: Int, scalaType: String, jdbcMapping: JdbcMapping, jdbcType: JdbcType, vendorTypeName: String) extends AlignmentError {
    val tag = "P"
    def msg = 
      // TODO: suggest alternatives: which Scala types would match? which JDBC types would match?
      s"$scalaType parameter $index (JDBC type ${jdbcMapping.primaryTarget.toString.toUpperCase}) may not be coercible to ${jdbcType.toString.toUpperCase} (native type ${vendorTypeName.toUpperCase})."
  }

  case class ColumnMisalignment(index: Int, alignment: JdkMeta \/ ColumnMeta) extends AlignmentError {
    val tag = "C"
    def msg = this match {
      case ColumnMisalignment(i, -\/(jdk)) => s"Too few columns are selected, which will result in a runtime failure. Add a column or remove mapped ${jdk.scalaType} from the result type."
      case ColumnMisalignment(i, \/-(col)) => s"Column ${col.name.toUpperCase} is unused. Remove it from the SELECT statement."
    }
  }

  case class NullabilityMisalignment(index: Int, name: String, jdk: Nullability, jdbc: Nullability) extends AlignmentError {
    val tag = "C"
    def msg = this match {
      case NullabilityMisalignment(i, name, Nullable, NoNulls) => s"Non-nullable column ${name.toUpperCase} is unnecessarily mapped to an Option type."
      case NullabilityMisalignment(i, name, NoNulls, Nullable) => s"Nullable column ${name.toUpperCase} should be mapped to an Option type; reading a NULL value will result in a runtime failure."
      case NullabilityMisalignment(i, name, NullableUnknown, NoNulls) => s"Column ${name.toUpperCase} may be nullable, but the driver doesn't know. You may want to map to an Option type; reading a NULL value will result in a runtime failure."
      case NullabilityMisalignment(i, name, NullableUnknown, Nullable) => s"Column ${name.toUpperCase} may be nullable, but the driver doesn't know. You may not need the Option wrapper."
    }
  }

}










