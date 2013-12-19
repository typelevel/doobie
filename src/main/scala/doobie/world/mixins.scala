package doobie
package world

import scalaz._
import Scalaz._

import doobie.util.RWSFWorld

trait EventLogging { this: RWSFWorld =>
  import rwsfops._

  type Event
  type Log = Vector[Event]

  protected type W = Log
  protected lazy val W: Monoid[W] = implicitly

  implicit class WriterOps[A](a: Action[A]) {

    /** Log after running `a`, using its result. */
    def :++>>(f: A => Event): Action[A] =
      a.flatMap(x => tell(Vector(f(x))).map(_ => x))

    /** Log after running `a`. */
    def :++>(l: => Event): Action[A] =
      :++>>(_ => l)

    /** Log before running `a`. */
    def :<++(l: => Event): Action[A] =
      tell(Vector(l)).flatMap(_ => a)
  
  }
}

trait RunReaderWriter { this: RWSFWorld =>
  protected def runrw[A](r: R, a: Action[A]): (W, Throwable \/ A)  
}

trait UnitState extends RunReaderWriter { this: RWSFWorld =>

  protected type S = Unit

  // Abbreviated execution for unit state :-\
  protected def runrw[A](r: R, a: Action[A]): (W, Throwable \/ A) = {
    val (w, s, e) = runrws(r, (), a)
    (w, e)
  }

}

trait IndexedState extends RunReaderWriter { this: RWSFWorld =>
  import rwsfops._

  protected type S = Int

  protected[world] def runrw[A](r: R, a: Action[A]): (W, Throwable \/ A) =
    runrws(r, 1, a) match { case (w, _ , e) => (w, e) }

  /** Increment the current index state by 1. */
  def advance: Action[Unit] =
    mod(_ + 1)

  /** Reset the index state to 1. */
  def reset: Action[Unit] =
    put(1)

}
