// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.*
import cats.arrow.Profunctor
import cats.data.NonEmptyList
import doobie.free.connection.ConnectionIO
import doobie.free.preparedstatement.PreparedStatementIO
import doobie.free.resultset.ResultSetIO
import doobie.free.{connection as IFC, preparedstatement as IFPS}
import doobie.hi.connection.PreparedExecution
import doobie.hi.{connection as IHC, preparedstatement as IHPS, resultset as IHRS}
import doobie.util.MultiVersionTypeSupport.=:=
import doobie.util.analysis.Analysis
import doobie.util.compat.FactoryCompat
import doobie.util.fragment.Fragment
import doobie.util.log.{LoggingInfo, Parameters}
import doobie.util.pos.Pos
import fs2.Stream

/** Module defining queries parameterized by input and output types. */
object query {

  val DefaultChunkSize = 512

  /** A query parameterized by some input type `A` yielding values of type `B`. We define here the core operations that
    * are needed. Additional operations are provided on `[[Query0]]` which is the residual query after applying an `A`.
    * This is the type constructed by the `sql` interpolator.
    */
  trait Query[A, B] { outer =>

    protected implicit val write: Write[A]
    protected implicit val read: Read[B]

    /** The SQL string.
      * @group Diagnostics
      */
    def sql: String

    /** An optional `[[Pos]]` indicating the source location where this `[[Query]]` was constructed. This is used only
      * for diagnostic purposes.
      * @group Diagnostics
      */
    def pos: Option[Pos]

    /** Convert this Query to a `Fragment`. */
    def toFragment(a: A): Fragment =
      write.toFragment(a, sql)

    /** Label to be used during logging */
    val label: String

    /** Program to construct an analysis of this query's SQL statement and asserted parameter and column types.
      * @group Diagnostics
      */
    def analysis: ConnectionIO[Analysis] =
      IHC.prepareQueryAnalysis[A, B](sql)

    /** Program to construct an analysis of this query's SQL statement and result set column types.
      * @group Diagnostics
      */
    def outputAnalysis: ConnectionIO[Analysis] =
      IHC.prepareQueryAnalysis0[B](sql)

    /** Program to construct an inspection of the query. Given arguments `a`, calls `f` with the SQL representation of
      * the query and a statement with all arguments set. Returns the result of the `ConnectionIO` program constructed.
      *
      * @group Diagnostics
      */
    def inspect[R](a: A)(f: (String, PreparedStatementIO[Unit]) => ConnectionIO[R]): ConnectionIO[R] =
      f(sql, IHPS.set(a))

    /** Apply the argument `a` to construct a `Stream` with the given chunking factor, with effect type
      * `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding elements of type `B`.
      * @group Results
      */
    def streamWithChunkSize(a: A, chunkSize: Int): Stream[ConnectionIO, B] =
      IHC.stream(
        create = IFC.prepareStatement(sql),
        prep = IHPS.set(a),
        exec = IFPS.executeQuery,
        chunkSize = chunkSize,
        loggingInfo = LoggingInfo(
          sql = sql,
          params = Parameters.NonBatch(Write[A].toList(a)),
          label = label
        )
      )

    /** Apply the argument `a` to construct a `Stream` with `DefaultChunkSize`, with effect type
      * `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding elements of type `B`.
      * @group Results
      */
    def stream(a: A): Stream[ConnectionIO, B] =
      streamWithChunkSize(a, DefaultChunkSize)

    /** Apply the argument `a` to construct a program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding
      * an `F[B]` accumulated via the provided `CanBuildFrom`. This is the fastest way to accumulate a collection.
      * @group Results
      */
    def to[F[_]](a: A)(implicit f: FactoryCompat[B, F[B]]): ConnectionIO[F[B]] = {
      toConnectionIO(a, IHRS.build[F, B])
    }

    /** Like [[to]], but allow altering various parts of the execute steps before it is run
      * @tparam F
      *   The collection type e.g. List, Vector
      * @tparam R
      *   The result type (Changing PreparedExecution's process step may change the result type)
      */
    def toAlteringExecution[F[_], R](
        a: A,
        fn: PreparedExecution[F[B]] => PreparedExecution[R]
    )(implicit f: FactoryCompat[B, F[B]]): ConnectionIO[R] = {
      toConnectionIOAlteringExecution(a, IHRS.build[F, B], fn)
    }

