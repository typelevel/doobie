// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.reflect.runtime.universe.TypeTag

import cats.effect._
import cats.instances.int._
import cats.instances.string._
import cats.syntax.show._
import doobie.free.connection.{ ConnectionIO, delay }
import doobie.syntax.connectionio._
import doobie.util.query._
import doobie.util.update._
import doobie.util.testing._
import doobie.util.transactor._
import fs2.Stream
import scala.Predef._

/** Module for implicit syntax useful in REPL session. */
@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
object yolo {

  import doobie.free.connection.AsyncConnectionIO

  class Yolo[M[_]: Sync](xa: Transactor[M]) {

    private def out(s: String, colors: Colors): ConnectionIO[Unit] =
      delay(Console.println(show"${colors.BLUE}  $s${colors.RESET}"))

    implicit class Query0YoloOps[A: TypeTag](q: Query0[A]) {

      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      def quick(implicit colors: Colors = Colors.Ansi): M[Unit] =
        q.stream
         .map(_.toString)
         .evalMap(out(_, colors))
         .compile
         .drain
         .transact(xa)

      def check(implicit colors: Colors = Colors.Ansi): M[Unit] =
        checkImpl(Analyzable.unpack(q), colors)

      def checkOutput(implicit colors: Colors = Colors.Ansi): M[Unit] =
        checkImpl(AnalysisArgs(
          show"Query0[${typeName[A]}]", q.pos, q.sql, q.outputAnalysis
        ), colors)
    }

    implicit class QueryYoloOps[I: TypeTag, A: TypeTag](q: Query[I,A]) {

      def quick(i: I): M[Unit] =
        q.toQuery0(i).quick

      def check(implicit colors: Colors = Colors.Ansi): M[Unit] =
        checkImpl(Analyzable.unpack(q), colors)

      def checkOutput(implicit colors: Colors = Colors.Ansi): M[Unit] =
        checkImpl(AnalysisArgs(
          show"Query[${typeName[I]}, ${typeName[A]}]", q.pos, q.sql, q.outputAnalysis
        ), colors)
    }

    implicit class Update0YoloOps(u: Update0) {

      def quick(implicit colors: Colors = Colors.Ansi): M[Unit] =
        u.run.flatMap(a => out(show"$a row(s) updated", colors)).transact(xa)

      def check(implicit colors: Colors = Colors.Ansi): M[Unit] =
        checkImpl(Analyzable.unpack(u), colors)
    }

    implicit class UpdateYoloOps[I: TypeTag](u: Update[I]) {

      def quick(i: I)(implicit colors: Colors = Colors.Ansi): M[Unit] =
        u.toUpdate0(i).quick

      def check(implicit colors: Colors = Colors.Ansi): M[Unit] =
        checkImpl(Analyzable.unpack(u), colors)
    }

    implicit class ConnectionIOYoloOps[A](ca: ConnectionIO[A]) {
      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      def quick(implicit colors: Colors = Colors.Ansi): M[Unit] =
        ca.flatMap(a => out(a.toString, colors)).transact(xa)
    }

    implicit class StreamYoloOps[A](pa: Stream[ConnectionIO, A]) {
      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      def quick(implicit colors: Colors = Colors.Ansi): M[Unit] =
        pa.evalMap(a => out(a.toString, colors)).compile.drain.transact(xa)
    }

    private def checkImpl(args: AnalysisArgs, colors: Colors): M[Unit] =
      analyze(args).flatMap { report =>
        val formatted = formatReport(args, report, colors)
        delay(println(formatted.padLeft("  ")))
      }.transact(xa)
  }
}
