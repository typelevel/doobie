package doobie.hi
package syntax

import java.sql
import scalaz._
import Scalaz._
import scalaz.effect.{IO, MonadIO}
import scalaz.syntax.effect.monadCatchIO._
import scalaz.stream._
import Process._
import Predef.???


class ProcInterpolator(val sc: StringContext) extends AnyVal {


  def proc[A: Prim, B: Prim](a: A, b: OutParam[B]): ProcBuilder[B] = ???

}

// OutParam ~ Option
sealed trait OutParam[A]

object OutParam {

  case class Out[A]() extends OutParam[A]
  case class InOut[A](a: A) extends OutParam[A]
  case class OutStream[A]() extends OutParam[Process[resultset.Action, A]]

  def out[A]: OutParam[A] = Out[A]()
  def inout[A](a: A): OutParam[A] = InOut(a)

}

trait ProcBuilder[A]

