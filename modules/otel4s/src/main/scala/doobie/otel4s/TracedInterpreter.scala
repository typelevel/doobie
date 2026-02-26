// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import java.sql.{PreparedStatement, ResultSet, SQLException}

import cats.data.Kleisli
import cats.effect.{Async, IOLocal, LiftIO, Resource}
import cats.free.Free
import cats.mtl.Local
import cats.syntax.all.*
import cats.{Applicative, ~>}
import doobie.free.KleisliInterpreter
import doobie.util.log.{LogHandler, LoggingInfo}
import doobie.util.trace.TraceEvent
import org.typelevel.otel4s.Attributes
import org.typelevel.otel4s.semconv.attributes.{DbAttributes, ErrorAttributes}
import org.typelevel.otel4s.trace.{SpanFinalizer, SpanKind, StatusCode, Tracer, TracerProvider}

/** Interpreter that wraps doobie execution in otel4s spans.
  *
  * Span enrichment and naming are controlled by [[TracingConfig]]:
  *
  *   - [[TracingConfig.captureQuery]] controls query text/parameter capture.
  *   - [[TracingConfig.attributesExtractor]] decodes a doobie label into attributes (default:
  *     [[AttributesExtractor.json]]).
  *   - [[TracingConfig.spanNamer]] chooses an optional custom span name from label/attributes context.
  *
  * With the default naming flow (`SpanNamer.fromAttribute(db.query.summary)`), span names come from extracted
  * `db.query.summary` when present; otherwise naming falls back to JDBC operation names (`executeQuery`, etc.).
  *
  * @example
  *   to support both new fragment helpers and legacy raw labels:
  *   {{{
  * import doobie.otel4s.*
  * import org.typelevel.otel4s.AttributeKey
  * import org.typelevel.otel4s.semconv.attributes.DbAttributes
  *
  * val legacyLabelKey = AttributeKey[String]("db.query.legacy_label")
  *
  * val config =
  *   TracingConfig
  *     .recommended(DbAttributes.DbSystemNameValue.Postgresql, "app")
  *     .withAttributesExtractor(
  *       AttributesExtractor
  *         .json
  *         .orElse(AttributesExtractor.plain(legacyLabelKey))
  *     )
  *     .withSpanNamer(
  *       SpanNamer
  *         .fromAttribute(DbAttributes.DbQuerySummary)
  *         .orElse(SpanNamer.fromAttribute(legacyLabelKey))
  *     )
  *   }}}
  *
  * @see
  *   [[https://opentelemetry.io/docs/specs/semconv/database/database-spans]]
  *
  * @param config
  *   tracing configuration including default span naming and capture settings
  *
  * @param logHandler
  *   log handler used by the underlying doobie interpreter
  *
  * @param local
  *   local storage for an optional per-operation span name
  */
