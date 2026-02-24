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
import org.typelevel.otel4s.semconv.attributes.DbAttributes
import org.typelevel.otel4s.trace.{SpanFinalizer, SpanKind, StatusCode, Tracer, TracerProvider}

/** Interpreter that wraps doobie execution in otel4s spans.
  *
  * @see
  *   [[https://opentelemetry.io/docs/specs/semconv/db/database-spans]]
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
class TracedInterpreter[F[_]: Async: Tracer](
    config: TracedInterpreter.Config,
    logHandler: LogHandler[F],
    local: Local[F, Option[String]]
) extends KleisliInterpreter(logHandler) {
  import TracedInterpreter.SpanParams

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
    Kleisli { j =>
      Async[F].uncancelable { poll =>
        val params = preparedStatementSpanParams(event.loggingInfo)

        Tracer[F]
          .spanBuilder(config.defaultSpanName)
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
}

object TracedInterpreter {

  private final case class SpanParams(
      customSpanName: Option[String],
      attributes: Attributes
  )

  trait Config {

    /** The name of the [[org.typelevel.otel4s.trace.TracerProvider]] scope to use for this interpreter.
      */
    def tracerScopeName: String

    /** The default span name to use when no label is available.
      */
    def defaultSpanName: String

    /** The attributes to add to every span created by this interpreter.
      */
    def constAttributes: Attributes

    /** Controls query text/parameter capture.
      */
    def captureConfig: CaptureConfig

    /** Extracts attributes from a raw label. */
    def attributesExtractor: AttributesExtractor

    /** Computes the final span name from typed label context. */
    def spanNamer: SpanNamer

    /** Returns a copy with a new tracer scope name. */
    def withTracerScopeName(value: String): Config

    /** Returns a copy with a new default span name. */
    def withDefaultSpanName(value: String): Config

    /** Returns a copy with new constant attributes. */
    def withConstAttributes(value: Attributes): Config

    /** Returns a copy with a new capture configuration. */
    def withCaptureConfig(value: CaptureConfig): Config

    /** Returns a copy with a new label parser. */
    def withLabelParser(value: AttributesExtractor): Config

    /** Returns a copy with a new span namer. */
    def withSpanNamer(value: SpanNamer): Config
  }

  object Config {
    private val Default = Config(
      "doobie",
      "doobie:exec",
      Attributes.empty,
      CaptureConfig.disabled,
      AttributesExtractor.json,
      SpanNamer.fromAttribute(DbAttributes.DbQuerySummary)
    )

    /** Default config:
      *
      *   - `tracerScopeName`: `"doobie"`
      *   - `defaultSpanName`: `"doobie:exec"`
      *   - `constAttributes`: empty
      *   - `captureConfig`: [[CaptureConfig.disabled]]
      */
    def default: Config = Default

    /** Builds a configuration instance with the provided values. */
    def apply(
        tracerScopeName: String,
        defaultSpanName: String,
        constAttributes: Attributes,
        captureConfig: CaptureConfig,
        labelParser: AttributesExtractor,
        spanNamer: SpanNamer
    ): Config =
      ConfigImpl(tracerScopeName, defaultSpanName, constAttributes, captureConfig, labelParser, spanNamer)

    /** Builds a configuration instance with default label parser and span namer. */
    def apply(
        tracerScopeName: String,
        defaultSpanName: String,
        constAttributes: Attributes,
        captureConfig: CaptureConfig
    ): Config =
      ConfigImpl(
        tracerScopeName,
        defaultSpanName,
        constAttributes,
        captureConfig,
        AttributesExtractor.json,
        SpanNamer.fromAttribute(DbAttributes.DbQuerySummary)
      )

    /** Builds a semantic-conventions-oriented config that guarantees key DB identity attributes.
      *
      * This constructor enforces explicit database identity and applies semconv-friendly defaults:
      *   - always sets `db.system.name`
      *   - always sets `db.namespace`
      *   - enables `db.query.text` capture
      *   - keeps parameter capture disabled by default
      */
    def recommended(
        dbSystemName: DbAttributes.DbSystemNameValue,
        dbNamespace: String
    ): Config = {
      require(dbNamespace.nonEmpty, "dbNamespace must be non-empty")

      val semconvConstAttributes = Attributes(
        DbAttributes.DbSystemName(dbSystemName),
        DbAttributes.DbNamespace(dbNamespace)
      )

      Default
        .withConstAttributes(semconvConstAttributes)
        .withCaptureConfig(
          CaptureConfig(
            Some(
              CaptureConfig.CaptureQuery(
                captureQueryStatementText = true,
                captureQueryStatementParameters = false
              )
            )
          )
        )
    }

    final case class ConfigImpl(
        tracerScopeName: String,
        defaultSpanName: String,
        constAttributes: Attributes,
        captureConfig: CaptureConfig,
        attributesExtractor: AttributesExtractor,
        spanNamer: SpanNamer
    ) extends Config {
      def withTracerScopeName(value: String): Config = copy(tracerScopeName = value)
      def withDefaultSpanName(value: String): Config = copy(defaultSpanName = value)
      def withConstAttributes(value: Attributes): Config = copy(constAttributes = value)
      def withCaptureConfig(value: CaptureConfig): Config = copy(captureConfig = value)
      def withLabelParser(value: AttributesExtractor): Config = copy(attributesExtractor = value)
      def withSpanNamer(value: SpanNamer): Config = copy(spanNamer = value)
    }
  }

  /** Controls query text/parameter capture.
    */
  sealed trait CaptureConfig {

    /** When query capture is enabled, span attributes follow OpenTelemetry DB semantic conventions where possible.
      */
    def captureQuery: Option[CaptureConfig.CaptureQuery]
  }

  object CaptureConfig {

    sealed trait CaptureQuery {

      /** When `captureQueryStatementText` is true, the SQL text is attached as `db.query.text` via
        * [[org.typelevel.otel4s.semconv.attributes.DbAttributes.DbQueryText]].
        */
      def captureQueryStatementText: Boolean

      /** When `captureQueryStatementParameters` is true, parameters are added as string attributes named
        * `db.query.parameter.<index>` for non-batched statements, or `db.query.parameter.<batch>.<index>` for batched
        * statements. Null parameters are encoded as the literal string `"<null>"`.
        */
      def captureQueryStatementParameters: Boolean
    }

    object CaptureQuery {

      /** Builds a query capture configuration.
        *
        * @param captureQueryStatementText
        *   when true, attach `db.query.text` with the SQL text
        *
        * @param captureQueryStatementParameters
        *   when true, attach `db.query.parameter.*` attributes for parameters
        */
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

    private val Disabled = CaptureConfig(
      captureQuery = None
    )

    /** Disabled capture config with no query capture. */
    def disabled: CaptureConfig = Disabled

    /** Build a capture configuration with optional query capture. */
    def apply(
        captureQuery: Option[CaptureQuery]
    ): CaptureConfig =
      CaptureConfigImpl(captureQuery)

    private final case class CaptureConfigImpl(
        captureQuery: Option[CaptureQuery]
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
