package doobie.util

import scala.reflect.Manifest

import doobie.enum.jdbctype.JdbcType

/** 
 * Module defining the type of exceptions representing unmet expectations. These typically indicate a problem with
 * the schema, with type mapping, with driver compliance, and so on. The intent is that they be as
 * fine-grained as reasonable for diagnostic purposes, but it is not expected that the application
 * should be able to handle them in any meaningful way.
 */
object invariant {

  sealed abstract class InvariantViolation(msg: String) extends Exception(msg)

  sealed abstract class UnexpectedCursorPosition(msg: String) extends InvariantViolation(msg)
  case object UnexpectedEnd 
    extends UnexpectedCursorPosition("ResultSet exhausted; more rows expected.")
  case object UnexpectedContinuation 
    extends UnexpectedCursorPosition("Expected ResultSet exhaustion, but more rows were available.")

  /** Unexpected ordinal value for an enumerated type. */
  final case class InvalidOrdinal[A](value: Int)(implicit ev: Manifest[A]) 
    extends InvariantViolation(s"${ev.runtimeClass.getName}: invalid ordinal: $value")

  /** The type of schema violations. */
  sealed abstract class MappingViolation(msg: String) extends InvariantViolation(msg) {
    def index: Int
    def jdbcType: JdbcType
  }
  final case class NonNullableParameter(index: Int, jdbcType: JdbcType) 
    extends MappingViolation(s"Scala `null` value passed as parameter $index (JDBC type $jdbcType); use an Option type here.")
  final case class NonNullableColumnUpdate(index: Int, jdbcType: JdbcType) 
    extends MappingViolation(s"Scala `null` value passed as update to column $index (JDBC type $jdbcType); use an Option type here.")
  final case class NonNullableColumnRead(index: Int, jdbcType: JdbcType)
    extends MappingViolation(s"SQL `NULL` read at column $index (JDBC type $jdbcType) but mapping is to a non-Option type; use Option here.")

}