package doobie

import scala.reflect.Manifest

/** 
 * The type of exceptions representing unmet expectations. These typically indicate a problem with
 * the schema, with type mapping, with driver compliance, and so on. The intent is that they be as
 * fine-grained as reasonable for diagnostic purposes, but it is not expected that the application
 * should be able to handle them in any meaningful way.
 */
sealed abstract class InvariantViolation(msg: String) extends Exception(msg)

/** Cursor position (at-end or not-at-end) was not expected. */
sealed abstract class UnexpectedCursorPosition(msg: String) extends InvariantViolation(msg)
case object UnexpectedEnd 
  extends UnexpectedCursorPosition("ResultSet exhausted; more rows expected.")
case object UnexpectedContinuation 
  extends UnexpectedCursorPosition("Expected ResultSet exhaustion, but more rows were available.")

/** Unexpected ordinal value for an enumerated type. */
case class InvalidOrdinal[A](value: Int)(implicit ev: Manifest[A]) 
  extends InvariantViolation(s"${ev.runtimeClass.getName}: invalid ordinal: $value")

