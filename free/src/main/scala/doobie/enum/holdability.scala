package doobie.enum

import doobie.util.invariant._

import java.sql.ResultSet._

import scalaz.Equal
import scalaz.std.anyVal.intInstance

/** 
 * Module for the enumerated type representing resultset holdability. 
 */
object holdability {

  /** 
   * Enumerated type representing resultset holdability. 
   * @param toInt The underlying JDBC constant.
   */
  sealed abstract class Holdability(val toInt: Int)

  /**
   * The constant indicating that open `ResultSet` objects with this holdability will remain open 
   * when the current transaction is commited. 
   * @group Constructors 
   */
  case object HoldCursorsOverCommit extends Holdability(HOLD_CURSORS_OVER_COMMIT)

  /** 
   * The constant indicating that open `ResultSet` objects with this holdability will be closed when 
   * the current transaction is commited.
   * @group Constructors 
   */
  case object CloseCursorsAtCommit  extends Holdability(CLOSE_CURSORS_AT_COMMIT)

  object Holdability {

    /** 
     * Returns the `Holdability` associated with the specified JDBC constant, if any.
     * @param n JCBC holdability constant, defined on `ResultSet`.
     * @group Lifting 
     */
    def fromInt(n:Int): Option[Holdability] =
      Some(n) collect {
        case HoldCursorsOverCommit.toInt => HoldCursorsOverCommit
        case CloseCursorsAtCommit.toInt  => CloseCursorsAtCommit
      }

    /** 
     * Returns the `Holdability` associated with the specified JDBC constant, else throws an
     * `InvalidOrdinal` exception.
     * @param n JCBC holdability constant, defined on `ResultSet`.
     * @throws InvalidOrdinal if no `Holdability` is associated with the given value.
     * @group Lifting 
     */
    def unsafeFromInt(n:Int): Holdability =
      fromInt(n).getOrElse(throw InvalidOrdinal[Holdability](n))

    /** @group Typeclass Instances */
    implicit val EqualHoldability: Equal[Holdability] =
      Equal.equalBy(_.toInt)

  }

}