    /** Apply the argument `a` to construct a program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding
      * an `Map[(K, V)]` accumulated via the provided `CanBuildFrom`. This is the fastest way to accumulate a
      * collection. this function can call only when B is (K, V).
      * @group Results
      */
    def toMap[K, V](a: A)(implicit ev: B =:= (K, V), f: FactoryCompat[(K, V), Map[K, V]]): ConnectionIO[Map[K, V]] =
      toConnectionIO(a, IHRS.buildPair[Map, K, V](f, read.map(ev)))

    /** Like [[toMap]], but allow altering various parts of the execute steps before it is run
      * @tparam K
      *   Type of the map's key
      * @tparam V
      *   Type of the map's values
      * @tparam R
      *   The result type (Changing PreparedExecution's process step may change the result type)
      */
    def toMapAlteringExecution[K, V, R](
        a: A,
        fn: PreparedExecution[Map[K, V]] => PreparedExecution[R]
    )(implicit
        ev: B =:= (K, V),
        f: FactoryCompat[(K, V), Map[K, V]]
    ): ConnectionIO[R] =
      toConnectionIOAlteringExecution(a, IHRS.buildPair[Map, K, V](f, read.map(ev)), fn)

    /** Apply the argument `a` to construct a program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding
      * an `F[B]` accumulated via `MonadPlus` append. This method is more general but less efficient than `to`.
      * @group Results
      */
    def accumulate[F[_]: Alternative](a: A): ConnectionIO[F[B]] =
      toConnectionIO(a, IHRS.accumulate[F, B])

    /** Like [[accumulate]], but allow altering various parts of the execute steps before it is run
      *
      * @tparam F
      *   The collection type e.g. List, Vector
      * @tparam R
      *   The result type (Changing PreparedExecution's process step may change the result type)
      */
    def accumulateAlteringExecution[F[_]: Alternative, R](
        a: A,
        fn: PreparedExecution[F[B]] => PreparedExecution[R]
    ): ConnectionIO[R] =
      toConnectionIOAlteringExecution(a, IHRS.accumulate[F, B], fn)

    /** Apply the argument `a` to construct a program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding
      * a unique `B` and raising an exception if the resultset does not have exactly one row. See also `option`.
      * @group Results
      */
    def unique(a: A): ConnectionIO[B] =
      toConnectionIO(a, IHRS.getUnique[B])

    /** Like [[unique]], but allow altering various parts of the execute steps before it is run
      *
      * @tparam R
      *   The result type (Changing PreparedExecution's process step may change the result type)
      */
    def uniqueAlteringExecution[R](a: A, fn: PreparedExecution[B] => PreparedExecution[R]): ConnectionIO[R] =
      toConnectionIOAlteringExecution(a, IHRS.getUnique[B], fn)

    /** Apply the argument `a` to construct a program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding
      * an optional `B` and raising an exception if the resultset has more than one row. See also `unique`.
      * @group Results
      */
    def option(a: A): ConnectionIO[Option[B]] =
      toConnectionIO(a, IHRS.getOption[B])

    /** Like [[option]], but allow altering various parts of the execute steps before it is run
      * @tparam R
      *   The result type (Changing PreparedExecution's process step may change the result type)
      */
    def optionAlteringExecution[R](
        a: A,
        fn: PreparedExecution[Option[B]] => PreparedExecution[R]
    ): ConnectionIO[R] =
      toConnectionIOAlteringExecution(a, IHRS.getOption[B], fn)

    /** Apply the argument `a` to construct a program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding
      * an `NonEmptyList[B]` and raising an exception if the resultset does not have at least one row. See also
      * `unique`.
      * @group Results
      */
    def nel(a: A): ConnectionIO[NonEmptyList[B]] =
      toConnectionIO(a, IHRS.nel[B])

    /** Like [[nel]], but allow altering various parts of the execute steps before it is run
      * @tparam R
      *   The result type (Changing PreparedExecution's process step may change the result type)
      */
    def nelAlteringExecution[R](
        a: A,
        fn: PreparedExecution[NonEmptyList[B]] => PreparedExecution[R]
    ): ConnectionIO[R] =
      toConnectionIOAlteringExecution(a, IHRS.nel[B], fn)

