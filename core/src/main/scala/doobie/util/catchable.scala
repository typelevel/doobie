package doobie.util

import scalaz.{ Monad, Catchable, \/, -\/, \/- }
import scalaz.syntax.bifunctor._
import scalaz.syntax.monad._
import scalaz.syntax.catchable._

/** 
 * Module of additional combinators for `Catchable`, all defined in terms of `attempt`. Similar to 
 * those provided for `MonadCatchIO`. 
 */
object catchable {

  /** Like `attempt` but catches (and maps) only where defined. */
  def attemptSome[M[_]: Monad: Catchable, A, B](ma: M[A])(p: PartialFunction[Throwable, B]): M[B \/ A] =
    ma.attempt.map(_.leftMap(e => p.lift(e).getOrElse(throw e)))

  /** Executes the handler, for exceptions propagating from `ma`. */
  def except[M[_]: Monad: Catchable, A](ma: M[A])(handler: Throwable => M[A]): M[A] =
    ma.attempt.flatMap(_.bimap(handler, _.point[M]).merge)

  /** Executes the handler where defined, for exceptions propagating from `ma`. */
  def exceptSome[M[_]: Monad: Catchable, A](ma: M[A])(pf: PartialFunction[Throwable, M[A]]): M[A] =
    except(ma)(e => pf.lift(e).getOrElse((throw e): M[A]))

  /** Like "finally", but only performs the final action if there was an exception. */
  def onException[M[_]: Monad: Catchable, A, B](ma: M[A])(action: M[B]): M[A] =
    except(ma)(e => action *> ((throw e): M[A]))

  /** Always execute `sequel` following `ma`; generalizes `finally`. */
  def ensuring[M[_]: Monad: Catchable, A, B](ma: M[A])(sequel: M[B]): M[A] =
    onException(ma)(sequel) <* sequel

}

