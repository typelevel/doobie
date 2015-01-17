package doobie.util

          
import doobie.free.connection.{ ConnectionIO, setAutoCommit, commit, rollback, close, delay }
import doobie.hi.connection.ProcessConnectionIOOps
import doobie.syntax.catchable._
import doobie.syntax.process._
import doobie.syntax.connectionio._
import doobie.util.analysis._
import doobie.util.capture._
import doobie.util.query._
import doobie.util.update._
import doobie.util.transactor._
import doobie.util.pretty.wrap

import scala.reflect.runtime.universe.TypeTag

import scalaz._,Scalaz._
import scalaz.stream.Process
import scalaz.stream.Process. { eval, eval_, halt }

import java.sql.Connection
import Predef._

/** Module for implicit syntax useful in REPL session. */
object yolo {

  class Yolo[M[_]: Monad: Catchable: Capture](xa: Transactor[M]) {

    private def out(s: String): ConnectionIO[Unit] =
      delay(Console.println(s"${Console.BLUE}  $s${Console.RESET}"))
    
    implicit class Query0YoloOps[A: TypeTag](q: Query0[A]) {
      
      def quick: M[Unit] = 
        q.sink(a => out(a.toString)).transact(xa)

      def check: M[Unit] = 
        (delay(showSql(q.sql)) >> q.analysis.attempt.flatMap {
          case -\/(e) => delay(failure("SQL Compiles and Typechecks", formatError(e.getMessage)))
          case \/-(a) => delay {
            success("SQL Compiles and Typechecks", None)
            a.paramDescriptions.foreach  { case (s, es) => assertEmpty(s, es) }
            a.columnDescriptions.foreach { case (s, es) => assertEmpty(s, es) }
          }
        }).transact(xa)

    }
    
    implicit class Update0YoloOps(u: Update0) {
      def quick: M[Unit] = u.run.flatMap(a => out(s"$a row(s) updated")).transact(xa)
    }
    
    implicit class ConnectionIOYoloOps[A](ca: ConnectionIO[A]) {
      def quick: M[Unit] = ca.flatMap(a => out(a.toString)).transact(xa)
    }

    implicit class ProcessYoloOps[A](pa: Process[ConnectionIO, A]) {
      def quick: M[Unit] = pa.sink(a => out(a.toString)).transact(xa)
    }
  
    private def assertEmpty(name: String, es: List[AlignmentError]) = 
      if (es.isEmpty) success(name, None)
      else failure(name, es.map(formatError).mkString("\n"))

    private val packagePrefix = "\\b[a-z]+\\.".r

    private def typeName[A](tag: TypeTag[A]): String =
      packagePrefix.replaceAllIn(tag.tpe.toString, "")

    private def formatError(e: AlignmentError): String =
      formatError(e.msg)

    private def formatError(s: String): String =
      (wrap(80)(s) match {
        case s :: ss => (s"${Console.RED}  - $s${Console.RESET}") :: ss.map(s => s"${Console.RED}    $s${Console.RESET}")
        case Nil => Nil
      }).mkString("\n")

    def showSql(sql: String): Unit = {
      println()
      sql.lines.dropWhile(_.trim.isEmpty).foreach(s => println(s"  \033[37m$s${Console.RESET}"))
      println()
    }

    def failure(name: String, desc: String): Unit = {
      println(s"${Console.RED}  ✕ ${Console.RESET}$name")
      desc.lines.foreach(s => println(s"  $s"))
    }

    def success(name: String, desc: Option[String]): Unit = {
      println(s"${Console.GREEN}  ✓ ${Console.RESET}" + name)
      desc.foreach(println)
    }

  }
}