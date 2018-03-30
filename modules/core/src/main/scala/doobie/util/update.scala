// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats._
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.free.{ preparedstatement => FPS }
import doobie.free.preparedstatement.PreparedStatementIO
import doobie.hi.{ connection => HC }
import doobie.hi.{ preparedstatement => HPS }
import doobie.util.analysis.Analysis
import doobie.util.composite.Composite
import doobie.util.log._
import doobie.util.pos.Pos
import doobie.util.fragment.Fragment
import fs2.Stream
import scala.Predef.longWrapper
import scala.concurrent.duration.{ FiniteDuration, NANOSECONDS }

/** Module defining updates parameterized by input type. */
object update {

  import doobie.free.preparedstatement.AsyncPreparedStatementIO // we need this instance ... TODO: re-org

  val DefaultChunkSize = query.DefaultChunkSize

  /**
   * Partial application hack to allow calling updateManyWithGeneratedKeys without passing the
   * F[_] type argument explicitly.
   */
  trait UpdateManyWithGeneratedKeysPartiallyApplied[A, K] {
    def apply[F[_]](as: F[A])(implicit F: Foldable[F], K: Composite[K]): Stream[ConnectionIO, K] =
      withChunkSize(as, DefaultChunkSize)
    def withChunkSize[F[_]](as: F[A], chunkSize: Int)(implicit F: Foldable[F], K: Composite[K]): Stream[ConnectionIO, K]
  }

  /**
   * An update parameterized by some input type `A`. This is the type constructed by the `sql`
   * interpolator.
   */
  trait Update[A] { u =>

    // Contravariant coyoneda trick for A
    protected type I
    protected val ai: A => I
    protected implicit val ic: Composite[I]

    // LogHandler is protected for now.
    protected val logHandler: LogHandler

    private val now: PreparedStatementIO[Long] = FPS.delay(System.nanoTime)
    private def fail[T](t: Throwable): PreparedStatementIO[T] = FPS.delay(throw t)

    // Equivalent to HPS.executeUpdate(k) but with logging if logHandler is defined
    private def executeUpdate[T](a: A): PreparedStatementIO[Int] = {
      // N.B. the .attempt syntax isn't working in cats. unclear why
      val args = ic.toList(ai(a))
      val c = Predef.implicitly[MonadError[PreparedStatementIO, Throwable]]
      def diff(a: Long, b: Long) = FiniteDuration((a - b).abs, NANOSECONDS)
      def log(e: LogEvent) = FPS.delay(logHandler.unsafeRun(e))
      for {
        t0 <- now
        en <- c.attempt(FPS.executeUpdate)
        t1 <- now
        n  <- en match {
                case Left(e)  => log(ExecFailure(sql, args, diff(t1, t0), e)) *> fail[Int](e)
                case Right(a) => a.pure[PreparedStatementIO]
              }
        _  <- log(Success(sql, args, diff(t1, t0), FiniteDuration(0L, NANOSECONDS)))
      } yield n
    }


    /**
     * The SQL string.
     * @group Diagnostics
     */
    val sql: String

    /**
     * An optional `[[Pos]]` indicating the source location where this `[[Update]]` was
     * constructed. This is used only for diagnostic purposes.
     * @group Diagnostics
     */
    val pos: Option[Pos]

    /** Turn this `Update` into a `Fragment`, given an argument. */
    def toFragment(a: A): Fragment =
      Fragment(sql, ai(a), pos)

    /**
     * Program to construct an analysis of this query's SQL statement and asserted parameter types.
     * @group Diagnostics
     */
    def analysis: ConnectionIO[Analysis] =
      HC.prepareUpdateAnalysis[I](sql)

    /**
     * Construct a program to execute the update and yield a count of affected rows, given the
     * composite argument `a`.
     * @group Execution
     */
    def run(a: A): ConnectionIO[Int] =
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> executeUpdate(a))

    /**
     * Program to execute a batch update and yield a count of affected rows.
     * @group Execution
     */
    def updateMany[F[_]: Foldable](fa: F[A]): ConnectionIO[Int] =
      HC.prepareStatement(sql)(HPS.addBatchesAndExecute(fa.toList.map(ai)))

    /**
     * Construct a stream that performs a batch update as with `updateMany`, yielding generated
     * keys of composite type `K`, identified by the specified columns. Note that not all drivers
     * support generated keys, and some support only a single key column.
     * @group Execution
     */
    def updateManyWithGeneratedKeys[K](columns: String*): UpdateManyWithGeneratedKeysPartiallyApplied[A, K] =
      new UpdateManyWithGeneratedKeysPartiallyApplied[A, K] {
        def withChunkSize[F[_]](as: F[A], chunkSize: Int)(implicit F: Foldable[F], K: Composite[K]): Stream[ConnectionIO, K] =
          HC.updateManyWithGeneratedKeys[List,I,K](columns.toList)(sql, ().pure[PreparedStatementIO], as.toList.map(ai), chunkSize)
      }

    /**
     * Construct a stream that performs the update, yielding generated keys of composite type `K`,
     * identified by the specified columns, given a composite argument `a`. Note that not all
     * drivers support generated keys, and some support only a single key column.
     * @group Execution
     */
    def withGeneratedKeys[K: Composite](columns: String*)(a: A): Stream[ConnectionIO, K] =
      withGeneratedKeysWithChunkSize[K](columns: _*)(a, DefaultChunkSize)

