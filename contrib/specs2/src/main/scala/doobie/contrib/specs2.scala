package doobie.contrib

import doobie.util.transactor._
import doobie.util.query._
import doobie.util.analysis._
import doobie.util.pretty._

import org.specs2.mutable.Specification
import org.specs2.execute.Failure

import scalaz.concurrent.Task
import scalaz._, Scalaz._

object specs2 {

  trait AnalysisSpec { this: Specification =>

    val transactor: Transactor[Task]

    def check[A](q: Query0[A])(implicit ev: Manifest[A]) = {

      def formatError(e: AlignmentError): String =
        (wrap(100)(e.msg) match {
          case s :: ss => ("* " + s) :: ss.map("  " + _)
          case Nil => Nil
        }).mkString("\n")

      def assertEmpty(es: List[AlignmentError]) = 
        if (es.isEmpty) success 
        else new Failure(es.map(formatError).mkString("\n"), "", q.stackFrame.toList)

      val loc = q.stackFrame.map(f => s"${f.getFileName}:${f.getLineNumber}").getOrElse("(source location unknown)")

      s"Query0[${ev.runtimeClass.getSimpleName}] defined at $loc\n${q.sql.lines.map(s => "  " + s.trim).filterNot(_.isEmpty).mkString("\n")}" >> {
        transactor.transact(q.analysis).attemptRun match {
          case -\/(e) => "SQL Compiles and Typechecks" in failure(e.getMessage)
          case \/-(a) => 
            "SQL Compiles and Typechecks" in ok
            examplesBlock { 
              a.paramDescriptions.foreach { case (s, es) => s in assertEmpty(es) }
              a.columnDescriptions.foreach { case (s, es) => s in assertEmpty(es) }
            }
        }        
      }
    }



  }
}