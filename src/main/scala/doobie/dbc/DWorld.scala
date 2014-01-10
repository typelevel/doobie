package doobie
package dbc

import scalaz.Show
import scalaz.effect.IO

trait DWorld[S] extends util.TWorld[(Log[LogElement], S)] {

  // This is what we're really after
  protected val x = new Lifted[IO]
  protected type Action[+A] = x.Action[A]

  protected final def eval[A](a: Action[A], l: Log[LogElement], s:S): IO[((Log[LogElement], S), A)] =
    x.eval(a, (l, s))

  protected def log: Action[Log[LogElement]] =
    x.effect(_._1)

  protected def effect[A](f: S => A): Action[A] =
    x.effect(s => f(s._2))

  protected def effect[A: Show](e: => LogElement, f: S => A): Action[A] =
    log.flatMap(_.log(e, effect(f)))

  def push[A: Show](e: => LogElement, a: Action[A]): Action[A] =
    log.flatMap(_.log(e, a))

}