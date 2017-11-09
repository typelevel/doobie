// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.MonadError
import cats.implicits._
import doobie.enum.SqlState
import java.sql.SQLException

/**
 * Module of additional combinators for `Catchable`, specific to `SQLException`.
 */
object catchsql {

  /** Like `attempt` but catches only `SQLException`. */
  def attemptSql[M[_]: MonadError[?[_], Throwable], A](ma: M[A]): M[Either[SQLException, A]] =
    ma.attempt.map(_.leftMap {
      case sqle: SQLException => sqle
      case e                  => throw e
    })

  /** Like `attemptSql` but yields only the exception's `SqlState`. */
  def attemptSqlState[M[_]: MonadError[?[_], Throwable], A](ma: M[A]): M[Either[SqlState, A]] =
    attemptSql(ma).map(_.leftMap(e => SqlState(e.getSQLState)))

  def attemptSomeSqlState[M[_]: MonadError[?[_], Throwable], A, B](ma: M[A])(f: PartialFunction[SqlState, B]): M[Either[B, A]] =
    attemptSql(ma).map(_.leftMap(sqle => f.lift(SqlState(sqle.getSQLState)).getOrElse(throw sqle)))

  /** Executes the handler, for exceptions propagating from `ma`. */
  def exceptSql[M[_]: MonadError[?[_], Throwable], A](ma: M[A])(handler: SQLException => M[A]): M[A] =
    attemptSql(ma).flatMap(_.bimap(handler, _.pure[M]).merge)

  /** Executes the handler, for exceptions propagating from `ma`. */
  def exceptSqlState[M[_]: MonadError[?[_], Throwable], A](ma: M[A])(handler: SqlState => M[A]): M[A] =
    exceptSql(ma)(e => handler(SqlState(e.getSQLState)))

  /** Executes the handler where defined, for exceptions propagating from `ma`. */
  def exceptSomeSqlState[M[_]: MonadError[?[_], Throwable], A](ma: M[A])(pf: PartialFunction[SqlState, M[A]]): M[A] =
    exceptSql(ma)(e => pf.lift(SqlState(e.getSQLState)).getOrElse((throw e): M[A]))

  /** Like "finally", but only performs the final action if there was an exception. */
  def onSqlException[M[_], A, B](ma: M[A])(action: M[B])(implicit c: MonadError[M, Throwable]): M[A] =
    exceptSql(ma)(e => action *> c.raiseError(e))

  def guarantee[M[_], A](ma: M[A])(finalizer: M[Unit])(implicit ev: MonadError[M, Throwable]): M[A] =
    ev.flatMap(ev.attempt(ma)) { e =>
      ev.flatMap(finalizer)(_ => e.fold(ev.raiseError, ev.pure))
    }

  def onError[M[_], A](ma: M[A])(handler: M[_])(implicit ev: MonadError[M, Throwable]): M[A] =
    ev.handleErrorWith(ma)(e => ev.flatMap(handler)(_ => ev.raiseError(e)))

}
