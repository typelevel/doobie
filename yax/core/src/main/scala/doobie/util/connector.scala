package doobie.util

import doobie.free.connection.{ ConnectionIO, ConnectionOp, setAutoCommit, commit, rollback, close, unit, delay }
import doobie.free.KleisliInterpreter
import doobie.util.lens._
import doobie.util.yolo.Yolo

#+scalaz
import scalaz.syntax.monad._
import scalaz.stream.Process
import scalaz.stream.Process. { eval, eval_, halt }
import scalaz.{ Free, Monad, Catchable, Kleisli, ~> }
import scalaz.stream.Process
import doobie.util.capture._
#-scalaz
#+cats
import cats.{ Monad, ~> }
import cats.data.Kleisli
import cats.free.Free
import cats.implicits._
#-cats
#+fs2
import fs2.{ Stream => Process }
import fs2.Stream.{ eval, eval_ }
import fs2.util.{ Catchable, Suspendable => Capture  }
import fs2.interop.cats._
import fs2.interop.cats.reverse.functionKToUf1
#-fs2

import java.sql.{ Connection, DriverManager }
import javax.sql.DataSource
import scala.Predef.implicitly

object transactor  {
  private val syntax = new doobie.syntax.catchable.ToDoobieCatchableOps {}
  import syntax._

  /** @group Type Aliases */
  type Interpreter[M[_]] = ConnectionOp ~> Kleisli[M, Connection, ?]

  /**
   * Data type representing the common setup, error-handling, and cleanup strategy associated with
   * an SQL transaction. A `Transactor` uses a `Strategy` to wrap programs prior to execution.
   * @param before a program to prepare the connection for use
   * @param after  a program to run on success
   * @param oops   a program to run on failure (catch)
   * @param always a programe to run in all cases (finally)
   * @group Data Types
   */
  case class Strategy(
    before: ConnectionIO[Unit],
    after:  ConnectionIO[Unit],
    oops:   ConnectionIO[Unit],
    always: ConnectionIO[Unit]
  ) {

    private implicit class VoidProcessOps(ma: ConnectionIO[Unit]) {
      def p: Process[ConnectionIO, Nothing] = eval_(ma) // empty effectful process
    }

    /** Natural transformation that wraps a `ConnectionIO` program. */
    val wrap = λ[ConnectionIO ~> ConnectionIO] { ma =>
      (before *> ma <* after) onException oops ensuring always
    }

    /** Natural transformation that wraps a `ConnectionIO` stream. */
    val wrapP = λ[Process[ConnectionIO, ?] ~> Process[ConnectionIO, ?]] { pa =>
#+scalaz
        (before.p ++ pa ++ after.p) onFailure { e => oops.p ++ eval_(delay(throw e)) } onComplete always.p
#-scalaz
#+fs2
        (before.p ++ pa ++ after.p) onError { e => oops.p ++ eval_(delay(throw e)) } onFinalize always
#-fs2
    }

    /**
     * Natural transformation that wraps a `Kleisli` program, using the provided interpreter to
     * interpret the `before`/`after`/`oops`/`always` strategy.
     */
    def wrapK[M[_]: Monad: Catchable](interp: Interpreter[M]) =
      λ[Kleisli[M, Connection, ?] ~> Kleisli[M, Connection, ?]] { ka =>
        val beforeʹ = before foldMap interp
        val afterʹ  = after  foldMap interp
        val oopsʹ   = oops   foldMap interp
        val alwaysʹ = always foldMap interp
        (beforeʹ *> ka <* afterʹ) onException oopsʹ ensuring alwaysʹ
      }

  }
  object Strategy {

    /** @group Lenses */ val before: Strategy @> ConnectionIO[Unit] = Lens(_.before, (a, b) => a.copy(before = b))
    /** @group Lenses */ val after:  Strategy @> ConnectionIO[Unit] = Lens(_.after,  (a, b) => a.copy(after  = b))
    /** @group Lenses */ val oops:   Strategy @> ConnectionIO[Unit] = Lens(_.oops,   (a, b) => a.copy(oops   = b))
    /** @group Lenses */ val always: Strategy @> ConnectionIO[Unit] = Lens(_.always, (a, b) => a.copy(always = b))

    /**
     * A default `Strategy` with the following properties:
     * - Auto-commit will be set to `false`;
     * - the transaction will `commit` on success and `rollback` on failure;
     * - and finally the connection will be closed in all cases.
     * @group Constructors
     */
    val default = Strategy(setAutoCommit(false), commit, rollback, close)

  }