    /**
     * Construct a stream that performs the update, yielding generated keys of composite type `K`,
     * identified by the specified columns, given a composite argument `a` and `chunkSize`. Note
     * that not all drivers support generated keys, and some support only a single key column.
     * @group Execution
     */
    def withGeneratedKeysWithChunkSize[K: Composite](columns: String*)(a: A, chunkSize: Int): Stream[ConnectionIO, K] =
      HC.updateWithGeneratedKeys[K](columns.toList)(sql, HPS.set(ai(a)), chunkSize)

    /**
     * Construct a program that performs the update, yielding a single set of generated keys of
     * composite type `K`, identified by the specified columns, given a composite argument `a`.
     * Note that not all drivers support generated keys, and some support only a single key column.
     * @group Execution
     */
    def withUniqueGeneratedKeys[K: Composite](columns: String*)(a: A): ConnectionIO[K] =
      HC.prepareStatementS(sql, columns.toList)(HPS.set(ai(a)) *> HPS.executeUpdateWithUniqueGeneratedKeys)

    /**
     * Update is a contravariant functor.
     * @group Transformations
     */
    def contramap[C](f: C => A): Update[C] =
      new Update[C] {
        type I  = u.I
        val ai  = u.ai compose f
        val ic  = u.ic
        val sql = u.sql
        val pos = u.pos
        val logHandler = u.logHandler
      }

    /**
     * Apply an argument, yielding a residual `[[Update0]]`.
     * @group Transformations
     */
    def toUpdate0(a: A): Update0 =
      new Update0 {
        val sql = u.sql
        val pos = u.pos
        def toFragment = u.toFragment(a)
        def analysis = u.analysis
        def run = u.run(a)
        def withGeneratedKeysWithChunkSize[K: Composite](columns: String*)(chunkSize: Int) =
          u.withGeneratedKeysWithChunkSize[K](columns: _*)(a, chunkSize)
        def withUniqueGeneratedKeys[K: Composite](columns: String*) =
          u.withUniqueGeneratedKeys(columns: _*)(a)
      }

  }

  object Update {

    /**
     * Construct an `Update` for some composite parameter type `A` with the given SQL string, and
     * optionally a `Pos` and/or `LogHandler` for diagnostics. The normal mechanism
     * for construction is the `sql/fr/fr0` interpolators.
     * @group Constructors
     */
    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    def apply[A](sql0: String, pos0: Option[Pos] = None, logHandler0: LogHandler = LogHandler.nop)(
      implicit C: Composite[A]
    ): Update[A] =
      new Update[A] {
        type I  = A
        val ai  = (a: A) => a
        val ic  = C
        val sql = sql0
        val logHandler = logHandler0
        val pos = pos0
      }

    /**
     * Update is a contravariant functor.
     * @group Typeclass Instances
     */
    implicit val updateContravariant: Contravariant[Update] =
      new Contravariant[Update] {
        def contramap[A, B](fa: Update[A])(f: B => A) = fa contramap f
      }

  }

  trait Update0 {

    /**
     * The SQL string.
     * @group Diagnostics
     */
    val sql: String

    /**
     * An optional `[[Pos]]` indicating the source location where this `[[Query]]` was
     * constructed. This is used only for diagnostic purposes.
     * @group Diagnostics
     */
    val pos: Option[Pos]

    /** Turn this `Update`0 into a `Fragment`. */
    def toFragment: Fragment

    /**
     * Program to construct an analysis of this query's SQL statement and asserted parameter types.
     * @group Diagnostics
     */
    def analysis: ConnectionIO[Analysis]

    /**
     * Program to execute the update and yield a count of affected rows.
     * @group Execution
     */
    def run: ConnectionIO[Int]

    /**
     * Construct a stream that performs the update, yielding generated keys of composite type `K`,
     * identified by the specified columns. Note that not all drivers support generated keys, and
     * some support only a single key column.
     * @group Execution
     */
    def withGeneratedKeys[K: Composite](columns: String*): Stream[ConnectionIO, K] =
      withGeneratedKeysWithChunkSize(columns: _*)(DefaultChunkSize)

    /**
     * Construct a stream that performs the update, yielding generated keys of composite type `K`,
     * identified by the specified columns, given a `chunkSize`. Note that not all drivers support
     * generated keys, and some support only a single key column.
     * @group Execution
     */
    def withGeneratedKeysWithChunkSize[K: Composite](columns: String*)(chunkSize:Int): Stream[ConnectionIO, K]

    /**
     * Construct a program that performs the update, yielding a single set of generated keys of
     * composite type `K`, identified by the specified columns. Note that not all drivers support
     * generated keys, and some support only a single key column.
     * @group Execution
     */
    def withUniqueGeneratedKeys[K: Composite](columns: String*): ConnectionIO[K]

  }

  object Update0 {

    /**
     * Construct an `Update0` with the given SQL string, and optionally a `Pos`
     * and/or `LogHandler` for diagnostics. The normal mechanism for construction is the
     * `sql/fr/fr0` interpolators.
     * @group Constructors
     */
    def apply(sql0: String, pos0: Option[Pos]): Update0 =
      Update[Unit](sql0, pos0).toUpdate0(())

  }

}
