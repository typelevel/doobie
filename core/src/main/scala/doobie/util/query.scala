package doobie.util

import doobie.hi.{ ConnectionIO, PreparedStatementIO }
import doobie.hi.connection.{ prepareStatement, prepareQueryAnalysis, prepareQueryAnalysis0, process => cprocess }
import doobie.hi.preparedstatement.{ set, executeQuery }
import doobie.hi.resultset.{ getUnique }
import doobie.util.composite.Composite
import doobie.util.analysis.Analysis
import doobie.syntax.process._

import scalaz.{ Profunctor, Contravariant, Functor, Monad }
import scalaz.syntax.monad._
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

    def list(a: A): ConnectionIO[List[B]] =
      process(a).list

    def vector(a: A): ConnectionIO[Vector[B]] =
      process(a).vector

    def sink(a: A)(f: B => ConnectionIO[Unit]): ConnectionIO[Unit] =
      process(a).sink(f)

    def unique(a: A): ConnectionIO[B]

    def map[C](f: B => C): Query[A, C] =
      new Query[A, C] {
        val sql = q.sql
        val stackFrame = q.stackFrame
        val analysis = q.analysis
        def process(a: A) = q.process(a).map(f)
        def unique(a: A) = q.unique(a).map(f)
      }

    def contramap[C](f: C => A): Query[C, B] =
      new Query[C, B] {
        val sql = q.sql
        val stackFrame = q.stackFrame
        val analysis = q.analysis
        def process(c: C) = q.process(f(c))
        def unique(c: C) = q.unique(f(c))
      }

    def toQuery0(a: A): Query0[B] =
      new Query0[B] {
        val sql = q.sql
        val stackFrame = q.stackFrame
        val analysis = q.analysis
        def process = q.process(a)
        def unique = q.unique(a)
    }

  }

  object Query {

    /** Construct a `Query` for the given parameter and output types. */
    def apply[A: Composite, B: Composite](sql0: String, stackFrame0: Option[StackTraceElement]): Query[A, B] =
      new Query[A, B] {
        val sql = sql0
        val stackFrame = stackFrame0
        val analysis = prepareQueryAnalysis[A,B](sql)
        def process(a: A) = cprocess[B](sql, set(a))
        def unique(a: A) = prepareStatement(sql)(set(a) >> executeQuery(getUnique[B]))
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

    def list: ConnectionIO[List[B]] =
      process.list

    def vector: ConnectionIO[Vector[B]] =
      process.vector

    def sink(f: B => ConnectionIO[Unit]): ConnectionIO[Unit] =
      process.sink(f)

    def unique: ConnectionIO[B]

    def map[C](f: B => C): Query0[C] =
      new Query0[C] {
        val sql = q.sql
        val stackFrame = q.stackFrame
        def analysis = q.analysis
        def process = q.process.map(f)
        def unique = q.unique.map(f)
      }

  }

  object Query0 {

    /** Construct a `Query0` for the given parameter and output types. */
    def apply[B: Composite](sql0: String, stackFrame0: Option[StackTraceElement]): Query0[B] =
      new Query0[B] {
        val sql = sql0
        val stackFrame = stackFrame0
        def analysis = prepareQueryAnalysis0[B](sql)
        def process = cprocess[B](sql, Monad[PreparedStatementIO].point(()))
        def unique = prepareStatement(sql)(executeQuery(getUnique[B]))
      }

    implicit val query0Covariant: Functor[Query0] =
      new Functor[Query0] {
        def map[A, B](fa: Query0[A])(f: A => B) = fa map f
      }

  }

}