private class TracedInterpreter[F[_]: Async: Tracer](
    config: TracingConfig,
    logHandler: LogHandler[F],
    local: Local[F, Option[String]]
) extends KleisliInterpreter(logHandler) {
  import TracedInterpreter.SpanParams

  private val finalizationStrategy: SpanFinalizer.Strategy = {
    case Resource.ExitCase.Errored(error) =>
      val setStatus = Option(error.getMessage).filter(_.nonEmpty) match {
        case Some(message) => SpanFinalizer.setStatus(StatusCode.Error, message)
        case None          => SpanFinalizer.setStatus(StatusCode.Error)
      }

      val general = SpanFinalizer.addAttribute(ErrorAttributes.ErrorType(error.getClass.getName)) |+|
        SpanFinalizer.recordException(error) |+|
        setStatus

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
      event: TraceEvent,
      interpreter: G ~> Kleisli[F, J, *]
  )(fa: Free[G, A]): Kleisli[F, J, A] =
    Kleisli { j =>
      Async[F].uncancelable { poll =>
        val params = preparedStatementSpanParams(event.loggingInfo)

        Tracer[F]
          .spanBuilder(params.customSpanName.getOrElse(config.defaultSpanName))
          .withSpanKind(SpanKind.Client)
          .addAttributes(params.attributes)
          .withFinalizationStrategy(finalizationStrategy)
          .build
          .surround(poll(local.scope(super.trace(event, interpreter)(fa).run(j))(params.customSpanName)))
      }
    }

  override lazy val PreparedStatementInterpreter: PreparedStatementInterpreter =
    new PreparedStatementInterpreter {

      def updateSpan(operation: String): Kleisli[F, PreparedStatement, Unit] =
        Kleisli.liftF[F, PreparedStatement, Unit](
          Tracer[F].withCurrentSpanOrNoop { span =>
            local.ask[Option[String]].flatMap { customSpanName =>
              span.updateName(customSpanName.getOrElse(operation)) >>
                span.addAttribute(DbAttributes.DbOperationName(operation))
            }
          }
        )

      override def execute: Kleisli[F, PreparedStatement, Boolean] =
        updateSpan("execute") >> super.execute

      override def execute(a: String): Kleisli[F, PreparedStatement, Boolean] =
        updateSpan("execute") >> super.execute(a)

      override def execute(a: String, b: Array[Int]): Kleisli[F, PreparedStatement, Boolean] =
        updateSpan("execute") >> super.execute(a, b)

      override def execute(a: String, b: Array[String]): Kleisli[F, PreparedStatement, Boolean] =
        updateSpan("execute") >> super.execute(a, b)

      override def execute(a: String, b: Int): Kleisli[F, PreparedStatement, Boolean] =
        updateSpan("execute") >> super.execute(a, b)

      override def executeBatch: Kleisli[F, PreparedStatement, Array[Int]] =
        updateSpan("executeBatch") >> super.executeBatch

      override def executeLargeBatch: Kleisli[F, PreparedStatement, Array[Long]] =
        updateSpan("executeLargeBatch") >> super.executeLargeBatch

      override def executeLargeUpdate: Kleisli[F, PreparedStatement, Long] =
        updateSpan("executeLargeUpdate") >> super.executeLargeUpdate

      override def executeLargeUpdate(a: String): Kleisli[F, PreparedStatement, Long] =
        updateSpan("executeLargeUpdate") >> super.executeLargeUpdate(a)

      override def executeLargeUpdate(a: String, b: Array[Int]): Kleisli[F, PreparedStatement, Long] =
        updateSpan("executeLargeUpdate") >> super.executeLargeUpdate(a, b)

      override def executeLargeUpdate(a: String, b: Array[String]): Kleisli[F, PreparedStatement, Long] =
        updateSpan("executeLargeUpdate") >> super.executeLargeUpdate(a, b)

      override def executeLargeUpdate(a: String, b: Int): Kleisli[F, PreparedStatement, Long] =
        updateSpan("executeLargeUpdate") >> super.executeLargeUpdate(a, b)

      override def executeQuery: Kleisli[F, PreparedStatement, ResultSet] =
        updateSpan("executeQuery") >> super.executeQuery

      override def executeQuery(a: String): Kleisli[F, PreparedStatement, ResultSet] =
        updateSpan("executeQuery") >> super.executeQuery(a)

      override def executeUpdate: Kleisli[F, PreparedStatement, Int] =
        updateSpan("executeUpdate") >> super.executeUpdate

      override def executeUpdate(a: String): Kleisli[F, PreparedStatement, Int] =
        updateSpan("executeUpdate") >> super.executeUpdate(a)

      override def executeUpdate(a: String, b: Array[Int]): Kleisli[F, PreparedStatement, Int] =
        updateSpan("executeUpdate") >> super.executeUpdate(a, b)

      override def executeUpdate(a: String, b: Array[String]): Kleisli[F, PreparedStatement, Int] =
        updateSpan("executeUpdate") >> super.executeUpdate(a, b)

      override def executeUpdate(a: String, b: Int): Kleisli[F, PreparedStatement, Int] =
        updateSpan("executeUpdate") >> super.executeUpdate(a, b)
    }

  private def preparedStatementSpanParams(info: LoggingInfo): SpanParams = {
    val label = info.label
    val builder = Attributes.newBuilder

    builder.addAll(config.constAttributes)

    recordQuery(info, builder)

    val parsedAttributes =
      if (label.nonEmpty && label != doobie.util.unlabeled)
        config.attributesExtractor.extract(label)
      else
        None

    parsedAttributes.foreach { attributes =>
      builder.addAll(attributes)
    }

    val customSpanName = config.spanNamer.spanName(
      SpanNamer.Context(label, info.sql, parsedAttributes)
    )

    SpanParams(customSpanName, builder.result())
  }

  private def recordQuery(info: LoggingInfo, builder: Attributes.Builder): Unit = {

    def addParams(batch: List[Any], prefix: String): Unit =
      batch.zipWithIndex.foreach { case (param, idx) =>
        builder.addOne(s"db.query.parameter.$prefix$idx", String.valueOf(param))
      }

    val isParameterized = info.params.allParams.exists(_.nonEmpty)

    if (info.params.allParams.sizeIs > 1)
      builder.addOne(DbAttributes.DbOperationBatchSize(info.params.allParams.size.toLong))

    config.captureQuery.queryTextPolicy match {
      case QueryCaptureConfig.QueryTextPolicy.Always =>
        builder.addOne(DbAttributes.DbQueryText(info.sql.trim))

      case QueryCaptureConfig.QueryTextPolicy.ParameterizedOnly =>
        if (isParameterized)
          builder.addOne(DbAttributes.DbQueryText(info.sql.trim))

      case QueryCaptureConfig.QueryTextPolicy.None =>
    }

    config.captureQuery.queryParametersPolicy match {
      case QueryCaptureConfig.QueryParametersPolicy.NonBatchOnly =>
        info.params.allParams match {
          case nonBatch :: Nil =>
            addParams(nonBatch, "")

          case _ =>

        }

      case QueryCaptureConfig.QueryParametersPolicy.All =>
        info.params.allParams match {
          case Nil =>

          case nonBatch :: Nil =>
            addParams(nonBatch, "")

          case batches =>
            batches.zipWithIndex.foreach { case (batch, idx) => addParams(batch, s"$idx.") }
        }

      case QueryCaptureConfig.QueryParametersPolicy.None =>
    }
  }

}

object TracedInterpreter {

  private final case class SpanParams(
      customSpanName: Option[String],
      attributes: Attributes
  )

  def create[F[_]: Async: TracerProvider: LiftIO](
      config: TracingConfig,
      logHandler: LogHandler[F]
  ): F[KleisliInterpreter[F]] =
    TracerProvider[F]
      .tracer(config.tracerScopeName)
      .withVersion(doobie.buildinfo.version)
      .get
      .flatMap { implicit tracer =>
        IOLocal(Option.empty[String]).to[F].map { ioLocal =>
          // todo: use .asLocal[F] when CE 3.7.0 is released
          val local: Local[F, Option[String]] = new Local[F, Option[String]] {
            def applicative: Applicative[F] = implicitly
            def ask[E2 >: Option[String]]: F[E2] =
              Async[F].widen[Option[String], E2](ioLocal.get.to[F])
            def local[A](fa: F[A])(f: Option[String] => Option[String]): F[A] =
              Async[F].bracket(ioLocal.modify(e => (f(e), e)).to[F])(_ => fa)(ioLocal.set(_).to[F])
          }

          new TracedInterpreter(config, logHandler, local)
        }
      }
}
