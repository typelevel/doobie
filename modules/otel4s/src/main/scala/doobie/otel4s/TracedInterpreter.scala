// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import java.sql.{PreparedStatement, ResultSet, SQLException}
import java.util.logging.Logger

import cats.data.Kleisli
import cats.effect.{Async, IOLocal, LiftIO, Resource}
import cats.free.Free
import cats.mtl.Local
import cats.syntax.all.*
import cats.{Applicative, ~>}
import doobie.free.KleisliInterpreter
import doobie.util.log.{LogHandler, LoggingInfo}
import doobie.util.trace.TraceEvent
import org.typelevel.otel4s.{AttributeKey, Attributes}
import org.typelevel.otel4s.semconv.attributes.DbAttributes
import org.typelevel.otel4s.trace.{SpanFinalizer, SpanKind, StatusCode, Tracer, TracerProvider}
import doobie.otel4s.AttributesCodec.*

class TracedInterpreter[F[_]: Async: Tracer](
    config: TracedInterpreter.Config,
    logHandler: LogHandler[F],
    local: Local[F, Option[String]]
) extends KleisliInterpreter(logHandler) {
  import TracedInterpreter.CaptureConfig.CaptureLabel

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
          Async[F].uncancelable { poll =>
            val (spanName, attributes) = preparedStatementSpanParams(evt.loggingInfo)

            Tracer[F]
              .spanBuilder(spanName.getOrElse(config.defaultSpanName))
              .withSpanKind(SpanKind.Client)
              .addAttributes(attributes)
              .withFinalizationStrategy(finalizationStrategy)
              .build
              .surround(poll(local.scope(super.trace(event, interpreter)(fa).run(j))(spanName)))
          }
        }
    }

  override lazy val PreparedStatementInterpreter: PreparedStatementInterpreter =
    new PreparedStatementInterpreter {

      def runTraced[A](operation: String, f: Kleisli[F, PreparedStatement, A]): Kleisli[F, PreparedStatement, A] =
        Kleisli.liftF[F, PreparedStatement, Unit](
          Tracer[F].withCurrentSpanOrNoop { span =>
            local.ask[Option[String]].flatMap { customSpanName =>
              span.updateName(customSpanName.getOrElse(operation)) >>
                span.addAttribute(DbAttributes.DbOperationName(operation))
            }
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

  private def preparedStatementSpanParams(info: LoggingInfo): (Option[String], Attributes) = {
    val label = info.label
    val builder = Attributes.newBuilder

    builder.addAll(config.constAttributes)

    config.captureConfig.captureQuery.foreach { queryConfig =>
      if (queryConfig.captureQueryStatementText)
        builder.addOne(DbAttributes.DbQueryText(info.sql))

      if (queryConfig.captureQueryStatementParameters) {
        def addParams(batch: List[Any], prefix: String): Unit =
          batch.zipWithIndex.foreach { case (param, idx) =>
            val asString = if (param != null) param.toString else "<null>"
            builder.addOne(s"db.query.parameter.$prefix$idx", asString)
          }

        info.params.allParams match {
          case Nil =>

          case nonBatch :: Nil =>
            addParams(nonBatch, "")

          case batches =>
            batches.zipWithIndex.foreach { case (batch, idx) => addParams(batch, s"$idx.") }
        }
      }
    }

    val spanName =
      config.captureConfig.captureLabel.filter(_ => label.nonEmpty && label != doobie.util.unlabeled) match {
        case Some(_: CaptureLabel.AsSpanName) =>
          Some(label)

        case Some(asAttribute: CaptureLabel.AsAttribute) =>
          builder.addOne(asAttribute.attributeKey, label)
          None

        case Some(decodeAttributes: CaptureLabel.DecodeAttributes) =>
          io.circe.parser.decode[Attributes](label) match {
            case Right(attributes) =>
              builder.addAll(attributes)
              decodeAttributes.spanNameFromAttribute.flatMap(key => attributes.get(key)).map(_.value)

            case Left(error) =>
              decodeAttributes.fallback match {
                case Some(fallback) =>
                  builder.addOne(fallback.attributeKey, label)
                  None

                case None =>
                  jdkLogger.warning(s"Error parsing query label '$label' as attributes: ${error.getMessage}")
                  None
              }
          }

        case _ =>
          None
      }

    (spanName, builder.result())
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
    def captureQuery: Option[CaptureConfig.CaptureQuery]
    def captureLabel: Option[CaptureConfig.CaptureLabel]
  }

  object CaptureConfig {
    sealed trait CaptureQuery {
      def captureQueryStatementText: Boolean
      def captureQueryStatementParameters: Boolean
    }

    object CaptureQuery {
      def apply(
          captureQueryStatementText: Boolean,
          captureQueryStatementParameters: Boolean
      ): CaptureQuery =
        CaptureQueryImpl(captureQueryStatementText, captureQueryStatementParameters)

      private final case class CaptureQueryImpl(
          captureQueryStatementText: Boolean,
          captureQueryStatementParameters: Boolean
      ) extends CaptureQuery
    }

    sealed trait CaptureLabel

    object CaptureLabel {
      sealed trait AsSpanName extends CaptureLabel {}

      sealed trait AsAttribute extends CaptureLabel {
        def attributeKey: AttributeKey[String]
      }

      sealed trait DecodeAttributes extends CaptureLabel {
        def spanNameFromAttribute: Option[AttributeKey[String]]
        def fallback: Option[AsAttribute]
      }

      def asSpanName: AsSpanName =
        AsSpanName

      def asAttribute(attributeKey: AttributeKey[String]): AsAttribute =
        AsAttributeImpl(attributeKey)

      def decodeAttributes(
          spanNameFromAttribute: Option[AttributeKey[String]],
          fallback: Option[AsAttribute]
      ): DecodeAttributes =
        DecodeAttributesImpl(spanNameFromAttribute, fallback)

      private object AsSpanName extends AsSpanName

      private final case class AsAttributeImpl(
          attributeKey: AttributeKey[String]
      ) extends AsAttribute

      private final case class DecodeAttributesImpl(
          spanNameFromAttribute: Option[AttributeKey[String]],
          fallback: Option[AsAttribute]
      ) extends DecodeAttributes
    }

    private val Default = CaptureConfig(
      captureQuery = None,
      captureLabel = None
    )

    def default: CaptureConfig = Default

    def apply(
        captureQuery: Option[CaptureQuery],
        captureLabel: Option[CaptureLabel]
    ): CaptureConfig =
      CaptureConfigImpl(captureQuery, captureLabel)

    private final case class CaptureConfigImpl(
        captureQuery: Option[CaptureQuery],
        captureLabel: Option[CaptureLabel]
    ) extends CaptureConfig
  }

  def create[F[_]: Async: TracerProvider: LiftIO](
      config: Config,
      logHandler: LogHandler[F]
  ): F[TracedInterpreter[F]] =
    TracerProvider[F].get(config.tracerScopeName).flatMap { implicit tracer =>
      IOLocal(Option.empty[String]).to[F].map { ioLocal =>
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