    private def toConnectionIO[C](a: A, rsio: ResultSetIO[C]): ConnectionIO[C] =
      IHC.executeWithResultSet(preparedExecution(sql, a, rsio), mkLoggingInfo(a))

    private def toConnectionIOAlteringExecution[C, R](
        a: A,
        rsio: ResultSetIO[C],
        fn: PreparedExecution[C] => PreparedExecution[R]
    ): ConnectionIO[R] =
      IHC.executeWithResultSet(fn(preparedExecution(sql, a, rsio)), mkLoggingInfo(a))

    private def preparedExecution[C](sql: String, a: A, rsio: ResultSetIO[C]): PreparedExecution[C] =
      PreparedExecution(
        create = IFC.prepareStatement(sql),
        prep = IHPS.set(a),
        exec = IFPS.executeQuery,
        process = rsio
      )

    private def mkLoggingInfo(a: A): LoggingInfo =
      LoggingInfo(
        sql = sql,
        params = Parameters.NonBatch(write.toList(a)),
        label = label
      )

    /** @group Transformations */
    def map[C](f: B => C): Query[A, C] =
      new Query[A, C] {
        val write: Write[A] = outer.write
        val read: Read[C] = outer.read.map(f)
        def sql: String = outer.sql
        def pos: Option[Pos] = outer.pos
        val label: String = outer.label
      }

    /** @group Transformations */
    def contramap[C](f: C => A): Query[C, B] =
      new Query[C, B] {
        val write: Write[C] = outer.write.contramap(f)
        val read: Read[B] = outer.read
        def sql: String = outer.sql
        def pos: Option[Pos] = outer.pos
        val label: String = outer.label
      }

    /** Apply an argument, yielding a residual `[[Query0]]`.
      * @group Transformations
      */
    def toQuery0(a: A): Query0[B] =
      new Query0[B] {
        override def sql: String = outer.sql
        override def pos: Option[Pos] = outer.pos
        override def toFragment: Fragment = outer.toFragment(a)
        override def analysis: ConnectionIO[Analysis] = outer.analysis
        override def outputAnalysis: ConnectionIO[Analysis] = outer.outputAnalysis
        override def streamWithChunkSize(n: Int): Stream[ConnectionIO, B] = outer.streamWithChunkSize(a, n)
        override def accumulate[F[_]: Alternative]: ConnectionIO[F[B]] = outer.accumulate[F](a)
        override def accumulateAlteringExecution[F[_]: Alternative, R](
            fn: PreparedExecution[F[B]] => PreparedExecution[R]
        ): ConnectionIO[R] = outer.accumulateAlteringExecution(a, fn)
        override def to[F[_]](implicit f: FactoryCompat[B, F[B]]): ConnectionIO[F[B]] = outer.to[F](a)
        override def toAlteringExecution[F[_], R](fn: PreparedExecution[F[B]] => PreparedExecution[R])(implicit
            f: FactoryCompat[B, F[B]]
        ): ConnectionIO[R] =
          outer.toAlteringExecution(a, fn)
        override def toMap[K, V](implicit
            ev: B =:= (K, V),
            f: FactoryCompat[(K, V), Map[K, V]]
        ): ConnectionIO[Map[K, V]] = outer.toMap(a)

        override def toMapAlteringExecution[K, V, R](fn: PreparedExecution[Map[K, V]] => PreparedExecution[R])(
            implicit
            ev: B =:= (K, V),
            f: FactoryCompat[(K, V), Map[K, V]]
        ): ConnectionIO[R] = outer.toMapAlteringExecution(a, fn)
        override def unique: ConnectionIO[B] = outer.unique(a)
        override def uniqueAlteringExecution[R](fn: PreparedExecution[B] => PreparedExecution[R]): ConnectionIO[R] =
          outer.uniqueAlteringExecution(a, fn)
        override def option: ConnectionIO[Option[B]] = outer.option(a)
        override def optionAlteringExecution[R](fn: PreparedExecution[Option[B]] => PreparedExecution[R])
            : ConnectionIO[R] =
          outer.optionAlteringExecution(a, fn)
        override def nel: ConnectionIO[NonEmptyList[B]] = outer.nel(a)
        override def nelAlteringExecution[R](
            fn: PreparedExecution[NonEmptyList[B]] => PreparedExecution[R]
        ): ConnectionIO[R] =
          outer.nelAlteringExecution(a, fn)
        override def map[C](f: B => C): Query0[C] = outer.map(f).toQuery0(a)
        override def inspect[R](f: (String, PreparedStatementIO[Unit]) => ConnectionIO[R]): ConnectionIO[R] =
          outer.inspect(a)(f)
      }

  }

