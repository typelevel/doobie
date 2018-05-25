// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.reflect.runtime.universe.TypeTag

import cats.effect._
import doobie.free.connection.{ ConnectionIO, delay }
import doobie.syntax.connectionio._
import doobie.util.query._
import doobie.util.update._
import doobie.util.testing._
import doobie.util.transactor._
import fs2.Stream
import scala.Predef._

/** Module for implicit syntax useful in REPL session. */
object yolo {

  import doobie.free.connection.AsyncConnectionIO

  class Yolo[M[_]: Sync](xa: Transactor[M]) {

    private def out(s: String): ConnectionIO[Unit] =
      delay(Console.println(s"${Console.BLUE}  $s${Console.RESET}"))

    implicit class Query0YoloOps[A: TypeTag](q: Query0[A]) {

      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      def quick: M[Unit] =
        q.stream
         .map(_.toString)
         .evalMap(out)
         .compile
         .drain
         .transact(xa)

      def check: M[Unit] =
        checkImpl(Analyzable.unpack(q))

      def checkOutput: M[Unit] =
        checkImpl(AnalysisArgs(
          typeName[Query0[A]], q.pos, q.sql, q.outputAnalysis
        ))
    }

    implicit class QueryYoloOps[I: TypeTag, A: TypeTag](q: Query[I,A]) {

      def quick(i: I): M[Unit] =
        q.toQuery0(i).quick

      def check: M[Unit] =
        checkImpl(Analyzable.unpack(q))

      def checkOutput: M[Unit] =
        checkImpl(AnalysisArgs(
          typeName[Query[I, A]], q.pos, q.sql, q.outputAnalysis
        ))
    }

    implicit class Update0YoloOps(u: Update0) {

      def quick: M[Unit] =
        u.run.flatMap(a => out(s"$a row(s) updated")).transact(xa)

      def check: M[Unit] =
        checkImpl(Analyzable.unpack(u))
    }

    implicit class UpdateYoloOps[I: TypeTag](u: Update[I]) {

      def quick(i: I): M[Unit] =
        u.toUpdate0(i).quick

      def check: M[Unit] =
        checkImpl(Analyzable.unpack(u))
    }

    implicit class ConnectionIOYoloOps[A](ca: ConnectionIO[A]) {
      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      def quick: M[Unit] = ca.flatMap(a => out(a.toString)).transact(xa)
    }

    implicit class StreamYoloOps[A](pa: Stream[ConnectionIO, A]) {
      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      def quick: M[Unit] = pa.evalMap(a => out(a.toString)).compile.drain.transact(xa)
    }

    private def checkImpl(args: AnalysisArgs): M[Unit] =
      analyze(args).flatMap { report =>
        val formatted = formatReport(args, report)
        delay(println(formatted.padLeft("  ")))
      }.transact(xa)
  }
}
