// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import org.typelevel.otel4s.Attributes
import org.typelevel.otel4s.semconv.attributes.DbAttributes

trait TracingConfig {

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
  def captureQuery: QueryCaptureConfig

  /** Extracts attributes from a raw label. */
  def attributesExtractor: AttributesExtractor

  /** Computes the final span name from typed label context. */
  def spanNamer: SpanNamer

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

  /** Returns a copy with a new span namer. */
  def withSpanNamer(value: SpanNamer): TracingConfig
}

object TracingConfig {
  private object Defaults {
    val tracerScopeName = "doobie"
    val defaultSpanName = "doobie:exec"
    val queryCaptureConfig: QueryCaptureConfig = QueryCaptureConfig.recommended
    val attributesExtractor: AttributesExtractor = AttributesExtractor.json
    val spanNamer: SpanNamer = SpanNamer.fromAttribute(DbAttributes.DbQuerySummary)
  }

  /** Builds a configuration instance with the provided values. */
  def apply(
      tracerScopeName: String,
      defaultSpanName: String,
      constAttributes: Attributes,
      captureQuery: QueryCaptureConfig,
      attributesExtractor: AttributesExtractor,
      spanNamer: SpanNamer
  ): TracingConfig =
    TracingConfigImpl(
      tracerScopeName,
      defaultSpanName,
      constAttributes,
      captureQuery,
      attributesExtractor,
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
      spanNamer = Defaults.spanNamer,
    )
  }

  final case class TracingConfigImpl(
      tracerScopeName: String,
      defaultSpanName: String,
      constAttributes: Attributes,
      captureQuery: QueryCaptureConfig,
      attributesExtractor: AttributesExtractor,
      spanNamer: SpanNamer
  ) extends TracingConfig {
    def withTracerScopeName(value: String): TracingConfig = copy(tracerScopeName = value)
    def withDefaultSpanName(value: String): TracingConfig = copy(defaultSpanName = value)
    def withConstAttributes(value: Attributes): TracingConfig = copy(constAttributes = value)
    def withCaptureQuery(value: QueryCaptureConfig): TracingConfig = copy(captureQuery = value)
    def withAttributesExtractor(value: AttributesExtractor): TracingConfig = copy(attributesExtractor = value)
    def withSpanNamer(value: SpanNamer): TracingConfig = copy(spanNamer = value)
  }
}
