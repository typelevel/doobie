package doobie

import scalaz.{ Monad, Catchable }
import scalaz.stream.Process

/** Module of aliases for commonly-used types and syntax; use as `import doobie.imports._` */
object imports {

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
   * Alias for `doobie.hi.drivermanager`.
   * @group Hi Module Aliases
   */
  val HDM  = doobie.hi.drivermanager

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
  implicit def toDoobieCatchableOps[M[_]: Monad: Catchable, A](ma: M[A]) =
    new doobie.syntax.catchable.DoobieCatchableOps(ma)

  /** @group Syntax */
  implicit def toDoobieCatchSqlOps[M[_]: Monad: Catchable, A](ma: M[A]) =
    new doobie.syntax.catchsql.DoobieCatchSqlOps(ma)

  /** @group Syntax */
  implicit def toProcessOps[F[_]: Monad: Catchable, A](fa: Process[F, A]) =
    new doobie.syntax.process.ProcessOps(fa)

  /** @group Syntax */
  implicit def toSqlInterpolator(sc: StringContext) =
    new doobie.syntax.string.SqlInterpolator(sc)

  /** @group Syntax */
  implicit def toMoreConnectionIOOps[A](ma: ConnectionIO[A]) =
    new doobie.syntax.connectionio.MoreConnectionIOOps(ma)

  /** @group Type Aliases */      type Meta[A] = doobie.util.meta.Meta[A]
  /** @group Companion Aliases */ val  Meta    = doobie.util.meta.Meta

  /** @group Type Aliases */      type Atom[A] = doobie.util.atom.Atom[A]
  /** @group Companion Aliases */ val  Atom    = doobie.util.atom.Atom

  /** @group Type Aliases */      type Capture[M[_]] = doobie.util.capture.Capture[M]
  /** @group Companion Aliases */ val  Capture       = doobie.util.capture.Capture

  /** @group Type Aliases */      type Composite[A] = doobie.util.composite.Composite[A]
  /** @group Companion Aliases */ val  Composite    = doobie.util.composite.Composite

  /** @group Type Aliases */ type Query[A,B] = doobie.util.query.Query[A,B]
  /** @group Type Aliases */ type Query0[A]  = doobie.util.query.Query0[A]

  /** @group Type Aliases */ type Update[A] = doobie.util.update.Update[A]
  /** @group Type Aliases */ type Update0   = doobie.util.update.Update0

  /** @group Type Aliases */ type Transactor[M[_]] = doobie.util.transactor.Transactor[M]

  /** @group Companion Aliases */ val DriverManagerTransactor = doobie.util.transactor.DriverManagerTransactor

  /** @group Typeclass Instances */
  implicit val NameCatchable = doobie.util.name.NameCatchable

  /** @group Typeclass Instances */
  implicit val NameCapture   = doobie.util.name.NameCapture

}
