// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package util

import cats._, cats.effect._
import cats.implicits._
import cats.{ Alternative, Contravariant, Functor }
import cats.arrow.Profunctor
import cats.data.NonEmptyList

import doobie.implicits._
import doobie.util.analysis.Analysis
import doobie.util.log.{ LogEvent, ExecFailure, ProcessingFailure, Success }
import doobie.util.pos.Pos

import scala.collection.generic.CanBuildFrom
import scala.Predef.longWrapper
import scala.concurrent.duration.{ FiniteDuration, NANOSECONDS }

import java.sql.ResultSet


import fs2.Stream

/** Module defining queries parameterized by input and output types. */
object query {

  val DefaultChunkSize = 512

  /**
   * A query parameterized by some input type `A` yielding values of type `B`. We define here the
   * core operations that are needed. Additional operations are provided on `[[Query0]]` which is the
   * residual query after applying an `A`. This is the type constructed by the `sql` interpolator.
   */
  trait Query[A, B] { outer =>

    // jiggery pokery to support CBF; we're doing the coyoneda trick on B to avoid a Functor
    // constraint on the `F` parameter in `to`, and it's just easier to do the contravariant coyo
    // trick on A while we're at it.
    protected type I
    protected type O
    protected val ai: A => I
    protected val ob: O => B
    protected implicit val ic: Write[I]
    protected implicit val oc: Read[O]

    // LogHandler is protected for now.
    protected val logHandler: LogHandler

    private val now: PreparedStatementIO[Long] = FPS.delay(System.nanoTime)
    private def fail[T](t: Throwable): PreparedStatementIO[T] = FPS.delay(throw t)

    // Equivalent to HPS.executeQuery(k) but with logging
    private def executeQuery[T](a: A, k: ResultSetIO[T]): PreparedStatementIO[T] = {
      // N.B. the .attempt syntax isn't working in cats. unclear why
      val c = Predef.implicitly[Sync[PreparedStatementIO]]
      val args = ic.toList(ai(a))
      def diff(a: Long, b: Long) = FiniteDuration((a - b).abs, NANOSECONDS)
      def log(e: LogEvent) = FPS.delay(logHandler.unsafeRun(e))
      for {
        t0 <- now
        er <- c.attempt(FPS.executeQuery)
        t1 <- now
        rs <- er match {
                case Left(e) => log(ExecFailure(sql, args, diff(t1, t0), e)) *> fail[ResultSet](e)
                case Right(a) => a.pure[PreparedStatementIO]
              }
        et <- c.attempt(FPS.embed(rs, k guarantee FRS.close))
        t2 <- now
        t  <- et match {
                case Left(e) => log(ProcessingFailure(sql, args, diff(t1, t0), diff(t2, t1), e)) *> fail(e)
                case Right(a) => a.pure[PreparedStatementIO]
              }
        _  <- log(Success(sql, args, diff(t1, t0), diff(t2, t1)))
      } yield t
    }

    /**
     * The SQL string.
     * @group Diagnostics
     */
    def sql: String

    /**
     * An optional `[[Pos]]` indicating the source location where this `[[Query]]` was
     * constructed. This is used only for diagnostic purposes.
     * @group Diagnostics
     */
    def pos: Option[Pos]

    /** Turn this `Query` into a `Fragment`, given an argument. */
    def toFragment(a: A): Fragment =
      Fragment(sql, ai(a), pos)

    /**
     * Program to construct an analysis of this query's SQL statement and asserted parameter and
     * column types.
     * @group Diagnostics
     */
    def analysis: ConnectionIO[Analysis] =
      HC.prepareQueryAnalysis[I, O](sql)
    /**
     * Program to construct an analysis of this query's SQL statement and result set column types.
     * @group Diagnostics
     */
    def outputAnalysis: ConnectionIO[Analysis] =
      HC.prepareQueryAnalysis0[O](sql)

    /** @group Deprecated Methods */
    @deprecated("use .streamWithChunkSize", "0.5.0")
    def processWithChunkSize(a: A, chunkSize: Int): Stream[ConnectionIO, B] =
      streamWithChunkSize(a, chunkSize)

    /**
     * Apply the argument `a` to construct a `Stream` with the given chunking factor, with
     * effect type  `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding elements of
     * type `B`.
     * @group Results
     */
    def streamWithChunkSize(a: A, chunkSize: Int): Stream[ConnectionIO, B] =
      HC.stream[O](sql, HPS.set(ai(a)), chunkSize).map(ob)

    /** @group Deprecated Methods */
    @deprecated("use .stream", "0.5.0")
    def process(a: A): Stream[ConnectionIO, B] =
      stream(a)

    /**
     * Apply the argument `a` to construct a `Stream` with `DefaultChunkSize`, with
     * effect type  `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding elements of
     * type `B`.
     * @group Results
     */
    def stream(a: A): Stream[ConnectionIO, B] =
      streamWithChunkSize(a, DefaultChunkSize)


