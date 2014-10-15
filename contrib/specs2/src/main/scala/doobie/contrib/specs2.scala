package doobie.contrib

import doobie.util.transactor._
import doobie.util.query._
import doobie.util.analysis._
import doobie.util.pretty._

import org.specs2.mutable.Specification

import scalaz.concurrent.Task

object specs2 {

  trait AnalysisSpec { this: Specification =>

    val transactor: Transactor[Task]

    private def assertEmpty(es: List[AlignmentError]) = 
      if (es.isEmpty) success else failure(es.flatMap(e => wrap(100)(e.msg)).mkString("\n"))

    implicit class AnalysisSpecStringOps(s: String) {
      def is[A](q: Query0[A]) = {
        val a = transactor.transact(q.analysis).run
        val title = if (a.alignmentErrors.isEmpty) s else s + "\n\n" + a.header + "\n\n"
        title should {
          "have correct input arity"   in assertEmpty(a.parameterMisalignments)
          "have well-typed parameters" in assertEmpty(a.parameterTypeErrors)
          "have correct column arity"  in assertEmpty(a.columnMisalignments)
          "have well-typed columns"    in assertEmpty(a.columnTypeErrors ++ a.columnTypeWarnings)
          "have correct NULL mappings" in assertEmpty(a.nullabilityMisalignments)
        }
      }
    }

  }

}