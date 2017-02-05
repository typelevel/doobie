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

  /** Unexpected string value for an enumerated type. */
  final case class InvalidEnum[A](value: String)(implicit ev: Manifest[A]) 
    extends InvariantViolation(s"${ev.runtimeClass.getName}: invalid ordinal: $value")

  final case class SecondaryValidationFailed[A](value: String)(implicit ev: Manifest[A])
    extends InvariantViolation(s"${ev.runtimeClass.getName}: validation failed: $value")

  /** The type of schema violations. */
  sealed abstract class MappingViolation(msg: String) extends InvariantViolation(msg) {
    def index: Int
    def jdbcType: JdbcType
  }

  private def oneBasedDisclaimer = "Note that JDBC column indexing is 1-based."

  final case class NonNullableParameter(index: Int, jdbcType: JdbcType) 
    extends MappingViolation(s"Scala `null` value passed as parameter $index (JDBC type $jdbcType); use an Option type here. $oneBasedDisclaimer")
  final case class NonNullableColumnUpdate(index: Int, jdbcType: JdbcType) 
    extends MappingViolation(s"Scala `null` value passed as update to column $index (JDBC type $jdbcType); use an Option type here. $oneBasedDisclaimer")
  final case class NonNullableColumnRead(index: Int, jdbcType: JdbcType)
    extends MappingViolation(s"SQL `NULL` read at column $index (JDBC type $jdbcType) but mapping is to a non-Option type; use Option here. $oneBasedDisclaimer")

  /** Array violations. Not terribly illuminating at this point. */
  sealed abstract class ArrayStructureViolation(msg: String) extends InvariantViolation(msg)
  final case object NullableCellRead 
    extends ArrayStructureViolation("SQL `NULL` appears in an array cell that was asserted to be non-null.")
  final case object NullableCellUpdate
    extends ArrayStructureViolation("Scala `null` value appears in an array cell that was asserted to be non-null.")

  /** Invalid JAVA_OBJECT mapping. */
  final case class InvalidObjectMapping[A, B](expected: Class[A], actual: Class[B])
    extends InvariantViolation(s"SQL object of class ${actual.getName} cannot be cast to mapped class ${expected.getName}.") 

}
