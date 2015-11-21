package doobie.util

import scalaz.{ Applicative, Functor }
import scalaz.syntax.applicative._

import scalaz.stream.{ Process, Sink, Cause }
import scalaz.stream.Process.{ bracket, repeatEval, eval_ }

/** Additional functions for manipulating `Process` values. */
object process {

  /** Generalized `sink` constructor. */
  def sink[F[_]: Applicative, A](f: A => F[Unit]): Sink[F, A] = 
    Process.repeatEval(f.point[F])

  /** Generalized `resource` combinator. */
  def resource[F[_]: Functor,R,O](acquire: F[R])(release: R => F[Unit])(step: R => F[Option[O]]): Process[F,O] = 
    bracket(acquire)(r => eval_(release(r))) {
      r => repeatEval(step(r).map(_.getOrElse(throw Cause.Terminated(Cause.End))))
    } onHalt { _.asHalt }

}