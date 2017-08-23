package doobie.util

import doobie.free.connection.{ ConnectionIO, ConnectionOp, setAutoCommit, commit, rollback, close, unit, delay }
import doobie.free.KleisliInterpreter
import doobie.util.lens._
import doobie.util.yolo.Yolo
import doobie.syntax.monaderror._

import cats.{ Monad, MonadError, ~> }
import cats.data.Kleisli
import cats.free.Free
import cats.implicits._
import cats.effect.{ Effect, Sync, Async }
import fs2.Stream
import fs2.Stream.{ eval, eval_ }

import java.sql.{ Connection, DriverManager }
import javax.sql.DataSource

object transactor  {

  import doobie.free.connection.AsyncConnectionIO

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

    private implicit class VoidStreamOps(ma: ConnectionIO[Unit]) {
      def p: Stream[ConnectionIO, Nothing] = eval_(ma) // empty effectful process
    }

    /** Natural transformation that wraps a `ConnectionIO` program. */
    val wrap = λ[ConnectionIO ~> ConnectionIO] { ma =>
      (before *> ma <* after) onError oops guarantee always
    }

    /** Natural transformation that wraps a `ConnectionIO` stream. */
    val wrapP = λ[Stream[ConnectionIO, ?] ~> Stream[ConnectionIO, ?]] { pa =>
        (before.p ++ pa ++ after.p) onError { e => oops.p ++ eval_(delay(throw e)) } onFinalize always
    }

    /**
     * Natural transformation that wraps a `Kleisli` program, using the provided interpreter to
     * interpret the `before`/`after`/`oops`/`always` strategy.
     */
    def wrapK[M[_]: MonadError[?[_], Throwable]](interp: Interpreter[M]) =
      λ[Kleisli[M, Connection, ?] ~> Kleisli[M, Connection, ?]] { ka =>
        val beforeʹ = before foldMap interp
        val afterʹ  = after  foldMap interp
        val oopsʹ   = oops   foldMap interp
        val alwaysʹ = always foldMap interp
        (beforeʹ *> ka <* afterʹ) onError oopsʹ guarantee alwaysʹ
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

    /**
     * A no-op `Strategy`. All actions simply return `()`.
     * @group Constructors
     */
    val void = Strategy(unit, unit, unit, unit)

  }

  /**
   * A thin wrapper around a source of database connections, an interpreter, and a strategy for
   * running programs, parameterized over a target monad `M` and an arbitrary wrapped value `A`.
   * Given a stream or program in `ConnectionIO` or a program in `Kleisli`, a `Transactor` can
   * discharge the doobie machinery and yield an effectful stream or program in `M`.
   * @tparam M a target effect type; typically `IO`
   * @group Data Types
   */
  sealed abstract class Transactor[M[_]] { self =>
    /** An arbitrary value that will be handed back to `connect` **/
    type A

    /** An arbitrary value, meaningful to the instance **/
    def kernel: A

    /** A program in `M` that can provide a database connection, given the kernel **/
    def connect: A => M[Connection]

    /** A natural transformation for interpreting `ConnectionIO` **/
    def interpret: Interpreter[M]

    /** A `Strategy` for running a program on a connection **/
    def strategy: Strategy

    /** Construct a [[Yolo]] for REPL experimentation. */
    def yolo(implicit ev1: Sync[M]): Yolo[M] = new Yolo(this)

    /**
     * Construct a program to peform arbitrary configuration on the kernel value (changing the
     * timeout on a connection pool, for example). This can be the basis for constructing a
     * configuration language for a specific kernel type `A`, whose operations can be added to
     * compatible `Transactor`s via implicit conversion.
     * @group Configuration
     */
    def configure[B](f: A => B)(implicit ev: Sync[M]): M[B] =
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
    def exec(implicit ev: MonadError[M, Throwable]): Kleisli[M, Connection, ?] ~> M =
      strategy.wrapK(interpret) andThen rawExec(ev)

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

    def rawTransP(implicit ev: Effect[M]) = λ[Stream[ConnectionIO, ?] ~> Stream[M, ?]] { pa =>
      // TODO: this can almost certainly be simplified

      // Natural transformation by Kleisli application.
      def applyKleisli[F[_], E](e: E) =
        λ[Kleisli[F, E, ?] ~> F](_.run(e))

      // Lift a natural translation over an functor to one over its free monad.
      def liftF[F[_], G[_]: Monad](nat: F ~> G) =
        λ[Free[F, ?] ~> G](_.foldMap(nat))

      def nat(c: Connection): ConnectionIO ~> M =
        liftF(interpret andThen applyKleisli(c))

      eval(connect(kernel)).flatMap(c => pa.translate[M](nat(c)))

     }

