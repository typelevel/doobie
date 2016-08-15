package doobie.syntax

#+scalaz
import scalaz.{ Monad, Catchable, \/, Unapply }
#-scalaz
import doobie.util.{ catchsql => C }
#+cats
import cats.Unapply
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

  trait ToDoobieCatchSqlOps0 {

#+scalaz
    /** @group Syntax */
    implicit def toDoobieCatchSqlOpsUnapply[MA](ma: MA)(
      implicit M0: Unapply[Monad, MA],
               C0: Unapply[Catchable, MA]
    ): DoobieCatchSqlOps[M0.M, M0.A] =
      new DoobieCatchSqlOps[M0.M, M0.A](M0.apply(ma))(M0.TC, C0.TC.asInstanceOf[Catchable[M0.M]])
#-scalaz
#+fs2
    /** @group Syntax */
    implicit def toDoobieCatchSqlOpsUnapply[MA](ma: MA)(
      implicit C0: Unapply[Catchable, MA]
    ): DoobieCatchSqlOps[C0.M, C0.A] =
      new DoobieCatchSqlOps[C0.M, C0.A](C0.subst(ma))(C0.TC.asInstanceOf[Catchable[C0.M]])
#-fs2

  }

  trait ToDoobieCatchSqlOps extends ToDoobieCatchSqlOps0 {

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
