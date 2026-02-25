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
  def noop: SpanNamer = Noop

  /** Use the raw query label string as the span name.
    *
    * @example
    *   {{{
    * SpanNamer.fromQueryLabel
    *
    * // label "my query" -> span name "my query"
    *   }}}
    */
  def fromQueryLabel: SpanNamer = FromQueryLabel

  /** Look up a specific attribute value as the span name.
    *
    * With the default interpreter flow, this strategy receives attributes produced by the configured
    * [[AttributesExtractor]]. If the target key is found, that value is used as the operation span name; otherwise this
    * namer returns `None`.
    *
    * When used as the only namer, `None` means the interpreter falls back to JDBC operation names (`executeQuery`,
    * `executeUpdate`, etc.). Use [[SpanNamer.orElse]] to define explicit fallback naming chains.
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

  private object Noop extends SpanNamer {
    def spanName(context: Context): Option[String] = None
  }

  private object FromQueryLabel extends SpanNamer {
    def spanName(context: Context): Option[String] =
      Option(context.rawLabel).filter(label => label.nonEmpty && label != doobie.util.unlabeled)
  }

  final private class FromAttribute(key: AttributeKey[String]) extends SpanNamer {
    def spanName(context: Context): Option[String] =
      context.attributes.flatMap(_.get(key).map(_.value))
  }

}
