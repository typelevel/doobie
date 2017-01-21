package doobie.syntax

#+scalaz
import scalaz.{ Monad, Catchable, \/ }
#-scalaz
import doobie.util.{ catchsql => C }
#+cats
import scala.util.{ Either => \/ }
#-cats
#+fs2
import fs2.util.Catchable
#-fs2
import doobie.enum.sqlstate.SqlState
import java.sql.SQLException

/** Syntax for `Catchable` combinators defined in `util.catchsql`. */
object catchsql {

#+scalaz
  class DoobieCatchSqlOps[M[_]: Monad: Catchable, A](self: M[A]) {
#-scalaz
#+fs2
  class DoobieCatchSqlOps[M[_]: Catchable, A](self: M[A]) {
#-fs2

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
#+scalaz
    implicit def toDoobieCatchSqlOps[M[_]: Monad: Catchable, A](ma: M[A]): DoobieCatchSqlOps[M, A] =
#-scalaz
#+fs2
    implicit def toDoobieCatchSqlOps[M[_]: Catchable, A](ma: M[A]): DoobieCatchSqlOps[M, A] =
#-fs2
      new DoobieCatchSqlOps(ma)

  }

}
