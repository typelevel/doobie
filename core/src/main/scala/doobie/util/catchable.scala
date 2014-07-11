package doobie.util

import scalaz.{ Monad, Catchable, \/, -\/, \/- }
import scalaz.syntax.monad._

/** Module of additional combinators for `Catchable`, similar to those provided for `MonadCatchIO`. */
object catchable {

  def except[M[_], A](ma: M[A])(handler: Throwable => M[A])(implicit M: Monad[M], C: Catchable[M]): M[A] =
    M.bind(C.attempt(ma)) {
      case -\/(t) => handler(t)
      case \/-(a) => M.point(a)
    }

  /**
   * Executes the handler for exceptions that are raised and match the given predicate.
   * Other exceptions are rethrown.
   */
  def catchSome[M[_]: Monad: Catchable, A, B](ma: M[A])(p: Throwable => Option[B], handler: B => M[A]): M[A] =
    except(ma)(e => p(e) match {
      case Some(z) => handler(z)
      case None    => throw e
    })

  /**
   * Returns a disjunction result which is right if no exception was raised, or left if an
   * exception was raised.
   */
  def catchLeft[M[_]: Catchable, A](ma: M[A])(implicit M: Monad[M]): M[Throwable \/ A] =
    except(M.map(ma)(\/.right[Throwable, A]))(t => M.point(\/.left[Throwable, A](t)))

  /** Like "catchLeft" but takes a predicate to select which exceptions are caught. */
  def catchSomeLeft[M[_]: Catchable, A, B](ma: M[A])(p: Throwable => Option[B])(implicit M: Monad[M]): M[B \/ A] =
    M.map(catchLeft(ma))(_.leftMap(e => p(e).getOrElse(throw e)))

  /** Like "finally", but only performs the final action if there was an exception. */
  def onException[M[_]: Monad: Catchable, A, B](ma: M[A], action: M[B]): M[A] =
    except(ma)(e =>
      for {
        _ <- action
        a <- (throw e): M[A]
      } yield a)

  def bracket[M[_]: Monad: Catchable, A, B, C](before: M[A])(after: A => M[B])(during: A => M[C]): M[C] =
    for {
      a <- before
      r <- onException(during(a), after(a))
      _ <- after(a)
    } yield r

  /** Like "bracket", but takes only a computation to run afterward. Generalizes "finally". */
  def ensuring[M[_]: Monad: Catchable, A, B](ma: M[A], sequel: M[B]): M[A] =
    for {
      r <- onException(ma, sequel)
      _ <- sequel
    } yield r

  /** A variant of "bracket" where the return value of this computation is not needed. */
  def bracket_[M[_]: Monad: Catchable, A, B, C](before: M[A])(after: M[B])(during: M[C]): M[C] =
    bracket(before)(_ => after)(_ => during)

  /** A variant of "bracket" that performs the final action only if there was an error. */
  def bracketOnError[M[_]: Monad: Catchable, A, B, C](before: M[A])(after: A => M[B])(during: A => M[C]): M[C] =
    for {
      a <- before
      r <- onException(during(a), after(a))
    } yield r

}