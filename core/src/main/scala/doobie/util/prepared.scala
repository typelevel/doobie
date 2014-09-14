package doobie.util

import doobie.free.connection.ConnectionIO
import doobie.free.preparedstatement.{ PreparedStatementIO, getParameterMetaData, getMetaData }
import doobie.util.composite.Composite
import doobie.util.capture.Capture
import doobie.util.indexed.Indexed
import doobie.syntax.indexed._
import doobie.std.resultsetmetadata._
import doobie.std.parametermetadata._

import doobie.enum.nullability._
import doobie.enum.jdbctype._

import scalaz._, Scalaz._

object prepared {

  trait Prepared {
    def sql: String
    def check: ConnectionIO[Analysis]
  }

  type Mapping = Indexed.Meta \&/ Indexed.Meta

  def parameterMappings[A](implicit A: Composite[A]): PreparedStatementIO[List[Mapping]] =
    getParameterMetaData.map(_.meta align A.meta)

  def columnMappings[A](implicit A: Composite[A]): PreparedStatementIO[List[Mapping]] =
    getMetaData.map(_.meta align A.meta)

  def analysis[A: Composite, B: Composite](sql: String): PreparedStatementIO[Analysis] =
    (parameterMappings[A] |@| columnMappings[B])(Analysis(sql, _, _))

  case class Analysis(sql: String, parameters: List[Mapping], columns: List[Mapping]) {

    import Predef.augmentString

    def dump: String = {

      import pretty.Block

      val sqlB = Block(sql.trim.lines.map(_.trim).toList)

      def mBlock(label: String, ms: List[Mapping]): Block = {
        val ja = ms.map(_.a.map(_.jdbcType.toString).orZero)
        val jb = ms.map(_.b.map(_.jdbcType.toString).orZero)
        val na = ms.map(_.a.map(_.nullability).map(formatNullability).orZero)
        val nb = ms.map(_.b.map(_.nullability).map(formatNullability).orZero)
        val left  = Block(ja) leftOf1 Block(na)
        val right = Block(jb) leftOf1 Block(nb)
        val height = left.height max right.height
        val nums = Block((1 |-> height).map(_.toString))
        Block(List(label)) above
        nums.leftOf2(left).leftOfP(right, " => ") above1 Block(Nil)
      }

      val all = 
        (Block(Nil) above1 sqlB) above1 
        (mBlock("INPUTS", parameters.map(_.swap)).leftOfP(mBlock("OUTPUTS",columns), "     "))

      Block(Nil).leftOfP(all, "\t").toString

    }

    def formatNullability(n: Nullability): String = 
      n match {
        case NoNulls         => "Not Null"
        case Nullable        => "Null"
        case NullableUnknown => "Unknown"
      }

  }

}