    def transP(implicit ev: Effect[M]): Stream[ConnectionIO, ?] ~> Stream[M, ?] =
      strategy.wrapP andThen rawTransP

    def copy(
      kernel0: A = self.kernel,
      connect0: A => M[Connection] = self.connect,
      interpret0: Interpreter[M] = self.interpret,
      strategy0: Strategy = self.strategy
    ): Transactor.Aux[M, A] = new Transactor[M] {
      type A = self.A
      val kernel = kernel0
      val connect = connect0
      val interpret = interpret0
      val strategy = strategy0
    }
  }

  object Transactor {

    def apply[M[_], A0](
      kernel0: A0,
      connect0: A0 => M[Connection],
      interpret0: Interpreter[M],
      strategy0: Strategy
    ): Transactor.Aux[M, A0] = new Transactor[M] {
      type A = A0
      val kernel = kernel0
      val connect = connect0
      val interpret = interpret0
      val strategy = strategy0
    }

    type Aux[M[_], A0] = Transactor[M] { type A = A0  }

    /** @group Lenses */ def kernel   [M[_], A]: Transactor.Aux[M, A] Lens A                    = Lens(_.kernel,    (a, b) => a.copy(kernel0    = b))
    /** @group Lenses */ def connect  [M[_], A]: Transactor.Aux[M, A] Lens (A => M[Connection]) = Lens(_.connect,   (a, b) => a.copy(connect0   = b))
    /** @group Lenses */ def interpret[M[_]]: Transactor[M] Lens Interpreter[M]       = Lens(_.interpret, (a, b) => a.copy(interpret0 = b))
    /** @group Lenses */ def strategy [M[_]]: Transactor[M] Lens Strategy             = Lens(_.strategy,  (a, b) => a.copy(strategy0  = b))
    /** @group Lenses */ def before   [M[_]]: Transactor[M] Lens ConnectionIO[Unit]   = strategy[M] >=> Strategy.before
    /** @group Lenses */ def after    [M[_]]: Transactor[M] Lens ConnectionIO[Unit]   = strategy[M] >=> Strategy.after
    /** @group Lenses */ def oops     [M[_]]: Transactor[M] Lens ConnectionIO[Unit]   = strategy[M] >=> Strategy.oops
    /** @group Lenses */ def always   [M[_]]: Transactor[M] Lens ConnectionIO[Unit]   = strategy[M] >=> Strategy.always

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
        def apply[A <: DataSource](a: A)(implicit ev: Async[M]): Transactor.Aux[M, A] =
          Transactor(a, a => ev.delay(a.getConnection), KleisliInterpreter[M].ConnectionInterpreter, Strategy.default)
      }
    }

    /** @group Constructors */
    def fromConnection[M[_]: Async](a: Connection): Transactor.Aux[M, Connection] =
      Transactor(a, _.pure[M], KleisliInterpreter[M].ConnectionInterpreter, Strategy.default.copy(always = unit))

    /**
     * Construct a constructor of `Transactor[M, Unit]` backed by the JDBC DriverManager by partial
     * application of `M`, which cannot be inferred in general. This follows the pattern described
     * [here](http://tpolecat.github.io/2015/07/30/infer.html).
     * @group Constructors
     */
    object fromDriverManager {

      private def create[M[_]: Async](driver: String, conn: => Connection): Transactor.Aux[M, Unit] =
        Transactor((), u => Sync[M].delay { Class.forName(driver); conn }, KleisliInterpreter[M].ConnectionInterpreter, Strategy.default)

      def apply[M[_]: Async](driver: String, url: String): Transactor.Aux[M, Unit] =
        create(driver, DriverManager.getConnection(url))

      def apply[M[_]: Async](driver: String, url: String, user: String, pass: String): Transactor.Aux[M, Unit] =
        create(driver, DriverManager.getConnection(url, user, pass))

      def apply[M[_]: Async](driver: String, url: String, info: java.util.Properties): Transactor.Aux[M, Unit] =
        create(driver, DriverManager.getConnection(url, info))

    }

  }


}
