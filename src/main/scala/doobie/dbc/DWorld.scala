package doobie
package dbc

import scalaz.Show
import scalaz.effect.IO

trait DWorld[S] extends util.TWorld[(Log[LogElement], S)] {

  // This is what we're really after
  protected val tworldfixedio = new Lifted[IO]
  protected type Action[+A] = tworldfixedio.Action[A]

  protected final def eval[A](a: Action[A], l: Log[LogElement], s:S): IO[((Log[LogElement], S), A)] =
    tworldfixedio.eval(a, (l, s))

  protected def log: Action[Log[LogElement]] =
    tworldfixedio.effect(_._1)

  protected def effect[A](f: S => A): Action[A] =
    tworldfixedio.effect(s => f(s._2))

  protected def primitive[A](e: => String, f: S => A): Action[A] =
    log.flatMap(_.log(LogElement("jdbc:" + e), tworldfixedio.effect(s => f(s._2))))

  def push[A](e: => String, a: Action[A]): Action[A] =
    log.flatMap(_.log(LogElement(e), a))

}


// Can we simplify?

trait Context[S] {
  import scalaz._
  import scalaz.effect._
  import Scalaz._

  import Kleisli._
  import kleisliEffect._

  type Action[+A] = Kleisli[IO, (Log[LogElement], S), A]


  Monad[Action]
  MonadIO[Action]

}