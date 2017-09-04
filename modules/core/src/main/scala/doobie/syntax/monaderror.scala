// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.syntax

import cats.MonadError
import doobie.util.{ catchsql => C }
import doobie.enum.SqlState
import java.sql.SQLException

class MonadErrorOps[M[_]: MonadError[?[_], Throwable], A](self: M[A]) {
  def attemptSql: M[Either[SQLException, A]] = C.attemptSql(self)
  def attemptSqlState: M[Either[SqlState, A]] = C.attemptSqlState(self)
  def attemptSomeSqlState[B](f: PartialFunction[SqlState, B]): M[Either[B, A]] = C.attemptSomeSqlState(self)(f)
  def exceptSql(handler: SQLException => M[A]): M[A] = C.exceptSql(self)(handler)
  def exceptSqlState(handler: SqlState => M[A]): M[A] = C.exceptSqlState(self)(handler)
  def exceptSomeSqlState(pf: PartialFunction[SqlState, M[A]]): M[A] = C.exceptSomeSqlState(self)(pf)
  def onSqlException[B](action: M[B]): M[A] = C.onSqlException(self)(action)
  def guarantee(finalizer: M[Unit]): M[A] = C.guarantee(self)(finalizer)
  def onError(handler: M[_]): M[A] = C.onError(self)(handler)
}

trait ToMonadErrorOps {
  implicit def toDoobieMonadErrorOps[M[_]: MonadError[?[_], Throwable], A](ma: M[A]): MonadErrorOps[M, A] =
    new MonadErrorOps(ma)
}

object monaderror extends ToMonadErrorOps
