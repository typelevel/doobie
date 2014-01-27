package doobie
package hi
package co

import dbc._
import scalaz._
import Scalaz._
import scalaz.effect.{IO, MonadIO}
import scalaz.effect.kleisliEffect._
import scalaz.syntax.effect.monadCatchIO._
import scalaz.stream._

trait ResultSetCombinators {

  /** A process that consumes values by repeated calls to `getNext`. */
  def process[A: Comp]: Process[ResultSet, A] = 
    Process.repeatEval(getNext[A]).takeWhile(_.isDefined).map(_.get)

  /** Construct a `Sink` from an IO action, useful for Process.to */
  def mkSink[M[+_]: MonadIO, A](f: A => IO[Unit]): Sink[M, A] =
    Process.repeatEval(((a: A) => f(a).liftIO[M]).point[M])

  /** Get a structured value at the specified index. */
  def get[A](index: Int)(implicit A: Comp[A]): ResultSet[A] =
    resultset.push(s"structured get at index $index")(A.get(index))

  /** Get a structured value at the beginning of the current row. */
  def get1[A](implicit A: Comp[A]): ResultSet[A] =
    resultset.push(s"get1")(A.get(1))

  /** Consume and return all remaining rows as a Vector. */
  def vector[A: Comp]: ResultSet[Vector[A]] = 
    resultset.push("vector")(process[A].runLog.map(_.toVector)) // N.B. it's already a Vector

  /** Consume and return all remaining rows as a List. */
  def list[A: Comp]: ResultSet[List[A]] = 
    resultset.push("list")(vector[A].map(_.toList))

  /** Consume all remaining by passing to effectful action `effect`. */
  def sink[A: Comp](effect: A => IO[Unit]): ResultSet[Unit] = 
    resultset.push(s"sink($effect)")(process[A].to(mkSink[ResultSet, A](effect)).run)

  /** Consume and return the next row as a value of type `A`s, if any. */
  def getNext[A: Comp]: ResultSet[Option[A]] =
    resultset.push("getNext") {
      resultset.next >>= {
        case true  => get1[A].map(Some(_))
        case false => None.point[ResultSet]
      }
    }
  
}