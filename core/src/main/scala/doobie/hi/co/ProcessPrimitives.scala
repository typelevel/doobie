package doobie
package hi
package co

import dbc._
import scalaz._
import Scalaz._
import scalaz.effect.{IO, MonadIO}
import scalaz.syntax.effect.monadCatchIO._
import scalaz.stream._
import scalaz.stream.Process.{await, emit, eval, halt, End}

trait ProcessPrimitives[S] { this: op.PrimitiveOps[S] =>

  /** Construct a Sink in this monad, lifting the provided IO effect. */
  def mkSink[A](f: A => IO[Unit]): Sink[Action, A] =
    Process.repeatEval(((a: A) => f(a).liftIO[Action]).point[Action])

  /** 
   * Construct a Process in this monad using a resource of type `R`, producing values of type `A`
   * until `step` produces `None`, calling `release` for any termination, normal or not.
   */
  def resource[R,O](acquire: Action[R])(release: R => Action[Unit])(step: R => Action[Option[O]]): Process[Action,O] = {
    def go(step: Action[O], onExit: Process[Action,O]): Process[Action,O] =
      await[Action,O,O](step)(o => emit(o) ++ go(step, onExit), onExit, onExit)                          
    await(acquire)(r => go(step(r).map(_.getOrElse(throw End)), eval(release(r)).drain), halt, halt)
  }

}