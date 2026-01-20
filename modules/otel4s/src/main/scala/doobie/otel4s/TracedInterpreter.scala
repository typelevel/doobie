// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import java.sql.{PreparedStatement, ResultSet, SQLException}
import java.util.logging.Logger

import cats.data.Kleisli
import cats.effect.Resource
import cats.free.Free
import cats.syntax.all.*
import cats.~>
import doobie.WeakAsync
import doobie.free.KleisliInterpreter
import doobie.util.log.{LogHandler, LoggingInfo}
import doobie.util.trace.TraceEvent
import org.typelevel.otel4s.Attributes
import org.typelevel.otel4s.semconv.attributes.DbAttributes
import org.typelevel.otel4s.trace.{SpanFinalizer, SpanKind, StatusCode, Tracer, TracerProvider}
import doobie.otel4s.AttributesCodec.*

class TracedInterpreter[F[_]: WeakAsync: Tracer](
    config: TracedInterpreter.Config,
    logHandler: LogHandler[F]
) extends KleisliInterpreter(logHandler) {

  private val jdkLogger = Logger.getLogger(getClass.getName)

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
      event: TraceEvent,
      interpreter: G ~> Kleisli[F, J, *]
  )(fa: Free[G, A]): Kleisli[F, J, A] =
    event match {
      case evt: TraceEvent.ExecutePreparedStatement =>
        Kleisli { j =>
          WeakAsync[F].uncancelable { poll =>
            Tracer[F]
              .spanBuilder(config.defaultSpanName)
              .withSpanKind(SpanKind.Client)
              .addAttributes(tracePreparedStatementAttributes(evt.loggingInfo))
              .withFinalizationStrategy(finalizationStrategy)
              .build
              .surround(poll(super.trace(event, interpreter)(fa).run(j)))
          }
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

  private def tracePreparedStatementAttributes(info: LoggingInfo): Attributes = {
    val params = info.params
    val label = info.label

    val builder = Attributes.newBuilder

    if (config.captureConfig.captureLabelAttributes) {
      io.circe.parser.decode[Attributes](label) match {
        case Right(attributes) =>
          builder.addAll(attributes)

        case Left(error) =>
          jdkLogger.warning(s"Error parsing query label $label as attributes: ${error.getMessage}")
      }
    }

    if (config.captureConfig.captureQueryStatementText) {
      builder.addOne(DbAttributes.DbQueryText(info.sql))
    }

    if (config.captureConfig.captureQueryStatementParameters) {
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
    }

    builder.result()
  }
}

object TracedInterpreter {

  trait Config {
    def tracerScopeName: String
    def defaultSpanName: String
    def constAttributes: Attributes
    def captureConfig: CaptureConfig
  }

  object Config {
    private val Default = Config(
      "doobie",
      "doobie:exec",
      Attributes.empty,
      CaptureConfig.default
    )

    def default: Config = Default

    def apply(
        tracerScopeName: String,
        defaultSpanName: String,
        constAttributes: Attributes,
        captureConfig: CaptureConfig
    ): Config =
      ConfigImpl(tracerScopeName, defaultSpanName, constAttributes, captureConfig)

    final case class ConfigImpl(
        tracerScopeName: String,
        defaultSpanName: String,
        constAttributes: Attributes,
        captureConfig: CaptureConfig
    ) extends Config
  }

  sealed trait CaptureConfig {
    def captureQueryStatementText: Boolean

    def captureQueryStatementParameters: Boolean

    def captureLabelAttributes: Boolean
  }

  object CaptureConfig {
    private val Default = CaptureConfig(
      captureQueryStatementText = false,
      captureQueryStatementParameters = false,
      captureLabelAttributes = false
    )

    def default: CaptureConfig = Default

    def apply(
        captureQueryStatementText: Boolean,
        captureQueryStatementParameters: Boolean,
        captureLabelAttributes: Boolean
    ): CaptureConfig =
      CaptureConfigImpl(captureQueryStatementText, captureQueryStatementParameters, captureLabelAttributes)

    private final case class CaptureConfigImpl(
        captureQueryStatementText: Boolean,
        captureQueryStatementParameters: Boolean,
        captureLabelAttributes: Boolean
    ) extends CaptureConfig
  }

  def create[F[_]: WeakAsync: TracerProvider](
      config: Config,
      logHandler: LogHandler[F]
  ): F[TracedInterpreter[F]] =
    TracerProvider[F].get(config.tracerScopeName).map { implicit tracer =>
      new TracedInterpreter(config, logHandler)
    }
}
