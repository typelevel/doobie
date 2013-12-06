package doobie
package world

import scalaz._
import Scalaz._

import doobie.util.RWSFWorld

// All doobie worlds have Log as their writer
trait DWorld extends RWSFWorld {
  protected type W = Log
  protected lazy val W: Monoid[Log] = implicitly
}

object DWorld {

  // World with Unit (i.e., meaningless) state
  trait Stateless extends DWorld {
    protected type S = Unit

    // Abbreviated execution for unit state :-\
    protected def runrw[A](r: R, a: Action[A]): (W, Throwable \/ A) = {
      val (w, s, e) = runrws(r, (), a)
      (w, e)
    }

  }

  // World with a 1-based index as state
  trait Indexed extends DWorld {
    import rwsfops._

    protected type S = Int

    protected[world] def runi[A](r: R, a: Action[A]): (W, Throwable \/ A) =
      runrws(r, 1, a) match { case (w, _ , e) => (w, e) }

    /** Increment the current index state by 1. */
    def advance: Action[Unit] =
      mod(_ + 1)

    /** Reset the index state to 1. */
    def reset: Action[Unit] =
      put(1)

  }

}

