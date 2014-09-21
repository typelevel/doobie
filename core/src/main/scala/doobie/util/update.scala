package doobie.util

import doobie.hi.ConnectionIO
import doobie.hi.connection.prepareStatement
import doobie.hi.preparedstatement.{ set, executeUpdate }
import doobie.util.invariant.MappingViolation
import doobie.util.composite.Composite
import doobie.util.analysis.Analysis

import scalaz.{ Contravariant, Functor, Profunctor, ValidationNel }
import scalaz.stream.Process
import scalaz.syntax.monad._

object update {

  trait Update[A] { u =>

    def analysis: ConnectionIO[Analysis] 

    def run(a: A): ConnectionIO[Int]

    def contramap[C](f: C => A): Update[C] =
      new Update[C] {
        def analysis: ConnectionIO[Analysis] = u.analysis
        def run(c: C) = u.run(f(c))
      }

    def toUpdate0(a: A): Update0 =
      new Update0 {
        def analysis = u.analysis
        def run = u.run(a)
      }

  }

  object Update {

    def apply[A: Composite](sql: String): Update[A] =
      new Update[A] {
        def analysis: ConnectionIO[Analysis] = Predef.???
        def run(a: A) = prepareStatement(sql)(set(a) >> executeUpdate)
      }

    implicit val updateContravariant: Contravariant[Update] =
      new Contravariant[Update] {
        def contramap[A, B](fa: Update[A])(f: B => A) = fa contramap f
      }

  }

  trait Update0 {
    def run: ConnectionIO[Int]
  }

  object Update0 {

    def apply(sql: String): Update0 =
      new Update0 {
        def analysis: ConnectionIO[Analysis] = Predef.???
        def run = prepareStatement(sql)(executeUpdate)
      }

  }

}