package doobie.util

import doobie.enum.nullability._
import doobie.enum.parametermode._
import doobie.enum.jdbctype._
import doobie.util.capture._

import scala.reflect.runtime.universe.TypeTag

import scalaz.{ \&/, \/, -\/, \/- }
import scalaz.\&/._

object analysis {

  /** Metadata for either endpoint of a column/parameter mapping. */
  sealed trait Meta {
    def jdbcType: JdbcType
    def nullability: Nullability
  }
  
  /** Metadata for the JDBC end of a column/parameter mapping. */
  final case class ColumnMeta(jdbcType: JdbcType, vendorTypeName: String, nullability: Nullability, name: String) extends Meta
  
  /** Metadata for the JDBC end of a column/parameter mapping. */
  final case class ParameterMeta(jdbcType: JdbcType, vendorTypeName: String, nullability: Nullability, mode: ParameterMode) extends Meta 

  /** Metadata for the JDK end of a column/parameter mapping. */
  final case class JdkMeta(jdbcType: JdbcType, nullability: NullabilityKnown, jdkType: TypeTag[_]) extends Meta {
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

    def columnMisalignments: List[ColumnMisalignment] =
      columnAlignment.zipWithIndex.collect {
        case (This(j), n) => ColumnMisalignment(n + 1, -\/(j))
        case (That(p), n) => ColumnMisalignment(n + 1, \/-(p))
      }

    def nullabilityMisalignments: List[NullabilityMisalignment] =
      columnAlignment.zipWithIndex.collect {
        case (Both(JdkMeta(_, na, _), ColumnMeta(_, _, nb, col)), n) => NullabilityMisalignment(n + 1, col, na, nb)
      }

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
              val jdbcColor = if (j1 == j2) color(BLACK) else color(RED)
              List(f"P${i+1}%02d", 
                   s"${j.scalaType}", 
                   jdbcColor(j1.toString.toUpperCase),
                   " → ", 
                   jdbcColor(j2.toString.toUpperCase),
                   gray(s2.toUpperCase))

            case (This(j @ JdkMeta(j1, n1, t)), i) => 
              List(f"P${i+1}%02d", 
                   s"${j.scalaType}", 
                   black(j1.toString.toUpperCase),
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

        val x = Block(Nil) leftOf2 (sqlBlock above1 params above1 cols)

        Console.println()
        Console.println(x)
        Console.println()
      }

  }

  case class ParameterMisalignment(index: Int, alignment: JdkMeta \/ ParameterMeta)
  case class ColumnMisalignment(index: Int, alignment: JdkMeta \/ ColumnMeta)
  case class NullabilityMisalignment(index: Int, name: String, jdk: Nullability, jdbc: Nullability)

}










