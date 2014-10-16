package doobie.util

import doobie.hi._
import doobie.util.composite.Composite
import doobie.util.analysis.Analysis
import doobie.syntax.catchable._
import doobie.enum.jdbctype.JdbcType
import doobie.enum.parameternullable._
import doobie.hi.connection.{ prepareStatementAnalysis, delay => cdelay }

import scalaz._, Scalaz._
import scalaz.stream.Process

object query {

  /** Encapsulates a `ConnectionIO` that prepares and executes a parameterized statement. */
  trait Query[A, B] { q =>

    val sql: String

    /** Stack frame where defined, for diagnostic purposes. */
    val stackFrame: Option[StackTraceElement]

    def analysis: ConnectionIO[Analysis]

    def run(a: A): Process[ConnectionIO, B] 

    def map[C](f: B => C): Query[A, C] =
      new Query[A, C] {
        val sql = q.sql
        val stackFrame = q.stackFrame
        val analysis = q.analysis
        def run(a: A) = q.run(a).map(f)
      }

    def contramap[C](f: C => A): Query[C, B] =
      new Query[C, B] {
        val sql = q.sql
        val stackFrame = q.stackFrame
        val analysis = q.analysis
        def run(c: C) = q.run(f(c))
      }

    def toQuery0(a: A): Query0[B] =
      new Query0[B] {
        val sql = q.sql
        val stackFrame = q.stackFrame
        val analysis = q.analysis
        def run = q.run(a)
      }

  }

  object Query {

    /** Construct a `Query` for the given parameter and output types. */
    def apply[A: Composite, B: Composite](sql0: String, stackFrame0: Option[StackTraceElement]): Query[A, B] =
      new Query[A, B] {
        val sql = sql0
        val stackFrame = stackFrame0
        val analysis = connection.prepareStatementAnalysis[A,B](sql)
        def run(a: A) = connection.process[B](sql, preparedstatement.set(a))
      }

    implicit val queryProfunctor: Profunctor[Query] =
      new Profunctor[Query] {
        def mapfst[A, B, C](fab: Query[A,B])(f: C => A) = fab contramap f
        def mapsnd[A, B, C](fab: Query[A,B])(f: B => C) = fab map f
      }

    implicit def queryCovariant[A]: Functor[({ type l[b] = Query[A, b]})#l] =
      queryProfunctor.covariantInstance[A]

    implicit def queryContravariant[B]: Contravariant[({ type l[a] = Query[a, B]})#l] =
      queryProfunctor.contravariantInstance[B]

  }

  /** Encapsulates a `ConnectionIO` that prepares and executes a zero-parameter statement. */
  trait Query0[B] { q =>

    val sql: String

    val stackFrame: Option[StackTraceElement]

    def analysis: ConnectionIO[Analysis]

    def run: Process[ConnectionIO, B] 

    def map[C](f: B => C): Query0[C] =
      new Query0[C] {
        val sql = q.sql
        val stackFrame = q.stackFrame
        def analysis = q.analysis
        def run = q.run.map(f)
      }

  }

  object Query0 {

    /** Construct a `Query0` for the given parameter and output types. */
    def apply[B: Composite](sql0: String, stackFrame0: Option[StackTraceElement]): Query0[B] =
      new Query0[B] {
        val sql = sql0
        val stackFrame = stackFrame0
        def analysis = Predef.???
        def run = connection.process[B](sql, Monad[PreparedStatementIO].point(()))
      }

    implicit val query0Covariant: Functor[Query0] =
      new Functor[Query0] {
        def map[A, B](fa: Query0[A])(f: A => B) = fa map f
      }

  }

}