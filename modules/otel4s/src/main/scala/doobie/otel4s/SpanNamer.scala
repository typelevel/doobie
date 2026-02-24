// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import org.typelevel.otel4s.{AttributeKey, Attributes}

trait SpanNamer { self =>

  /** Chooses a span name from the raw label and/or parsed attributes.
    */
  def spanName(context: SpanNamer.Context): Option[String]

  /** Compose with a fallback namer tried when this namer returns `None`. */
  def orElse(fallback: SpanNamer): SpanNamer =
    new SpanNamer {
      def spanName(context: SpanNamer.Context): Option[String] =
        self.spanName(context).orElse(fallback.spanName(context))
    }
}

object SpanNamer {

  sealed trait Context {

    /** The raw label string.
      */
    def rawLabel: String

    /** The SQL statement text.
      */
    def sql: String

    /** Attributes extracted by [[AttributesExtractor]].
      *
      * @return
      *   `None` if attributes extraction failed.
      */
    def attributes: Option[Attributes]

  }

  object Context {

    /** Creates a context using given parameters.
      *
      * @param rawLabel
      *   the raw label string
      *
      * @param sql
      *   the SQL statement text
      *
      * @param attributes
      *   attributes extracted by [[AttributesExtractor]]
      */
    def apply(
        rawLabel: String,
        sql: String,
        attributes: Option[Attributes]
    ): Context =
      ContextImpl(rawLabel, sql, attributes)

    private final case class ContextImpl(
        rawLabel: String,
        sql: String,
        attributes: Option[Attributes]
    ) extends Context
  }

  /** No span name override. */
  def none: SpanNamer = NoOverride

  /** Use the raw label string as the span name.
    *
    * @example
    *   {{{
    * SpanNamer.direct
    *
    * // label "my query" -> span name "my query"
    *   }}}
    */
  def direct: SpanNamer = Direct

  /** Look up a specific attribute value as the span name.
    *
    * @example
    *   {{{
    * SpanNamer.fromAttribute(DbAttributes.DbQuerySummary)
    *
    * // if parsed attributes contain db.query.summary: "select users" -> span name "select users"
    *   }}}
    *
    * @param key
    *   the [[org.typelevel.otel4s.AttributeKey]] to look up in parsed attributes
    */
  def fromAttribute(key: AttributeKey[String]): SpanNamer = new FromAttribute(key)

  private object NoOverride extends SpanNamer {
    def spanName(context: Context): Option[String] = None
  }

  private object Direct extends SpanNamer {
    def spanName(context: Context): Option[String] =
      Option(context.rawLabel).filter(label => label.nonEmpty && label != doobie.util.unlabeled)
  }

  final private class FromAttribute(key: AttributeKey[String]) extends SpanNamer {
    def spanName(context: Context): Option[String] =
      context.attributes.flatMap(_.get(key).map(_.value))
  }

}
