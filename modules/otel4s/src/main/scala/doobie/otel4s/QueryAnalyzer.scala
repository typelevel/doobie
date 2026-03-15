// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.otel4s

/** Analyzes SQL text and returns semantic query metadata.
  *
  * This abstraction is intentionally implementation-agnostic. Users can plug in their analyzer of choice (for example,
  * an adapter around OpenTelemetry Java instrumentation) and keep doobie tracing integration pure at the boundary.
  *
  * In particular, analyzers may derive `querySummary` suitable for `db.query.summary` span naming/cardinality rules.
  *
  * @see
  *   [[https://opentelemetry.io/docs/specs/semconv/db/database-spans/#generating-a-summary-of-the-query]]
  */
trait QueryAnalyzer { self =>

  /** Analyze SQL and return semantic query metadata when available. */
  def analyze(sql: String): Option[QueryAnalyzer.QueryMetadata]

  /** Compose with a fallback analyzer that is tried when this analyzer returns `None`. */
  def orElse(fallback: QueryAnalyzer): QueryAnalyzer =
    new QueryAnalyzer {
      def analyze(sql: String): Option[QueryAnalyzer.QueryMetadata] =
        self.analyze(sql).orElse(fallback.analyze(sql))
    }
}

object QueryAnalyzer {

  /** A no-op analyzer that never produces analysis. */
  val noop: QueryAnalyzer = Noop

  /** Query metadata. All fields are optional because analyzers may produce partial information.
    */
  sealed trait QueryMetadata {

    /** Original SQL text (possibly normalized or sanitized). */
    def queryText: Option[String]

    /** Operation verb, for example `SELECT`, `INSERT`, or `CALL`. */
    def operationName: Option[String]

    /** Target table name when applicable. */
    def collectionName: Option[String]

    /** Stored procedure name for procedure-style operations. */
    def storedProcedureName: Option[String]

    /** Low-cardinality summary suitable for `db.query.summary` span naming. */
    def querySummary: Option[String]
  }

  object QueryMetadata {

    /** Creates query metadata from the provided fields.
      *
      * @example
      *   {{{
      * QueryAnalyzer.QueryMetadata(
      *   queryText = Some("select * from users where id = ?"),
      *   operationName = Some("SELECT"),
      *   collectionName = Some("users"),
      *   storedProcedureName = None,
      *   querySummary = Some("SELECT users")
      * )
      *   }}}
      *
      * @example
      *   {{{
      * QueryAnalyzer.QueryMetadata(
      *   queryText = Some("CALL refresh_cache(?)"),
      *   operationName = Some("CALL"),
      *   collectionName = None,
      *   storedProcedureName = Some("refresh_cache"),
      *   querySummary = Some("CALL refresh_cache")
      * )
      *   }}}
      */
    def apply(
        queryText: Option[String],
        operationName: Option[String],
        collectionName: Option[String],
        storedProcedureName: Option[String],
        querySummary: Option[String]
    ): QueryMetadata =
      QueryMetadataImpl(
        queryText = queryText,
        operationName = operationName,
        collectionName = collectionName,
        storedProcedureName = storedProcedureName,
        querySummary = querySummary
      )

    private final case class QueryMetadataImpl(
        queryText: Option[String],
        operationName: Option[String],
        collectionName: Option[String],
        storedProcedureName: Option[String],
        querySummary: Option[String]
    ) extends QueryMetadata
  }

  private object Noop extends QueryAnalyzer {
    def analyze(sql: String): Option[QueryMetadata] = None
  }
}
