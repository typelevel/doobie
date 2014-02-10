package doobie
package hi
package co

import dbc._
import scalaz._
import Scalaz._
import scalaz.effect.{IO, MonadIO}
import scalaz.syntax.effect.monadCatchIO._
import scalaz.stream._

trait ResultSetCombinators extends op.ResultSetOps with ProcessPrimitives[java.sql.ResultSet] {

  ////// INITIAL COMBINATORS

  /** Get a structured value at the specified index. */
  def get[A](index: Int)(implicit A: Comp[A]): Action[A] =
    resultset.push(s"structured get at index $index")(A.get(index))

  /** Get a structured value at the beginning of the current row (index 1). */
  def get1[A: Comp]: Action[A] =
    get(1)

  /** Consume and return the next row as a value of type `A`, if any. */
  def getNext[A: Comp]: Action[Option[A]] =
    resultset.push("getNext") {
      resultset.next >>= {
        case true  => get1[A].map(Some(_))
        case false => None.point[Action]
      }
    }

  /** 
   * Consume and the one and only remaining row, failing with [[UnexpectedCursorPosition]] if there 
   * is not exactly one row remaining.
   */
  def getUnique[A: Comp]: Action[A] =
    (getNext[A] |@| next) {
      case (Some(a), false) => a
      case (Some(a), true)  => throw UnexpectedContinuation
      case (None, _)        => throw UnexpectedEnd
    }

  ////// PRIMITIVE STREAMING

  /** 
   * A process that consumes values by repeated calls to `getNext`. The various `run` methods on
   * `Process` will return a `ResultSet[_]` value. For example, `process[Widget].take(3).runLog`
   * will produce an Action[IndexedSeq[Widget]] with at most 3 elements.
   */
  def process[A: Comp]: Process[Action, A] = 
    Process.repeatEval(getNext[A]).takeWhile(_.isDefined).map(_.get)

  ////// CONVENIENCE STREAMS

  /** Consume and return all remaining rows as a Vector. */
  def vector[A: Comp]: Action[Vector[A]] = 
    resultset.push("vector")(process[A].runLog.map(_.toVector)) // N.B. it's already a Vector

  /** Consume and return all remaining rows as a List. */
  def list[A: Comp]: Action[List[A]] = 
    resultset.push("list")(vector[A].map(_.toList))

  /** Consume all remaining by passing to effectful action `effect`. */
  def sink[A: Comp](effect: A => IO[Unit]): Action[Unit] = 
    resultset.push(s"sink($effect)")(process[A].to(mkSink(effect)).run)

}

