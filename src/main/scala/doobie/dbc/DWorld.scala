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

  protected def effect[A: Show](e: => String, f: S => A): Action[A] =
    log.flatMap(_.log(LogElement(e), effect(f)))

  protected def effectA[A](e: => String, g: A => String, f: S => A): Action[A] =
    log.flatMap(_.logA(LogElement(e), g, effect(f)))

  def push[A: Show](e: => String, a: Action[A]): Action[A] =
    log.flatMap(_.log(LogElement(e), a))

}