package doobie
package dbc

import scalaz.Kleisli
import scalaz.Kleisli.ask
import scalaz.effect.IO
import scalaz.effect.kleisliEffect._

trait DWorld[S] {

  type Action[+A] = Kleisli[IO, (Log[LogElement], S), A]

  protected def log: Action[Log[LogElement]] =
    ask[IO, (Log[LogElement], S)].map(_._1)

  protected def effect[A](f: S => A): Action[A] =
    ask[IO, (Log[LogElement], S)].map(p => f(p._2))

  def push[A](e: => String, a: Action[A]): Action[A] =
    log.flatMap(_.log(LogElement(e), a))

  protected def primitive[A](e: => String, f: S => A): Action[A] =
    push("jdbc:" + e, effect(f))

}