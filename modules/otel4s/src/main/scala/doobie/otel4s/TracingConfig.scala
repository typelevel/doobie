// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import org.typelevel.otel4s.Attributes
import org.typelevel.otel4s.semconv.attributes.DbAttributes

sealed trait TracingConfig {

  /** The name of the [[org.typelevel.otel4s.trace.TracerProvider]] scope to use for this interpreter. */
  def tracerScopeName: String

  /** The default span name to use when no label is available. */
  def defaultSpanName: String

  /** The attributes to add to every span created by this interpreter. */
  def constAttributes: Attributes

  /** Controls query text/parameter capture. */
  def captureQuery: QueryCaptureConfig

  /** Extracts attributes from a raw label. */
  def attributesExtractor: AttributesExtractor

  /** Analyzes SQL text into structured query metadata. */
  def queryAnalyzer: QueryAnalyzer

  /** Computes the final span name from typed label context. */
  def spanNamer: SpanNamer

  /** Returns a copy with added attributes. */
  def addConstAttributes(attributes: Attributes): TracingConfig

  /** Returns a copy with a new tracer scope name. */
  def withTracerScopeName(value: String): TracingConfig

  /** Returns a copy with a new default span name. */
  def withDefaultSpanName(value: String): TracingConfig

  /** Returns a copy with new constant attributes. */
  def withConstAttributes(value: Attributes): TracingConfig

  /** Returns a copy with query capture settings. */
  def withCaptureQuery(value: QueryCaptureConfig): TracingConfig

  /** Returns a copy with a new attributes extractor. */
  def withAttributesExtractor(value: AttributesExtractor): TracingConfig

  /** Returns a copy with a new query analyzer. */
  def withQueryAnalyzer(value: QueryAnalyzer): TracingConfig

  /** Returns a copy with a new span namer. */
  def withSpanNamer(value: SpanNamer): TracingConfig
}

object TracingConfig {
  object Defaults {
    val tracerScopeName: String = "doobie"
    val defaultSpanName: String = "doobie:exec"
    val queryCaptureConfig: QueryCaptureConfig = QueryCaptureConfig.recommended
    val attributesExtractor: AttributesExtractor = AttributesExtractor.json
    val queryAnalyzer: QueryAnalyzer = QueryAnalyzer.noop
    val spanNamer: SpanNamer = SpanNamer.fromAttribute(DbAttributes.DbQuerySummary)
  }

  /** Builds a configuration instance with the provided values. */
  def apply(
      tracerScopeName: String,
      defaultSpanName: String,
      constAttributes: Attributes,
      captureQuery: QueryCaptureConfig,
      attributesExtractor: AttributesExtractor,
      queryAnalyzer: QueryAnalyzer,
      spanNamer: SpanNamer
  ): TracingConfig =
    TracingConfigImpl(
      tracerScopeName,
      defaultSpanName,
      constAttributes,
      captureQuery,
      attributesExtractor,
      queryAnalyzer,
      spanNamer
    )

  /** Builds a configuration instance with default attributes extractor and span namer. */
  def apply(
      tracerScopeName: String,
      defaultSpanName: String,
      constAttributes: Attributes,
      captureQuery: QueryCaptureConfig
  ): TracingConfig =
    TracingConfigImpl(
      tracerScopeName,
      defaultSpanName,
      constAttributes,
      captureQuery,
      AttributesExtractor.json,
      QueryAnalyzer.noop,
      SpanNamer.fromAttribute(DbAttributes.DbQuerySummary)
    )

  /** Builds a semantic-conventions-oriented tracing config.
    *
    * This constructor applies the library defaults for scope, naming, extractor, and span namer, plus semantic DB
    * identity attributes:
    *   - `tracerScopeName`: `"doobie"`
    *   - `defaultSpanName`: `"doobie:exec"`
    *   - `captureQuery`: [[QueryCaptureConfig.recommended]] (`db.query.text` for parameterized queries only, query
    *     parameters disabled)
    *   - `attributesExtractor`: [[AttributesExtractor.json]]
    *   - `queryAnalyzer`: [[QueryAnalyzer.noop]]
    *   - `spanNamer`: [[SpanNamer.fromAttribute]](`db.query.summary`)
    *   - `constAttributes`: `db.system.name` and `db.namespace` from constructor args
    */
  def recommended(
      dbSystemName: DbAttributes.DbSystemNameValue,
      dbNamespace: String
  ): TracingConfig = {
    require(dbNamespace.nonEmpty, "dbNamespace must be non-empty")

    val constAttributes = Attributes(
      DbAttributes.DbSystemName(dbSystemName),
      DbAttributes.DbNamespace(dbNamespace)
    )

    TracingConfigImpl(
      tracerScopeName = Defaults.tracerScopeName,
      defaultSpanName = Defaults.defaultSpanName,
      constAttributes = constAttributes,
      captureQuery = Defaults.queryCaptureConfig,
      attributesExtractor = Defaults.attributesExtractor,
      queryAnalyzer = Defaults.queryAnalyzer,
      spanNamer = Defaults.spanNamer
    )
  }

  private final case class TracingConfigImpl(
      tracerScopeName: String,
      defaultSpanName: String,
      constAttributes: Attributes,
      captureQuery: QueryCaptureConfig,
      attributesExtractor: AttributesExtractor,
      queryAnalyzer: QueryAnalyzer,
      spanNamer: SpanNamer
  ) extends TracingConfig {
    def addConstAttributes(attributes: Attributes): TracingConfig =
      copy(constAttributes = constAttributes ++ attributes)
    def withTracerScopeName(value: String): TracingConfig = copy(tracerScopeName = value)
    def withDefaultSpanName(value: String): TracingConfig = copy(defaultSpanName = value)
    def withConstAttributes(value: Attributes): TracingConfig = copy(constAttributes = value)
    def withCaptureQuery(value: QueryCaptureConfig): TracingConfig = copy(captureQuery = value)
    def withAttributesExtractor(value: AttributesExtractor): TracingConfig = copy(attributesExtractor = value)
    def withQueryAnalyzer(value: QueryAnalyzer): TracingConfig = copy(queryAnalyzer = value)
    def withSpanNamer(value: SpanNamer): TracingConfig = copy(spanNamer = value)
  }
}
