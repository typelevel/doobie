package doobie.util

import scalaz.{ Applicative, Functor }
import scalaz.syntax.applicative._

import scalaz.stream.Process
import scalaz.stream.Process.{ await, halt, emit, eval, End, Sink }

/** Additional functions for manipulating `Process` values. */
object process {

  /** Generalized `sink` constructor. */
  def sink[F[_]: Applicative, A](f: A => F[Unit]): Sink[F, A] = 
    Process.repeatEval(f.point[F])

  /** Generalized `resource` combinator. */
  def resource[F[_]: Functor,R,O](acquire: F[R])(release: R => F[Unit])(step: R => F[Option[O]]): Process[F,O] = {
    def go(step: F[O], onExit: Process[F,O]): Process[F,O] =
      await[F,O,O](step)(o => emit(o) ++ go(step, onExit), onExit, onExit)                          
    await(acquire)(r => go(step(r).map(_.getOrElse(throw End)), eval(release(r)).drain), halt, halt)
  }

}