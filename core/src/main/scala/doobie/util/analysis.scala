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
  final case class ColumnMeta(jdbcType: JdbcType, nullability: Nullability, name: String) extends Meta
  
  /** Metadata for the JDBC end of a column/parameter mapping. */
  final case class ParameterMeta(jdbcType: JdbcType, nullability: Nullability, mode: ParameterMode) extends Meta 

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
        case (Both(JdkMeta(_, na, _), ColumnMeta(_, nb, col)), n) => NullabilityMisalignment(n + 1, col, na, nb)
      }

    def print[M[_]](implicit M: Capture[M]): M[Unit] =
      M.apply {
        import Predef._
        import Console._
        import pretty._
        import scalaz._, Scalaz._

        val sqlBlock = Block(sql.lines.map(_.trim).filterNot(_.isEmpty).map(s => s"$BLUE$s$RESET").toList)

        val params: Block = 
          parameterAlignment.zipWithIndex.map { 

            case (Both(j @ JdkMeta(j1, n1, t), ParameterMeta(j2, n2, m)), i) => 
              List(f"$i%-2d", s"${j.scalaType}", s"${j1.toString.toUpperCase}", "→", s"${j2.toString.toUpperCase}")

          } .transpose.map(Block(_)).foldLeft(Block(Nil))(_ leftOf2 _)

        val cols: Block = 
          columnAlignment.zipWithIndex.map { 

            case (Both(j @ JdkMeta(j1, n1, t), ColumnMeta(j2, n2, m)), i) =>
              if (n1 == n2) List(f"$i%-2d", s"$m", s"${j2.toString.toUpperCase}", s"$BLACK$n2$RESET", "→", s"${j.scalaType}")
              else          List(f"$i%-2d", s"$m", s"${j2.toString.toUpperCase}", s"$RED$n2$RESET",   "→", s"$RED${j.scalaType}$RESET")
          
            case (This(j @ JdkMeta(j1, n1, t)), i) =>
              List(f"$i%-2d", s"$RED«missing»$RESET", "", "",   "→", s"$RED${j.scalaType}$RESET")

            case (That(ColumnMeta(j2, n2, m)), i) =>
              List(f"$i%-2d", s"$m", s"${j2.toString.toUpperCase}", s"$BLACK$n2$RESET", "→", s"$RED«missing»$RESET")

          } .transpose.map(Block(_)).foldLeft(Block(Nil))(_ leftOf2 _)

        val x = sqlBlock above Block(List("", "PARAMETERS")) above params above Block(List("", "COLUMNS")) above cols

        Console.println((Block(List("")) above (Block(List("  ")) leftOf x)) above Block(List("")))
      }

  }

  case class ParameterMisalignment(index: Int, alignment: JdkMeta \/ ParameterMeta)
  case class ColumnMisalignment(index: Int, alignment: JdkMeta \/ ColumnMeta)
  case class NullabilityMisalignment(index: Int, name: String, jdk: Nullability, jdbc: Nullability)

}
