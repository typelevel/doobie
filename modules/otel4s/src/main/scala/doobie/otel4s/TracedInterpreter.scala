// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import java.sql.{PreparedStatement, ResultSet, SQLException}

import cats.data.Kleisli
import cats.effect.Resource
import cats.free.Free
import cats.syntax.all.*
import cats.~>
import doobie.WeakAsync
import doobie.free.KleisliInterpreter
import doobie.util.log.{LogHandler, LoggingInfo}
import org.typelevel.otel4s.Attributes
import org.typelevel.otel4s.semconv.attributes.DbAttributes
import org.typelevel.otel4s.trace.{SpanFinalizer, SpanKind, StatusCode, Tracer, TracerProvider}

class TracedInterpreter[F[_]: WeakAsync: Tracer](logHandler: LogHandler[F]) extends KleisliInterpreter(logHandler) {

  private val finalizationStrategy: SpanFinalizer.Strategy = {
    case Resource.ExitCase.Errored(error) =>
      val general = SpanFinalizer.recordException(error) |+| SpanFinalizer.setStatus(StatusCode.Error)

      error match {
        case sql: SQLException =>
          general |+| SpanFinalizer.addAttribute(DbAttributes.DbResponseStatusCode(sql.getErrorCode.toString))

        case _ =>
          general
      }

    case Resource.ExitCase.Canceled =>
      SpanFinalizer.setStatus(StatusCode.Error, "canceled")
  }

  override def trace[G[_], J, A](
      info: LoggingInfo,
      interpreter: G ~> Kleisli[F, J, *]
  )(fa: Free[G, A]): Kleisli[F, J, A] =
    Kleisli { j =>
      val params = info.params
      val label = info.label

      val builder = Attributes.newBuilder

      builder.addOne(DbAttributes.DbQueryText(info.sql))
      builder.addOne("label", label)

      def addParams(batch: List[Any], prefix: String): Unit =
        batch.zipWithIndex.foreach { case (param, idx) =>
          val asString = if (param != null) param.toString else "<null>"
          builder.addOne(s"db.query.parameter.$prefix$idx", asString)
        }

      params.allParams match {
        case Nil =>

        case nonBatch :: Nil =>
          addParams(nonBatch, "")

        case batches =>
          batches.zipWithIndex.foreach { case (batch, idx) => addParams(batch, s"$idx.") }
      }

      val attributes = builder.result()

      WeakAsync[F].uncancelable { poll =>
        Tracer[F]
          .spanBuilder("doobie:trace:prepared_statement")
          .withSpanKind(SpanKind.Client)
          .addAttributes(attributes)
          .withFinalizationStrategy(finalizationStrategy)
          .build
          .surround(poll(super.trace(info, interpreter)(fa).run(j)))
      }
    }

  override lazy val PreparedStatementInterpreter: PreparedStatementInterpreter =
    new PreparedStatementInterpreter {

      def runTraced[A](operation: String, f: Kleisli[F, PreparedStatement, A]): Kleisli[F, PreparedStatement, A] =
        Kleisli.liftF[F, PreparedStatement, Unit](
          Tracer[F].currentSpanOrNoop.flatMap { span =>
            span.updateName(operation) >> span.addAttribute(DbAttributes.DbOperationName(operation))
          }
        ) >> f

      override def executeBatch: Kleisli[F, PreparedStatement, Array[Int]] =
        runTraced("executeBatch", super.executeBatch)

      override def executeLargeBatch: Kleisli[F, PreparedStatement, Array[Long]] =
        runTraced("executeLargeBatch", super.executeLargeBatch)

      override def execute: Kleisli[F, PreparedStatement, Boolean] =
        runTraced("execute", super.execute)

      override def executeUpdate: Kleisli[F, PreparedStatement, Int] =
        runTraced("executeUpdate", super.executeUpdate)

      override def executeQuery: Kleisli[F, PreparedStatement, ResultSet] =
        runTraced("executeQuery", super.executeQuery)
    }

}

object TracedInterpreter {
  def create[F[_]: WeakAsync: TracerProvider](logHandler: LogHandler[F]): F[TracedInterpreter[F]] =
    TracerProvider[F].get("doobie").map { implicit tracer =>
      new TracedInterpreter(logHandler)
    }
}
