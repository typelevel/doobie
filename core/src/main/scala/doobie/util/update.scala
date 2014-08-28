package doobie.util

import doobie.hi.ConnectionIO
import doobie.hi.connection.prepareStatement
import doobie.hi.preparedstatement.{ set, executeUpdate }
import doobie.util.invariant.MappingViolation
import doobie.util.composite.Composite
import doobie.util.prepared.Prepared

import scalaz.{ Contravariant, Functor, Profunctor, ValidationNel }
import scalaz.stream.Process
import scalaz.syntax.monad._

object update {

  trait Update[A] extends Prepared { u =>

    def run(a: A): ConnectionIO[Int]

    def contramap[C](f: C => A): Update[C] =
      new Update[C] {
        val sql = u.sql
        def run(c: C) = u.run(f(c))
        def check = u.check
      }

    def toUpdate0(a: A): Update0 =
      new Update0 {
        def sql = u.sql
        def run = u.run(a)
        def check = u.check
      }

  }

  object Update {

    def apply[A: Composite](sql0: String): Update[A] =
      new Update[A] {
        def sql = sql0
        def run(a: A) = prepareStatement(sql)(set(a) >> executeUpdate)
        def check = Predef.??? /// TODO
      }

    implicit val updateContravariant: Contravariant[Update] =
      new Contravariant[Update] {
        def contramap[A, B](fa: Update[A])(f: B => A) = fa contramap f
      }

  }

  trait Update0 extends Prepared {
    def run: ConnectionIO[Int]
  }

  object Update0 {

    def apply(sql0: String): Update0 =
      new Update0 {
        def sql = sql0
        def run = prepareStatement(sql)(executeUpdate)
        def check = Predef.??? /// TODO
      }

  }

}