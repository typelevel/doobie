package doobie

import doobie.util.pos.Pos
import doobie.syntax.catchsql.ToDoobieCatchSqlOps
import doobie.syntax.foldable.ToDoobieFoldableOps

import cats.effect.Sync
import fs2.{ Stream => Process }

/** Module of aliases for commonly-used types and syntax; use as `import doobie.imports._` */
object imports extends ToDoobieCatchSqlOps
                  with ToDoobieFoldableOps {

  /**
   * Alias for `doobie.free.connection`.
   * @group Free Module Aliases
   */
  val FC   = doobie.free.connection
  implicit val AsyncC = FC.AsyncConnectionIO

  /**
   * Alias for `doobie.free.statement`.
   * @group Free Module Aliases
   */
  val FS   = doobie.free.statement
  implicit val AsyncS = FS.AsyncStatementIO

  /**
   * Alias for `doobie.free.preparedstatement`.
   * @group Free Module Aliases
   */
  val FPS  = doobie.free.preparedstatement
  implicit val AsyncPS = FPS.AsyncPreparedStatementIO

  /**
   * Alias for `doobie.free.resultset`.
   * @group Free Module Aliases
   */
  val FRS  = doobie.free.resultset
  implicit val AsyncRS = FRS.AsyncResultSetIO

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

  /** @group Type Aliases */ type ConnectionIO[A]        =  FC.ConnectionIO[A]
  /** @group Type Aliases */ type StatementIO[A]         =  FS.StatementIO[A]
  /** @group Type Aliases */ type PreparedStatementIO[A] = FPS.PreparedStatementIO[A]
  /** @group Type Aliases */ type ResultSetIO[A]         = FRS.ResultSetIO[A]

  /** @group Syntax */
  implicit def toProcessOps[F[_]: Sync, A](fa: Process[F, A]): doobie.syntax.process.ProcessOps[F, A] =
    new doobie.syntax.process.ProcessOps(fa)

  /** @group Syntax */
  implicit def toSqlInterpolator(sc: StringContext)(implicit pos: Pos): doobie.syntax.string.SqlInterpolator =
    new doobie.syntax.string.SqlInterpolator(sc)

  /** @group Syntax */
  implicit def toMoreConnectionIOOps[A](ma: ConnectionIO[A]): doobie.syntax.connectionio.MoreConnectionIOOps[A] =
    new doobie.syntax.connectionio.MoreConnectionIOOps(ma)

  /** @group Type Aliases */      type Meta[A] = doobie.util.meta.Meta[A]
  /** @group Companion Aliases */ val  Meta    = doobie.util.meta.Meta


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

  /** @group Type Aliases */      type Transactor[M[_]] = doobie.util.transactor.Transactor[M]
  /** @group Type Aliases */      val  Transactor          = doobie.util.transactor.Transactor

  /** @group Type Aliases */      type LogHandler = doobie.util.log.LogHandler
  /** @group Companion Aliases */ val  LogHandler = doobie.util.log.LogHandler

  /** @group Type Aliases */      type Fragment = doobie.util.fragment.Fragment
  /** @group Companion Aliases */ val  Fragment = doobie.util.fragment.Fragment

  /** @group Type Aliases */      type KleisliInterpreter[F[_]] = doobie.free.KleisliInterpreter[F]
  /** @group Companion Aliases */ val  KleisliInterpreter       = doobie.free.KleisliInterpreter

  /** @group Companion Aliases */ val  Fragments = doobie.util.fragments

}
