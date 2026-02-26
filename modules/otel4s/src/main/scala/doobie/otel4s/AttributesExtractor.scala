// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import org.typelevel.otel4s.{AttributeKey, Attributes}

trait AttributesExtractor { self =>

  /** Extract attributes from the label.
    *
    * @param label
    *   label is guaranteed to be non-empty and not be "unlabeled"
    *
    * @return
    *   `Some(attributes)` on success, `None` on failure (triggers `orElse`)
    */
  def extract(label: String): Option[Attributes]

  /** Compose with a fallback extractor that is tried when this extractor returns `None`. */
  def orElse(fallback: AttributesExtractor): AttributesExtractor =
    new AttributesExtractor {
      def extract(label: String): Option[Attributes] =
        self.extract(label).orElse(fallback.extract(label))
    }
}

object AttributesExtractor {

  /** Decodes the raw doobie label as JSON-encoded [[org.typelevel.otel4s.Attributes]].
    *
    * This is the extractor used by [[doobie.otel4s.TracingConfig.recommended]]. It is also the expected decoder for
    * syntax helpers that encode attributes into labels, e.g.:
    *
    *   - `queryWithAttributes(...)` / `updateWithAttributes(...)`
    *   - `queryWithSummary(...)` / `updateWithSummary(...)` (they emit `db.query.summary` as attributes)
    *
    * If decoding fails, this extractor returns `None` and any `SpanNamer.fromAttribute(...)` strategy will not see
    * extracted attributes for that operation unless composed with `orElse(...)`.
    */
  val json: AttributesExtractor =
    JsonExtractor

  /** Wraps the raw label string as a single attribute. Always succeeds.
    *
    * @example
    *   {{{
    * AttributesExtractor.plain(AttributeKey[String]("db.query.label"))
    *
    * // label "my query" -> attribute db.query.label: "my query"
    *   }}}
    *
    * @param key
    *   the [[org.typelevel.otel4s.AttributeKey]] to use for the attribute
    */
  def plain(key: AttributeKey[String]): AttributesExtractor =
    new Plain(key)

  private object JsonExtractor extends AttributesExtractor {
    import AttributesCodec.*
    def extract(label: String): Option[Attributes] =
      io.circe.parser.decode[Attributes](label).toOption
  }

  private final class Plain(key: AttributeKey[String]) extends AttributesExtractor {
    def extract(label: String): Option[Attributes] =
      Some(Attributes(key(label)))
  }
}