  /**
   * A thin wrapper around a source of database connections, an interpreter, and a strategy for
   * running programs, parameterized over a target monad `M` and an arbitrary wrapped value `A`.
   * Given a stream or program in `ConnectionIO` or a program in `Kleisli`, a `Transactor` can
   * discharge the doobie machinery and yield an effectful stream or program in `M`.
   * @param kernel    an arbitrary value, meaningful to the instance
   * @param connect   a program in `M` that can provide a database connection, given the kernel
   * @param interpret a natural transformation for interpreting `ConnectionIO`
   * @param strategy  a `Strategy` for running a program on a connection
   * @tparam M a target effect type; typically `IO` or `Task`
   * @tparam A an arbitrary value that will be handed back to `connect`
   * @group Data Types
   */
  case class Transactor[M[_], A](
    kernel:    A,
    connect:   A => M[Connection],
    interpret: Interpreter[M],
    strategy:  Strategy
  ) {

    /** Construct a [[Yolo]] for REPL experimentation. */
    def yolo(implicit ev: Monad[M], ev1: Catchable[M], ev2: Capture[M]): Yolo[M] = new Yolo(this)

    /**
     * Construct a program to peform arbitrary configuration on the kernel value (changing the
     * timeout on a connection pool, for example). This can be the basis for constructing a
     * configuration language for a specific kernel type `A`, whose operations can be added to
     * compatible `Transactor`s via implicit conversion.
     * @group Configuration
     */
    def configure(f: A => Unit)(implicit ev: Capture[M]): M[Unit] =
      ev.delay(f(kernel))

    /**
     * Natural transformation equivalent to `exec` that does not use the provided `Strategy` and
     * instead directly binds the `Connection` provided by `connect`. This can be useful in cases
     * where transactional handling is unsupported or undesired.
     * @group Natural Transformations
     */
    def rawExec(implicit ev: Monad[M]): Kleisli[M, Connection, ?] ~> M =
      λ[Kleisli[M, Connection, ?] ~> M](k => connect(kernel).flatMap(k.run))

    /**
     * Natural transformation that provides a connection and binds through a `Kleisli` program
     * using the given `Strategy`, yielding an independent program in `M`.
     * @group Natural Transformations
     */
    def exec(implicit ev: Monad[M], ev2: Catchable[M]): Kleisli[M, Connection, ?] ~> M =
      strategy.wrapK(interpret)(ev, ev2) andThen rawExec(ev)

    /**
     * Natural transformation equivalent to `trans` that does not use the provided `Strategy` and
     * instead directly binds the `Connection` provided by `connect`. This can be useful in cases
     * where transactional handling is unsupported or undesired.
     * @group Natural Transformations
     */
    def rawTrans(implicit ev: Monad[M]): ConnectionIO ~> M =
      λ[ConnectionIO ~> M](f => connect(kernel).flatMap(f.foldMap(interpret).run))

    /**
     * Natural transformation that provides a connection and binds through a `ConnectionIO` program
     * interpreted via the given interpreter, using the given `Strategy`, yielding an independent
     * program in `M`. This is the most common way to run a doobie program.
     * @group Natural Transformations
     */
    def trans(implicit ev: Monad[M]): ConnectionIO ~> M =
      strategy.wrap andThen rawTrans

    def rawTransP(implicit ev: Monad[M]) = λ[Process[ConnectionIO, ?] ~> Process[M, ?]] { pa =>
      // TODO: this can almost certainly be simplified

      // Natural transformation by Kleisli application.
      def applyKleisli[F[_], E](e: E) =
        λ[Kleisli[F, E, ?] ~> F](_.run(e))

      // Lift a natural translation over an functor to one over its free monad.
      def liftF[F[_], G[_]: Monad](nat: F ~> G) =
        λ[Free[F, ?] ~> G](_.foldMap(nat))

      def nat(c: Connection): ConnectionIO ~> M = liftF(interpret andThen applyKleisli(c))
#+scalaz
      eval(connect(kernel)).flatMap(c => pa.translate[M](nat(c)))
#-scalaz
#+fs2
      eval(connect(kernel)).flatMap(c => pa.translate[M](functionKToUf1(nat(c))))
#-fs2
     }

    def transP(implicit ev: Monad[M]): Process[ConnectionIO, ?] ~> Process[M, ?] =
      strategy.wrapP andThen rawTransP

  }

