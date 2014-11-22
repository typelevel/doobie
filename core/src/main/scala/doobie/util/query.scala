package doobie.util

import doobie.hi._
import doobie.util.composite.Composite
import doobie.util.analysis.Analysis
import doobie.syntax.catchable._
import doobie.syntax.process._
import doobie.enum.jdbctype.JdbcType
import doobie.enum.parameternullable._
import doobie.hi.connection.{ delay => cdelay }

import scalaz._, Scalaz._
import scalaz.stream.Process

/** Module defining queries parameterized by input and output types. */
object query {

  /** Mixin trait for queries with diagnostic information. */
  trait QueryDiagnostics {
    val sql: String
    val stackFrame: Option[StackTraceElement]
    def analysis: ConnectionIO[Analysis]
  }

  /** Encapsulates a `ConnectionIO` that prepares and executes a parameterized statement. */
  trait Query[A, B] extends QueryDiagnostics { q =>

    def process(a: A): Process[ConnectionIO, B]

    def map[C](f: B => C): Query[A, C] =
      new Query[A, C] {
        val sql = q.sql
        val stackFrame = q.stackFrame
        val analysis = q.analysis
        def process(a: A) = q.process(a).map(f)
      }

    def contramap[C](f: C => A): Query[C, B] =
      new Query[C, B] {
        val sql = q.sql
        val stackFrame = q.stackFrame
        val analysis = q.analysis
        def process(c: C) = q.process(f(c))
      }

    def toQuery0(a: A): Query0[B] =
      new Query0[B] {
        val sql = q.sql
        val stackFrame = q.stackFrame
        val analysis = q.analysis
        def process = q.process(a)
    }

  }

  object Query {

    /** Construct a `Query` for the given parameter and output types. */
    def apply[A: Composite, B: Composite](sql0: String, stackFrame0: Option[StackTraceElement]): Query[A, B] =
      new Query[A, B] {
        val sql = sql0
        val stackFrame = stackFrame0
        val analysis = connection.prepareQueryAnalysis[A,B](sql)
        def process(a: A) = connection.process[B](sql, preparedstatement.set(a))
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
  trait Query0[B] extends QueryDiagnostics { q =>

    def process: Process[ConnectionIO, B] 

    def map[C](f: B => C): Query0[C] =
      new Query0[C] {
        val sql = q.sql
        val stackFrame = q.stackFrame
        def analysis = q.analysis
        def process = q.process.map(f)
      }

  }

  object Query0 {

    /** Construct a `Query0` for the given parameter and output types. */
    def apply[B: Composite](sql0: String, stackFrame0: Option[StackTraceElement]): Query0[B] =
      new Query0[B] {
        val sql = sql0
        val stackFrame = stackFrame0
        def analysis = connection.prepareQueryAnalysis0[B](sql)
        def process = connection.process[B](sql, Monad[PreparedStatementIO].point(()))
      }

    implicit val query0Covariant: Functor[Query0] =
      new Functor[Query0] {
        def map[A, B](fa: Query0[A])(f: A => B) = fa map f
      }

  }

}