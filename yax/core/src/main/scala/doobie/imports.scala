package doobie

import doobie.util.pos.Pos
import doobie.syntax.catchsql.ToDoobieCatchSqlOps
import doobie.syntax.catchable.ToDoobieCatchableOps
import doobie.syntax.foldable.ToDoobieFoldableOps

#+scalaz
import scalaz.{ Monad, Catchable, Leibniz, Free, Functor }
import scalaz.stream.Process
#-scalaz
#+cats
import cats.{ Monad, Functor }
import cats.free.Free
#-cats
#+fs2
import fs2.util.{ Catchable, Suspendable }
import fs2.{ Stream => Process }
#-fs2

/** Module of aliases for commonly-used types and syntax; use as `import doobie.imports._` */
object imports extends ToDoobieCatchSqlOps
                  with ToDoobieCatchableOps
                  with ToDoobieFoldableOps {

  /**
   * Alias for `doobie.free.connection`.
   * @group Free Module Aliases
   */
  val FC   = doobie.free.connection

  /**
   * Alias for `doobie.free.statement`.
   * @group Free Module Aliases
   */
  val FS   = doobie.free.statement

  /**
   * Alias for `doobie.free.preparedstatement`.
   * @group Free Module Aliases
   */
  val FPS  = doobie.free.preparedstatement

  /**
   * Alias for `doobie.free.resultset`.
   * @group Free Module Aliases
   */
  val FRS  = doobie.free.resultset

  /**
   * Alias for `doobie.hi.connection`.
   * @group Hi Module Aliases
   */
  val HC   = doobie.hi.connection

  /**
   * Alias for `doobie.hi.statement`.
   * @group Hi Module Aliases
   */
  val HS   = doobie.hi.statement

  /**
   * Alias for `doobie.hi.preparedstatement`.
   * @group Hi Module Aliases
   */
  val HPS  = doobie.hi.preparedstatement

  /**
   * Alias for `doobie.hi.resultset`.
   * @group Hi Module Aliases
   */
  val HRS  = doobie.hi.resultset

  /** @group Type Aliases */ type ConnectionIO[A]        = doobie.free.connection.ConnectionIO[A]
  /** @group Type Aliases */ type StatementIO[A]         = doobie.free.statement.StatementIO[A]
  /** @group Type Aliases */ type PreparedStatementIO[A] = doobie.free.preparedstatement.PreparedStatementIO[A]
  /** @group Type Aliases */ type ResultSetIO[A]         = doobie.free.resultset.ResultSetIO[A]

  /** @group Syntax */
#+scalaz
  implicit def toProcessOps[F[_]: Monad: Catchable: Capture, A](fa: Process[F, A]): doobie.syntax.process.ProcessOps[F, A] =
#-scalaz
#+fs2
  implicit def toProcessOps[F[_]: Catchable: Suspendable, A](fa: Process[F, A]): doobie.syntax.process.ProcessOps[F, A] =
#-fs2
    new doobie.syntax.process.ProcessOps(fa)

  /** @group Syntax */
  implicit def toSqlInterpolator(sc: StringContext)(implicit pos: Pos): doobie.syntax.string.SqlInterpolator =
    new doobie.syntax.string.SqlInterpolator(sc)

  /** @group Syntax */
  implicit def toMoreConnectionIOOps[A](ma: ConnectionIO[A]): doobie.syntax.connectionio.MoreConnectionIOOps[A] =
    new doobie.syntax.connectionio.MoreConnectionIOOps(ma)

  /** @group Type Aliases */      type Meta[A] = doobie.util.meta.Meta[A]
  /** @group Companion Aliases */ val  Meta    = doobie.util.meta.Meta

  /** @group Type Aliases */      type Atom[A] = doobie.util.atom.Atom[A]
  /** @group Companion Aliases */ val  Atom    = doobie.util.atom.Atom

#+scalaz
  /** @group Type Aliases */      type Capture[M[_]] = doobie.util.capture.Capture[M]
  /** @group Companion Aliases */ val  Capture       = doobie.util.capture.Capture
#-scalaz

  /** @group Type Aliases */      type Composite[A] = doobie.util.composite.Composite[A]
  /** @group Companion Aliases */ val  Composite    = doobie.util.composite.Composite

  /** @group Type Aliases */      type Query[A,B] = doobie.util.query.Query[A,B]
  /** @group Companion Aliases */ val  Query      = doobie.util.query.Query

  /** @group Type Aliases */      type Update[A] = doobie.util.update.Update[A]
  /** @group Companion Aliases */ val  Update    = doobie.util.update.Update

  /** @group Type Aliases */      type Query0[A]  = doobie.util.query.Query0[A]
  /** @group Companion Aliases */ val  Query0     = doobie.util.query.Query0

  /** @group Type Aliases */      type Update0   = doobie.util.update.Update0
  /** @group Companion Aliases */ val  Update0   = doobie.util.update.Update0

  /** @group Type Aliases */      type SqlState = doobie.enum.sqlstate.SqlState
  /** @group Companion Aliases */ val  SqlState = doobie.enum.sqlstate.SqlState

  /** @group Type Aliases */      type Param[A] = doobie.util.param.Param[A]
  /** @group Companion Aliases */ val  Param    = doobie.util.param.Param

  /** @group Type Aliases */ type Transactor[M[_]] = doobie.util.transactor.Transactor[M]

  /** @group Companion Aliases */ val DriverManagerTransactor = doobie.util.transactor.DriverManagerTransactor
  /** @group Companion Aliases */ val DataSourceTransactor = doobie.util.transactor.DataSourceTransactor

  /** @group Type Aliases */      type IOLite[A] = doobie.util.iolite.IOLite[A]
  /** @group Companion Aliases */ val  IOLite    = doobie.util.iolite.IOLite

  /** @group Type Aliases */      type LogHandler = doobie.util.log.LogHandler
  /** @group Companion Aliases */ val  LogHandler = doobie.util.log.LogHandler

  /** @group Type Aliases */      type Fragment = doobie.util.fragment.Fragment
  /** @group Companion Aliases */ val  Fragment = doobie.util.fragment.Fragment

  /** @group Type Aliases */      type KleisliInterpreter[F[_]] = doobie.free.KleisliInterpreter[F]
  /** @group Companion Aliases */ val  KleisliInterpreter       = doobie.free.KleisliInterpreter

  /** @group Companion Aliases */ val  Fragments = doobie.util.fragments

#+scalaz
  /** @group Typeclass Instances */
  implicit val NameCatchable = doobie.util.name.NameCatchable

  /** @group Typeclass Instances */
  implicit val NameCapture   = doobie.util.name.NameCapture
#-scalaz

}