  object Transactor {

    /** @group Lenses */ def kernel   [M[_], A]: Transactor[M, A] Lens A                    = Lens(_.kernel,    (a, b) => a.copy(kernel    = b))
    /** @group Lenses */ def connect  [M[_], A]: Transactor[M, A] Lens (A => M[Connection]) = Lens(_.connect,   (a, b) => a.copy(connect   = b))
    /** @group Lenses */ def interpret[M[_], A]: Transactor[M, A] Lens Interpreter[M]       = Lens(_.interpret, (a, b) => a.copy(interpret = b))
    /** @group Lenses */ def strategy [M[_], A]: Transactor[M, A] Lens Strategy             = Lens(_.strategy,  (a, b) => a.copy(strategy  = b))
    /** @group Lenses */ def before   [M[_], A]: Transactor[M, A] Lens ConnectionIO[Unit]   = strategy[M, A] >=> Strategy.before
    /** @group Lenses */ def after    [M[_], A]: Transactor[M, A] Lens ConnectionIO[Unit]   = strategy[M, A] >=> Strategy.after
    /** @group Lenses */ def oops     [M[_], A]: Transactor[M, A] Lens ConnectionIO[Unit]   = strategy[M, A] >=> Strategy.oops
    /** @group Lenses */ def always   [M[_], A]: Transactor[M, A] Lens ConnectionIO[Unit]   = strategy[M, A] >=> Strategy.always

    /**
     * Construct a constructor of `Transactor[M, D]` for some `D <: DataSource` by partial
     * application of `M`, which cannot be inferred in general. This follows the pattern described
     * [here](http://tpolecat.github.io/2015/07/30/infer.html).
     * @group Constructors
     */
     object fromDataSource {
       def apply[M[_]] = new FromDataSourceUnapplied[M]

      /**
       * Constructor of `Transactor[M, D]` fixed for `M`; see the `apply` method for details.
       * @group Constructors (Partially Applied)
       */
      class FromDataSourceUnapplied[M[_]] {
        def apply[A <: DataSource](a: A)(implicit ev0: Monad[M], ev1: Catchable[M], ev2: Capture[M]): Transactor[M, A] =
          Transactor(a, a => ev2.delay(a.getConnection), KleisliInterpreter[M](ev0, implicitly, implicitly).ConnectionInterpreter, Strategy.default)
      }
    }
    @deprecated("use Transactor.fromDataSource", "0.4.2")
    val DataSourceTransactor: fromDataSource.type = fromDataSource

    /** @group Constructors */
    def fromConnection[M[_]: Catchable: Capture](a: Connection)(implicit M: Monad[M]): Transactor[M, Connection] =
      apply(a, M.pure(_), KleisliInterpreter[M](M, implicitly, implicitly).ConnectionInterpreter, Strategy.default.copy(always = unit))

    /**
     * Construct a constructor of `Transactor[M, Unit]` backed by the JDBC DriverManager by partial
     * application of `M`, which cannot be inferred in general. This follows the pattern described
     * [here](http://tpolecat.github.io/2015/07/30/infer.html).
     * @group Constructors
     */
    object fromDriverManager {

#+scalaz
      private def create[M[_]: Monad: Capture: Catchable](driver: String, conn: => Connection): Transactor[M, Unit] =
#-scalaz
#+cats
      private def create[M[_]: Capture: Catchable](driver: String, conn: => Connection): Transactor[M, Unit] =
#-cats
        Transactor((), u => Capture[M].delay { Class.forName(driver); conn }, KleisliInterpreter[M].ConnectionInterpreter, Strategy.default)

      def apply[M[_]: Monad: Capture: Catchable](driver: String, url: String): Transactor[M, Unit] =
        create(driver, DriverManager.getConnection(url))

      def apply[M[_]: Monad: Capture: Catchable](driver: String, url: String, user: String, pass: String): Transactor[M, Unit] =
        create(driver, DriverManager.getConnection(url, user, pass))

      def apply[M[_]: Monad: Capture: Catchable](driver: String, url: String, info: java.util.Properties): Transactor[M, Unit] =
        create(driver, DriverManager.getConnection(url, info))

    }
    @deprecated("use Transactor.fromDriverManager", "0.4.2")
    val DriverManagerTransactor: fromDriverManager.type = fromDriverManager

  }


}
