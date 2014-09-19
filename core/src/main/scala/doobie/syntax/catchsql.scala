package doobie.syntax

import scalaz.{ Monad, Catchable, \/, -\/, \/- }
import doobie.util.{ catchsql => C }
import doobie.enum.sqlstate.SqlState
import java.sql.SQLException

/** Syntax for `Catchable` combinators defined in `util.catchsql`. */
object catchsql {

  implicit class DoobieCatchaSqlOps[M[_]: Monad: Catchable, A](self: M[A]) {

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

}
