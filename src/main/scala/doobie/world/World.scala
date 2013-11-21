package doobie
package world

import scala.annotation.tailrec
import scalaz._
import scalaz.Scalaz._
import scalaz.Free._

trait World {

  /** Our world's state. Subclasses may choose to make this type public. */
  protected type State

  /** Our operations are simple state transitions. */
  case class Op[+A] private[World] (f: State => (State, A))

  /** Operation must have a `Functor` in order to gain a `Free` monad. */
  implicit val OpFunctor: Functor[Op] =  
    new Functor[Op] {
      def map[A, B](op: Op[A])(g: A => B) = 
        Op(op.f(_).rightMap(g))
    }

  /** Alias for our monad. */
  type Action[A] = Free[Op, A]

  /** Construct an `Action`. */
  protected def action[A](f: State => (State, A)): Action[A] = 
    Suspend(Op(f(_).rightMap(Return(_))))

  /** Construct an `Action` that does not change the state. */
  protected final def effect[A](f: State => A): Action[A] = 
    action(s => (s, f(s)))

  /** Modify the state. */
  protected final def modify(f: State => State): Action[Unit] =
    action(s => (f(s), ()))

  /** Pure constructor. */
  object Action {
    final def apply[A](a: => A): Action[A] =
      action(s => (s, a))
  }

  /** Run th */
  @tailrec protected final def runWorld[A](s: State, a: Action[A]): (State, A) =
    a.resume match { // N.B. resume.fold() doesn't permit TCO
      case -\/(Op(f)) => 
        val (s0, a0) = f(s)
        runWorld(s0, a0)
      case \/-(a) => (s, a)
    }

}

