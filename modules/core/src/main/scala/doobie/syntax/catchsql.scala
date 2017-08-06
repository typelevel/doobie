package doobie.syntax

import cats.MonadError
import doobie.util.{ catchsql => C }
import scala.util.{ Either => \/ }
import doobie.enum.sqlstate.SqlState
import java.sql.SQLException

/** Syntax for `Catchable` combinators defined in `util.catchsql`. */
object catchsql {

  class DoobieCatchSqlOps[M[_]: MonadError[?[_], Throwable], A](self: M[A]) {

    def attemptSql: M[SQLException \/ A] =
      C.attemptSql(self)

    def attemptSqlState: M[SqlState \/ A] =
      C.attemptSqlState(self)

    def attemptSomeSqlState[B](f: PartialFunction[SqlState, B]): M[B \/ A] =
      C.attemptSomeSqlState(self)(f)

    def exceptSql(handler: SQLException => M[A]): M[A] =
      C.exceptSql(self)(handler)

    def exceptSqlState(handler: SqlState => M[A]): M[A] =
      C.exceptSqlState(self)(handler)

    def exceptSomeSqlState(pf: PartialFunction[SqlState, M[A]]): M[A] =
      C.exceptSomeSqlState(self)(pf)

    def onSqlException[B](action: M[B]): M[A] =
      C.onSqlException(self)(action)

  }

  trait ToDoobieCatchSqlOps {

    /** @group Syntax */
    implicit def toDoobieCatchSqlOps[M[_]: MonadError[?[_], Throwable], A](ma: M[A]): DoobieCatchSqlOps[M, A] =
      new DoobieCatchSqlOps(ma)

  }

}