    /**
     * Apply the argument `a` to construct a program in
     *`[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding an `F[B]` accumulated
     * via the provided `CanBuildFrom`. This is the fastest way to accumulate a collection.
     * @group Results
     */
    def to[F[_]](a: A)(implicit cbf: CanBuildFrom[Nothing, B, F[B]]): ConnectionIO[F[B]] =
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> executeQuery(a, HRS.buildMap[F,O,B](ob)))

    /**
     * Apply the argument `a` to construct a program in
     * `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding an `F[B]` accumulated
     * via `MonadPlus` append. This method is more general but less efficient than `to`.
     * @group Results
     */
    def accumulate[F[_]: Alternative](a: A): ConnectionIO[F[B]] =
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> executeQuery(a, HRS.accumulate[F, O].map(_.map(ob))))

    /**
     * Apply the argument `a` to construct a program in
     * `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding a unique `B` and
     * raising an exception if the resultset does not have exactly one row. See also `option`.
     * @group Results
     */
    def unique(a: A): ConnectionIO[B] =
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> executeQuery(a, HRS.getUnique[O])).map(ob)

    /**
     * Apply the argument `a` to construct a program in
     * `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding an optional `B` and
     * raising an exception if the resultset has more than one row. See also `unique`.
     * @group Results
     */
    def option(a: A): ConnectionIO[Option[B]] =
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> executeQuery(a, HRS.getOption[O])).map(_.map(ob))

    /**
      * Apply the argument `a` to construct a program in
      * `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding an `NonEmptyList[B]` and
      * raising an exception if the resultset does not have at least one row. See also `unique`.
      * @group Results
      */
    def nel(a: A): ConnectionIO[NonEmptyList[B]] =
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> executeQuery(a, HRS.nel[O])).map(_.map(ob))

    /** @group Deprecated Methods */
    @deprecated("use .to[List]", "0.5.0")
    def list(a: A): ConnectionIO[List[B]] = to[List](a)

    /** @group Deprecated Methods */
    @deprecated("use .to[Vector]", "0.5.0")
    def vector(a: A): ConnectionIO[Vector[B]] = to[Vector](a)

    /** @group Transformations */
    def map[C](f: B => C): Query[A, C] =
      new Query[A, C] {
        type I = outer.I
        type O = outer.O
        val ai = outer.ai
        val ob = outer.ob andThen f
        val ic: Write[I] = outer.ic
        val oc: Read[O] = outer.oc
        def sql = outer.sql
        def pos = outer.pos
        val logHandler = outer.logHandler
      }

    /** @group Transformations */
    def contramap[C](f: C => A): Query[C, B] =
      new Query[C, B] {
        type I = outer.I
        type O = outer.O
        val ai = outer.ai compose f
        val ob = outer.ob
        val ic: Write[I] = outer.ic
        val oc: Read[O] = outer.oc
        def sql = outer.sql
        def pos = outer.pos
        val logHandler = outer.logHandler
      }

    /**
     * Apply an argument, yielding a residual `[[Query0]]`.
     * @group Transformations
     */
    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def toQuery0(a: A): Query0[B] =
      new Query0[B] {
        def sql = outer.sql
        def pos = outer.pos
        def toFragment = outer.toFragment(a)
        def analysis = outer.analysis
        def outputAnalysis = outer.outputAnalysis
        def streamWithChunkSize(n: Int) = outer.streamWithChunkSize(a, n)
        def accumulate[F[_]: Alternative] = outer.accumulate[F](a)
        def to[F[_]](implicit cbf: CanBuildFrom[Nothing, B, F[B]]) = outer.to[F](a)
        def unique = outer.unique(a)
        def option = outer.option(a)
        def nel = outer.nel(a)
        def map[C](f: B => C): Query0[C] = outer.map(f).toQuery0(a)
      }

  }

  object Query {

    /**
     * Construct a `Query` with the given SQL string, an optional `Pos` for diagnostic
     * purposes, and type arguments for writable input and readable output types. Note that the
     * most common way to construct a `Query` is via the `sql` interpolator.
     * @group Constructors
     */
    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    def apply[A, B](sql0: String, pos0: Option[Pos] = None, logHandler0: LogHandler = LogHandler.nop)(implicit A: Write[A], B: Read[B]): Query[A, B] =
      new Query[A, B] {
        type I = A
        type O = B
        val ai: A => I = a => a
        val ob: O => B = o => o
        implicit val ic: Write[I] = A
        implicit val oc: Read[O] = B
        val sql = sql0
        val pos = pos0
        val logHandler = logHandler0
      }

    /** @group Typeclass Instances */
    implicit val queryProfunctor: Profunctor[Query] =
      new Profunctor[Query] {
        def dimap[A, B, C, D](fab: Query[A,B])(f: C => A)(g: B => D): Query[C,D] =
          fab.contramap(f).map(g)
      }

    /** @group Typeclass Instances */
    implicit def queryCovariant[A]: Functor[Query[A, ?]] =
      new Functor[Query[A, ?]] {
        def map[B, C](fa: Query[A, B])(f: B => C): Query[A, C] =
          fa.map(f)
      }

    /** @group Typeclass Instances */
    implicit def queryContravariant[B]: Contravariant[Query[?, B]] =
      new Contravariant[Query[?, B]] {
        def contramap[A, C](fa: Query[A, B])(f: C => A): Query[C, B] =
          fa.contramap(f)
      }

  }

  /**
   * An abstract query closed over its input arguments and yielding values of type `B`, without a
   * specified disposition. Methods provided on `[[Query0]]` allow the query to be interpreted as a
   * stream or program in `CollectionIO`.
   */
  trait Query0[B] { outer =>

    /**
     * The SQL string.
     * @group Diagnostics
     */
    def sql: String

    /**
     * An optional `Pos` indicating the source location where this `Query` was
     * constructed. This is used only for diagnostic purposes.
     * @group Diagnostics
     */
    def pos: Option[Pos]

    /** Turn this `Query0` into a `Fragment`. */
    def toFragment: Fragment

    /**
     * Program to construct an analysis of this query's SQL statement and asserted parameter and
     * column types.
     * @group Diagnostics
     */
    def analysis: ConnectionIO[Analysis]

    /**
     * Program to construct an analysis of this query's SQL statement and result set column types.
     * @group Diagnostics
     */
    def outputAnalysis: ConnectionIO[Analysis]

    /** @group Deprecated Methods */
    @deprecated("use .stream", "0.5.0")
    def process: Stream[ConnectionIO, B] =
      stream

    /**
     * `Stream` with default chunk factor, with effect type
     * `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding  elements of type `B`.
     * @group Results
     */
    def stream : Stream[ConnectionIO, B] =
      streamWithChunkSize(DefaultChunkSize)

    /** @group Deprecated Methods */
    @deprecated("use .streamWithChunkSize", "0.5.0")
    def processWithChunkSize(n: Int): Stream[ConnectionIO, B] =
      streamWithChunkSize(n)

    /**
     * `Stream` with given chunk factor, with effect type
     * `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding  elements of type `B`.
     * @group Results
     */
    def streamWithChunkSize(n: Int): Stream[ConnectionIO, B]

    /**
     * Program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding an `F[B]`
     * accumulated via the provided `CanBuildFrom`. This is the fastest way to accumulate a
     * collection.
     * @group Results
     */
    def to[F[_]](implicit cbf: CanBuildFrom[Nothing, B, F[B]]): ConnectionIO[F[B]]

    /**
     * Program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding an `F[B]`
     * accumulated via `MonadPlus` append. This method is more general but less efficient than `to`.
     * @group Results
     */
    def accumulate[F[_]: Alternative]: ConnectionIO[F[B]]

    /**
     * Program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding a unique `B` and
     * raising an exception if the resultset does not have exactly one row. See also `option`.
     * @group Results
     */
    def unique: ConnectionIO[B]

    /**
     * Program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding an optional `B`
     * and raising an exception if the resultset has more than one row. See also `unique`.
     * @group Results
     */
    def option: ConnectionIO[Option[B]]

    /**
      * Program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding a `NonEmptyList[B]`
      * and raising an exception if the resultset does not have at least one row. See also `unique`.
      * @group Results
      */
    def nel: ConnectionIO[NonEmptyList[B]]

    /** @group Transformations */
    def map[C](f: B => C): Query0[C]

    /**
     * Convenience method; equivalent to `process.sink(f)`
     * @group Results
     */
    def sink(f: B => ConnectionIO[Unit]): ConnectionIO[Unit] =
      stream.evalMap(f).compile.drain

    /** @group Deprecated Methods */
    @deprecated("use .to[List]", "0.5.0")
    def list: ConnectionIO[List[B]] = to[List]

    /** @group Deprecated Methods */
    @deprecated("use .to[Vector]", "0.5.0")
    def vector: ConnectionIO[Vector[B]] = to[Vector]

  }

  object Query0 {

    /**
     * Construct a `Query` with the given SQL string, an optional `Pos` for diagnostic
     * purposes, with no parameters. Note that the most common way to construct a `Query` is via the
     * `sql`interpolator.
     * @group Constructors
     */
     @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
     def apply[A: Read](sql: String, pos: Option[Pos] = None, logHandler: LogHandler = LogHandler.nop): Query0[A] =
       Query[Unit, A](sql, pos, logHandler).toQuery0(())

    /** @group Typeclass Instances */
    implicit val queryFunctor: Functor[Query0] =
      new Functor[Query0] {
        def map[A, B](fa: Query0[A])(f: A => B) = fa map f
      }

  }

}
