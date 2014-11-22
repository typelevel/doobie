package doobie.util

import doobie.hi.ConnectionIO
import doobie.hi.connection.{ prepareStatement, prepareUpdateAnalysis, prepareUpdateAnalysis0 }
import doobie.hi.preparedstatement.{ set, executeUpdate }
import doobie.util.invariant.MappingViolation
import doobie.util.composite.Composite
import doobie.util.analysis.Analysis

import scalaz.{ Contravariant, Functor, Profunctor, ValidationNel }
import scalaz.stream.Process
import scalaz.syntax.monad._

/** Module defining updates parameterized by input type. */
object update {

  /** Mixin trait for queries with diagnostic information. */
  trait UpdateDiagnostics {
    val sql: String
    val stackFrame: Option[StackTraceElement]
    def analysis: ConnectionIO[Analysis]
  }

  trait Update[A] extends UpdateDiagnostics { u =>

    def run(a: A): ConnectionIO[Int]

    def contramap[C](f: C => A): Update[C] =
      new Update[C] {
        val sql = u.sql
        val stackFrame = u.stackFrame
        def analysis: ConnectionIO[Analysis] = u.analysis
        def run(c: C) = u.run(f(c))
      }

    def toUpdate0(a: A): Update0 =
      new Update0 {
        val sql = u.sql
        val stackFrame = u.stackFrame
        def analysis = u.analysis
        def run = u.run(a)
      }

  }

  object Update {

    def apply[A: Composite](sql0: String, stackFrame0: Option[StackTraceElement]): Update[A] =
      new Update[A] {
        val sql = sql0
        val stackFrame = stackFrame0
        def analysis: ConnectionIO[Analysis] = prepareUpdateAnalysis[A](sql)
        def run(a: A) = prepareStatement(sql)(set(a) >> executeUpdate)
      }

    implicit val updateContravariant: Contravariant[Update] =
      new Contravariant[Update] {
        def contramap[A, B](fa: Update[A])(f: B => A) = fa contramap f
      }

  }

  trait Update0 extends UpdateDiagnostics {
    def run: ConnectionIO[Int]
  }

  object Update0 {

    def apply(sql0: String, stackFrame0: Option[StackTraceElement]): Update0 =
      new Update0 {
        val sql = sql0
        val stackFrame = stackFrame0
        def analysis: ConnectionIO[Analysis] = prepareUpdateAnalysis0(sql)
        def run = prepareStatement(sql)(executeUpdate)
      }

  }

}