  object Query {

    /** Construct a `Query` with the given SQL string, an optional `Pos` for diagnostic purposes, and type arguments for
      * writable input and readable output types. Note that the most common way to construct a `Query` is via the `sql`
      * interpolator.
      * @group Constructors
      */
    def apply[A, B](sql: String, pos: Option[Pos] = None, label: String = unlabeled)(implicit
        A: Write[A],
        B: Read[B]
    ): Query[A, B] = {
      val sql0 = sql
      val label0 = label
      val pos0 = pos
      new Query[A, B] {
        val write: Write[A] = A
        val read: Read[B] = B
        val sql: String = sql0
        val pos: Option[Pos] = pos0
        val label: String = label0
      }
    }

    /** @group Typeclass Instances */
    implicit val queryProfunctor: Profunctor[Query] =
      new Profunctor[Query] {
        def dimap[A, B, C, D](fab: Query[A, B])(f: C => A)(g: B => D): Query[C, D] =
          fab.contramap(f).map(g)
      }

    /** @group Typeclass Instances */
    implicit def queryCovariant[A]: Functor[Query[A, *]] =
      new Functor[Query[A, *]] {
        def map[B, C](fa: Query[A, B])(f: B => C): Query[A, C] =
          fa.map(f)
      }

    /** @group Typeclass Instances */
    implicit def queryContravariant[B]: Contravariant[Query[*, B]] =
      new Contravariant[Query[*, B]] {
        def contramap[A, C](fa: Query[A, B])(f: C => A): Query[C, B] =
          fa.contramap(f)
      }

  }

  /** An abstract query closed over its input arguments and yielding values of type `B`, without a specified
    * disposition. Methods provided on `[[Query0]]` allow the query to be interpreted as a stream or program in
    * `CollectionIO`.
    */
  trait Query0[B] { outer =>

    /** The SQL string.
      * @group Diagnostics
      */
    def sql: String

    /** An optional `Pos` indicating the source location where this `Query` was constructed. This is used only for
      * diagnostic purposes.
      * @group Diagnostics
      */
    def pos: Option[Pos]

    /** Program to construct an analysis of this query's SQL statement and asserted parameter and column types.
      * @group Diagnostics
      */
    def analysis: ConnectionIO[Analysis]

    /** Convert this Query0 to a `Fragment`. */
    def toFragment: Fragment

    /** Program to construct an inspection of the query. Calls `f` with the SQL representation of the query and a
      * statement with all statement arguments set. Returns the result of the `ConnectionIO` program constructed.
      *
      * @group Diagnostics
      */
    def inspect[R](f: (String, PreparedStatementIO[Unit]) => ConnectionIO[R]): ConnectionIO[R]

    /** Program to construct an analysis of this query's SQL statement and result set column types.
      * @group Diagnostics
      */
    def outputAnalysis: ConnectionIO[Analysis]

    /** `Stream` with default chunk factor, with effect type `[[doobie.free.connection.ConnectionIO ConnectionIO]]`
      * yielding elements of type `B`.
      * @group Results
      */
    def stream: Stream[ConnectionIO, B] =
      streamWithChunkSize(DefaultChunkSize)

    /** `Stream` with given chunk factor, with effect type `[[doobie.free.connection.ConnectionIO ConnectionIO]]`
      * yielding elements of type `B`.
      * @group Results
      */
    def streamWithChunkSize(n: Int): Stream[ConnectionIO, B]

    /** Program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding an `F[B]` accumulated via the
      * provided `CanBuildFrom`. This is the fastest way to accumulate a collection.
      * @group Results
      */
    def to[F[_]](implicit f: FactoryCompat[B, F[B]]): ConnectionIO[F[B]]

