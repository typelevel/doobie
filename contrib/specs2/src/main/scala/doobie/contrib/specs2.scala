package doobie.contrib

import doobie.util.transactor._
import doobie.util.query._
import doobie.util.update._
import doobie.util.analysis._
import doobie.util.pretty._

import org.specs2.mutable.Specification
import org.specs2.execute.Failure

import scala.reflect.runtime.universe.TypeTag

import scalaz.concurrent.Task
import scalaz._, Scalaz._

object specs2 {

  trait AnalysisSpec { this: Specification =>

    val transactor: Transactor[Task]


    def check[A, B](q: Query[A, B])(implicit A: TypeTag[A], B: TypeTag[B]) =
      checkD(s"Query[${typeName(A)}, ${typeName(B)}]", q)

    def check[A](q: Query0[A])(implicit A: TypeTag[A]) =
      checkD(s"Query0[${typeName(A)}]", q)

    def check[A](q: Update[A])(implicit A: TypeTag[A]) =
      checkU(s"Update[${typeName(A)}]", q)

    def check[A](q: Update0)(implicit A: TypeTag[A]) =
      checkU(s"Update0", q)





    private def checkD(typeName: String, q: QueryDiagnostics) =
      s"$typeName defined at ${loc(q.stackFrame)}\n${q.sql.lines.map(s => "  " + s.trim).filterNot(_.isEmpty).mkString("\n")}" >> {
        transactor.transact(q.analysis).attemptRun match {
          case -\/(e) => "SQL Compiles and Typechecks" in failure(formatError(e.getMessage))
          case \/-(a) => 
            "SQL Compiles and Typechecks" in ok
            examplesBlock { 
              a.paramDescriptions.foreach  { case (s, es) => s in assertEmpty(es, q.stackFrame) }
              a.columnDescriptions.foreach { case (s, es) => s in assertEmpty(es, q.stackFrame) }
            }
        }        
      }

    private def checkU(typeName: String, q: UpdateDiagnostics) = 
      s"$typeName defined at ${loc(q.stackFrame)}\n${q.sql.lines.map(s => "  " + s.trim).filterNot(_.isEmpty).mkString("\n")}" >> {
        transactor.transact(q.analysis).attemptRun match {
          case -\/(e) => "SQL Compiles and Typechecks" in failure(formatError(e.getMessage))
          case \/-(a) => 
            "SQL Compiles and Typechecks" in ok
            // TODO: verify that this is not a select statement; there should be no output columns
            examplesBlock { 
              a.paramDescriptions.foreach  { case (s, es) => s in assertEmpty(es, q.stackFrame) }
              a.columnDescriptions.foreach { case (s, es) => s in assertEmpty(es, q.stackFrame) }
            }
        }        
      }

    private def loc(f: Option[StackTraceElement]): String = 
      f.map(f => s"${f.getFileName}:${f.getLineNumber}").getOrElse("(source location unknown)")

    private def assertEmpty(es: List[AlignmentError], f: Option[StackTraceElement]) = 
      if (es.isEmpty) success 
      else new Failure(es.map(formatError).mkString("\n"), "", f.toList)

    private val packagePrefix = "\\b[a-z]+\\.".r

    private def typeName[A](tag: TypeTag[A]): String =
      packagePrefix.replaceAllIn(tag.tpe.toString, "")

    private def formatError(e: AlignmentError): String =
      formatError(e.msg)

    private def formatError(s: String): String =
      (wrap(100)(s) match {
        case s :: ss => (s"x " + s) :: ss.map("  " + _)
        case Nil => Nil
      }).mkString("\n")

}

}



