package doobie.util

import doobie.hi._
import doobie.util.invariant.MappingViolation
import doobie.util.composite.Composite

import scalaz.{ Contravariant, Functor, Monad, Profunctor, ValidationNel }
import scalaz.stream.Process

object query {

  /** Encapsulates a `ConnectionIO` that prepares and executes a parameterized statement. */
  trait Query[A, B] { q =>

    def sql: String
    def check: ConnectionIO[ValidationNel[MappingViolation, Unit]]
    def run(a: A): Process[ConnectionIO, B] 

    def map[C](f: B => C): Query[A, C] =
      new Query[A, C] {
        val sql = q.sql
        def run(a: A) = q.run(a).map(f)
        def check = q.check
      }

    def contramap[C](f: C => A): Query[C, B] =
      new Query[C, B] {
        val sql = q.sql
        def run(c: C) = q.run(f(c))
        def check = q.check
      }

    def toQuery0(a: A): Query0[B] =
      new Query0[B] {
        val sql = q.sql
        def run = q.run(a)
        def check = q.check
      }

  }

  object Query {

    /** Construct a `Query` for the given parameter and output types. */
    def apply[A: Composite, B: Composite](sql0: String): Query[A, B] =
      new Query[A, B] {
        def sql = sql0
        def run(a: A) = connection.process[B](sql, preparedstatement.set(a))
        def check = Predef.??? /// TODO
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

    def sql: String
    def check: ConnectionIO[ValidationNel[MappingViolation, Unit]]
    def run(): Process[ConnectionIO, B] 

    def map[C](f: B => C): Query0[C] =
      new Query0[C] {
        val sql = q.sql
        def run = q.run.map(f)
        def check = q.check
      }

  }

  object Query0 {

    /** Construct a `Query0` for the given parameter and output types. */
    def apply[B: Composite](sql0: String): Query0[B] =
      new Query0[B] {
        def sql = sql0
        def run() = connection.process[B](sql, Monad[PreparedStatementIO].point(()))
        def check = Predef.??? /// TODO
      }

    implicit val query0Covariant: Functor[Query0] =
      new Functor[Query0] {
        def map[A, B](fa: Query0[A])(f: A => B) = fa map f
      }

  }

}