    /** Like [[to]], but allow altering various parts of the execute steps before it is run
      * @tparam F
      *   The collection type e.g. List, Vector
      * @tparam R
      *   The result type (Changing PreparedExecution's process step may change the result type)
      */
    def toAlteringExecution[F[_], R](
        fn: PreparedExecution[F[B]] => PreparedExecution[R]
    )(implicit f: FactoryCompat[B, F[B]]): ConnectionIO[R]

    /** Apply the argument `a` to construct a program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding
      * an `Map[(K, V)]` accumulated via the provided `CanBuildFrom`. This is the fastest way to accumulate a
      * collection. this function can call only when B is (K, V).
      * @group Results
      */
    def toMap[K, V](implicit ev: B =:= (K, V), f: FactoryCompat[(K, V), Map[K, V]]): ConnectionIO[Map[K, V]]

    /** Like [[toMap]], but allow altering various parts of the execute steps before it is run
      * @tparam R
      *   The result type (Changing PreparedExecution's process step may change the result type)
      */
    def toMapAlteringExecution[K, V, R](
        fn: PreparedExecution[Map[K, V]] => PreparedExecution[R]
    )(implicit
        ev: B =:= (K, V),
        f: FactoryCompat[(K, V), Map[K, V]]
    ): ConnectionIO[R]

    /** Program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding an `F[B]` accumulated via `MonadPlus`
      * append. This method is more general but less efficient than `to`.
      * @group Results
      */
    def accumulate[F[_]: Alternative]: ConnectionIO[F[B]]

    /** Like [[to]], but allow altering various parts of the execute steps before it is run
      * @tparam R
      *   The result type (Changing PreparedExecution's process step may change the result type)
      */
    def accumulateAlteringExecution[F[_]: Alternative, R](
        fn: PreparedExecution[F[B]] => PreparedExecution[R]
    ): ConnectionIO[R]

    /** Program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding a unique `B` and raising an exception
      * if the resultset does not have exactly one row. See also `option`.
      * @group Results
      */
    def unique: ConnectionIO[B]

    /** Like [[unique]], but allow altering various parts of the execute steps before it is run
      * @tparam R
      *   The result type (Changing PreparedExecution's process step may change the result type)
      */
    def uniqueAlteringExecution[R](fn: PreparedExecution[B] => PreparedExecution[R]): ConnectionIO[R]

    /** Program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding an optional `B` and raising an
      * exception if the resultset has more than one row. See also `unique`.
      * @group Results
      */
    def option: ConnectionIO[Option[B]]

    /** Like [[option]], but allow altering various parts of the execute steps before it is run
      * @tparam R
      *   The result type (Changing PreparedExecution's process step may change the result type)
      */
    def optionAlteringExecution[R](
        fn: PreparedExecution[Option[B]] => PreparedExecution[R]
    ): ConnectionIO[R]

    /** Program in `[[doobie.free.connection.ConnectionIO ConnectionIO]]` yielding a `NonEmptyList[B]` and raising an
      * exception if the resultset does not have at least one row. See also `unique`.
      * @group Results
      */
    def nel: ConnectionIO[NonEmptyList[B]]

    /** Like [[nel]], but allow altering various parts of the execute steps before it is run
      * @tparam R
      *   The result type (Changing PreparedExecution's process step may change the result type)
      */
    def nelAlteringExecution[R](
        fn: PreparedExecution[NonEmptyList[B]] => PreparedExecution[R]
    ): ConnectionIO[R]

    /** @group Transformations */
    def map[C](f: B => C): Query0[C]

  }

  object Query0 {

    /** Construct a `Query` with the given SQL string, an optional `Pos` for diagnostic purposes, with no parameters.
      * Note that the most common way to construct a `Query` is via the `sql`interpolator.
      * @group Constructors
      */
    def apply[A: Read](sql: String, pos: Option[Pos] = None, label: String = unlabeled): Query0[A] =
      Query[Unit, A](sql, pos, label).toQuery0(())

    /** @group Typeclass Instances */
    implicit val queryFunctor: Functor[Query0] =
      new Functor[Query0] {
        def map[A, B](fa: Query0[A])(f: A => B): Query0[B] = fa `map` f
      }

  }

}
