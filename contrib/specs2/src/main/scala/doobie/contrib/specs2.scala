package doobie.contrib

import doobie.util.transactor._
import doobie.util.query._
import doobie.util.update._
import doobie.util.analysis._
import doobie.util.pretty._

import org.specs2.mutable.Specification
import org.specs2.execute.Failure

import scalaz.concurrent.Task
import scalaz._, Scalaz._

object specs2 {

  trait AnalysisSpec { this: Specification =>

    val transactor: Transactor[Task]

    def check[A, B](q: Query[A, B])(implicit A: Manifest[A], B: Manifest[B]) =
      checkD(s"Query[${A.runtimeClass.getSimpleName}, ${B.runtimeClass.getSimpleName}]", q)

    def check[A](q: Query0[A])(implicit A: Manifest[A]) =
      checkD(s"Query0[${A.runtimeClass.getSimpleName}]", q)

    def check[A](q: Update[A])(implicit A: Manifest[A]) =
      checkU(s"Update[${A.runtimeClass.getSimpleName}]", q)

    def check[A](q: Update0)(implicit A: Manifest[A]) =
      checkU(s"Update0", q)

    private def checkD(typeName: String, q: QueryDiagnostics) = {

      def formatError(e: AlignmentError): String =
        (wrap(100)(e.msg) match {
          case s :: ss => ("* " + s) :: ss.map("  " + _)
          case Nil => Nil
        }).mkString("\n")

      def assertEmpty(es: List[AlignmentError]) = 
        if (es.isEmpty) success 
        else new Failure(es.map(formatError).mkString("\n"), "", q.stackFrame.toList)

      val loc = q.stackFrame.map(f => s"${f.getFileName}:${f.getLineNumber}").getOrElse("(source location unknown)")

      s"$typeName defined at $loc\n${q.sql.lines.map(s => "  " + s.trim).filterNot(_.isEmpty).mkString("\n")}" >> {
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


    private def checkU(typeName: String, q: UpdateDiagnostics) = {

      def formatError(e: AlignmentError): String =
        (wrap(100)(e.msg) match {
          case s :: ss => ("* " + s) :: ss.map("  " + _)
          case Nil => Nil
        }).mkString("\n")

      def assertEmpty(es: List[AlignmentError]) = 
        if (es.isEmpty) success 
        else new Failure(es.map(formatError).mkString("\n"), "", q.stackFrame.toList)

      val loc = q.stackFrame.map(f => s"${f.getFileName}:${f.getLineNumber}").getOrElse("(source location unknown)")

      s"$typeName defined at $loc\n${q.sql.lines.map(s => "  " + s.trim).filterNot(_.isEmpty).mkString("\n")}" >> {
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