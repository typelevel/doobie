package doobie.util

import doobie.enum.nullability._
import doobie.enum.parametermode._
import doobie.enum.jdbctype._

import scala.reflect.runtime.universe.TypeTag

import scalaz.\&/

/** 
 * Typeclass for statements that can be checked for validity with respect to asserted parameter 
 * and column types. 
 */
object analysis {

  /** Metadata for either endpoint of a column/parameter mapping. */
  sealed trait Meta {
    def jdbcType: JdbcType
    def nullability: Nullability
  }
  
  /** Metadata for the JDBC end of a column/parameter mapping. */
  final case class ColumnMeta(jdbcType: JdbcType, nullability: Nullability) extends Meta
  
  /** Metadata for the JDBC end of a column/parameter mapping. */
  final case class ParameterMeta(jdbcType: JdbcType, nullability: Nullability, mode: ParameterMode) extends Meta
  
  /** JMetadata for the DK end of a column/parameter mapping. */
  final case class JdkMeta(jdbcType: JdbcType, nullability: Nullability, jdkType: TypeTag[_]) extends Meta

  /** Compatibility analysis for the given statement and aligned mappings. */
  final case class Analysis(
    sql: String,
    parameterAlignment: List[JdkMeta \&/ ParameterMeta],
    columnAlignment: List[JdkMeta \&/ ColumnMeta])

}

