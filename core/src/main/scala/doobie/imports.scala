package doobie

import scalaz.{ Monad, Catchable }
import scalaz.stream.Process

/** Single import for commonly-used stuff, if that's your kind of thing. */
object imports {
  
  val FC   = doobie.free.connection
  val FS   = doobie.free.statement
  val FPS  = doobie.free.preparedstatement
  val FRS  = doobie.free.resultset

  val HC   = doobie.hi.connection
  val HS   = doobie.hi.statement
  val HPS  = doobie.hi.preparedstatement
  val HRS  = doobie.hi.resultset

  type ConnectionIO[A]        = doobie.free.connection.ConnectionIO[A]
  type StatementIO[A]         = doobie.free.statement.StatementIO[A]
  type PreparedStatementIO[A] = doobie.free.preparedstatement.PreparedStatementIO[A]
  type ResultSetIO[A]         = doobie.free.resultset.ResultSetIO[A]

  // Syntax

  implicit def toDoobieCatchableOps[M[_]: Monad: Catchable, A](ma: M[A]) =
    new doobie.syntax.catchable.DoobieCatchableOps(ma)

  implicit def toDoobieCatchSqlOps[M[_]: Monad: Catchable, A](ma: M[A]) =
    new doobie.syntax.catchsql.DoobieCatchSqlOps(ma)

  implicit def toProcessOps[F[_]: Monad: Catchable, A](fa: Process[F, A]) =
    new doobie.syntax.process.ProcessOps(fa)

  implicit def toSqlInterpolator(sc: StringContext) =
    new doobie.syntax.string.SqlInterpolator(sc)

  // Util

  type Meta[A] = doobie.util.meta.Meta[A]
  val  Meta    = doobie.util.meta.Meta

  type Atom[A] = doobie.util.atom.Atom[A]
  val  Atom    = doobie.util.atom.Atom

  type Composite[A] = doobie.util.composite.Composite[A]
  val  Composite    = doobie.util.composite.Composite

  type Query[A,B] = doobie.util.query.Query[A,B]
  type Query0[A]  = doobie.util.query.Query0[A]

  type Update[A] = doobie.util.update.Update[A]
  type Update0   = doobie.util.update.Update0

  type Transactor[M[_]] = doobie.util.transactor.Transactor[M]

  val  DriverManagerTransactor = doobie.util.transactor.DriverManagerTransactor

}
