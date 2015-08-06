package doobie.util

import doobie.imports.{ ConnectionIO, FC, toDoobieCatchableOps }
import doobie.imports.HC.delay

import scalaz.syntax.monad._
import scalaz.stream.Process
import scalaz.stream.Process.eval_

object liftxa {

  /**
   * A data type that can lift an ordinary `ConnectionIO` or `Process` thereof into one with 
   * transactional handling. The `default` instance provided on the companion object is suitable for
   * most applications.
   * @param before an action to prepare the `Connection`
   * @param oops an action to run if the main action fails with an exception
   * @param after an action to run on success
   * @param always a clean up action to run in all cases
   */
  final case class LiftXA(
    before:  ConnectionIO[Unit],
    oops:    ConnectionIO[Unit],
    after:   ConnectionIO[Unit],
    always:  ConnectionIO[Unit]
  ) {

    /** Wrap a `ConnectionIO` in before/after/oops/always logic. */
    def safe[A](ma: ConnectionIO[A]): ConnectionIO[A] =
      (before *> ma <* after) onException oops ensuring always

    /** Wrap a `Process[ConnectionIO, ?]` in before/after/oops/always logic. */
    def safeP[A](pa: Process[ConnectionIO, A]): Process[ConnectionIO, A] =
      (eval_(before) ++ pa ++ eval_(after)) onFailure { e => 
        eval_(oops) ++ eval_(delay(throw e)) 
      } onComplete eval_(always)

  }
  object LiftXA {

    /** 
     * A default instance of `LiftXA` with the following common semantics: `autoCommit` is set to 
     * false; the transaction is rolled back on an exception and committed on success; and it is 
     * closed in all cases.
     */
    val default = LiftXA(
      FC.setAutoCommit(false),
      FC.rollback,
      FC.commit,
      FC.close
    )

  }

}