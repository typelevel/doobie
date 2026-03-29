# Tracing with otel4s

In this chapter we discuss the `doobie-otel4s` module, which adds OpenTelemetry tracing to doobie
through [otel4s](https://github.com/typelevel/otel4s).

## Adding the Dependency

@@@ vars

```scala
libraryDependencies += "org.tpolecat" %% "doobie-otel4s" % "$version$"
```

@@@

In addition to `doobie-otel4s` you will need:

- an `otel4s` SDK setup in your application
- a `TracerProvider[F]`, which `doobie-otel4s` uses to create spans
- a `Transactor[F]` that you want to wrap

## Setting Up

Tracing is enabled by wrapping a normal doobie transactor with `TracedTransactor.create`.

```scala mdoc:silent
import cats.effect.{Async, IO, LiftIO}
import doobie._
import doobie.implicits._
import doobie.otel4s._
import org.typelevel.otel4s.trace.TracerProvider
import org.typelevel.otel4s.semconv.attributes.DbAttributes

object Main {

  def tracedTransactor[F[_] : Async : TracerProvider : LiftIO]: F[Transactor[F]] = {
    val base =
      Transactor.fromDriverManager[F](
        driver = "org.postgresql.Driver",
        url = "jdbc:postgresql:app",
        user = "postgres",
        password = "password",
        logHandler = None
      )

    val config =
      TracingConfig.recommended(
        dbSystemName = DbAttributes.DbSystemNameValue.Postgresql,
        dbNamespace = "app"
      )

    TracedTransactor.create(base, config, logHandler = None)
  }

  def program(implicit tp: TracerProvider[IO]): IO[Unit] =
    tracedTransactor[IO].flatMap { xa =>
      sql"select 42".query[Int].unique.transact(xa).void
    }
}
```

With `TracingConfig.recommended(...)`, spans include:

- `db.system.name`
- `db.namespace`
- `db.operation.name`
- `db.query.text` only for parameterized statements

By default, span names come from `db.query.summary` when it is present. Otherwise the interpreter uses JDBC operation
names such as `executeQuery` or `executeUpdate`.

## How It Works

For each traced operation the interpreter builds span data in this order:

1. Start from `TracingConfig.constAttributes`.
2. Apply `TracingConfig.captureQuery` to decide whether to add:
    - `db.query.text`
    - `db.query.parameter.*`
    - `db.operation.batch.size`
3. Read the doobie label, if there is one, and pass it to `TracingConfig.attributesExtractor`.
4. Analyze the raw SQL with `TracingConfig.queryAnalyzer`.
5. Compute an optional custom span name with `TracingConfig.spanNamer`.
6. If no custom name is produced, fall back to JDBC operation names.

The main extension points are:

- `AttributesExtractor`, which reads doobie labels
- `QueryAnalyzer`, which reads SQL text
- `SpanNamer`, which chooses the final span name

## Recommended Configuration

`TracingConfig.recommended(dbSystemName, dbNamespace)` provides a default configuration.

It configures:

- `tracerScopeName = "doobie"`
- `defaultSpanName = "doobie:exec"`
- `constAttributes = db.system.name + db.namespace`
- `captureQuery = QueryCaptureConfig.recommended`
- `attributesExtractor = AttributesExtractor.json`
- `queryAnalyzer = QueryAnalyzer.noop`
- `spanNamer = SpanNamer.fromAttribute(DbAttributes.DbQuerySummary)`

In practice this means:

- plain doobie queries are traced immediately
- query text is only recorded for parameterized queries
- parameters are not recorded
- if you use the otel4s syntax helpers described below, the `db.query.summary` attribute becomes the span name

## Query Annotations

The module provides helpers on `Fragment` for attaching tracing metadata to queries and updates.

```scala mdoc:silent
import doobie._
import doobie.implicits._
import doobie.otel4s.syntax.fragment._
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.semconv.attributes.DbAttributes

def byId(id: Int) =
  sql"select name from person where id = $id"
    .queryWithSummary[String]("select person by id")

def insertPerson(name: String, age: Int) =
  sql"insert into person(name, age) values ($name, $age)"
    .updateWithAttributes(
      DbAttributes.DbQuerySummary("insert person"),
      Attribute("app.operation", "create-person")
    )
```

These helpers encode attributes into the doobie label. The default extractor, `AttributesExtractor.json`, decodes that
label back into `Attributes` during tracing.

The available helpers are:

- `queryWithSummary` - attaches `db.query.summary` attribute
- `updateWithSummary` - attaches `db.query.summary` attribute
- `queryWithAttributes` - attaches one or more tracing attributes
- `updateWithAttributes` - attaches one or more tracing attributes

## `AttributesExtractor.json`

The syntax helpers above rely on `AttributesExtractor.json`.

If you replace the extractor with something incompatible, for example `AttributesExtractor.plain(...)`, the helpers
still compile, but their encoded label payload is no longer decoded, so:

- `db.query.summary` is not recovered from the label
- `SpanNamer.fromAttribute(...)` will not find it
- span naming falls back to whatever other strategy you configured

This can be useful, but it should be explicit.

## Query Capture Policies

Query capture is controlled by `QueryCaptureConfig`.

```scala mdoc:silent
import doobie.otel4s.QueryCaptureConfig
import doobie.otel4s.QueryCaptureConfig.{QueryParametersPolicy, QueryTextPolicy}

val captureNothing =
  QueryCaptureConfig.disabled

val captureRecommended =
  QueryCaptureConfig.recommended

val captureEverything =
  QueryCaptureConfig(
    queryTextPolicy = QueryTextPolicy.Always,
    queryParametersPolicy = QueryParametersPolicy.All
  )
```

The policies are:

- `QueryTextPolicy.None` - does not record `db.query.text`
- `QueryTextPolicy.ParameterizedOnly` - records `db.query.text` only for statements with bound parameters
- `QueryTextPolicy.Always` - records `db.query.text` for all statements
- `QueryParametersPolicy.None` - does not record bound parameters
- `QueryParametersPolicy.NonBatchOnly` - records parameters only for non-batch operations
- `QueryParametersPolicy.All` - records parameters for both non-batch and batch operations

### Notes

- `recommended` captures query text only for parameterized statements and does not record parameters
- `QueryTextPolicy.Always` for `db.query.text` is only appropriate if raw SQL is acceptable from a privacy and
  cardinality perspective
- parameter capture is usually only appropriate in controlled environments, because values often contain sensitive
  information or high-cardinality data
- `QueryParametersPolicy.All` should be used with care, especially for batch operations

## Span Naming

`SpanNamer` controls how a traced operation gets its final name.

The built-in strategies are:

- `SpanNamer.noop` - does not provide a custom span name
- `SpanNamer.fromQueryLabel` - uses the raw doobie label as the span name
- `SpanNamer.fromAttribute(key)` - uses the value of the given extracted attribute as the span name
- `SpanNamer.fromQueryMetadata` - builds the span name from [QueryAnalyzer.QueryMetadata](#sql-analysis-with-queryanalyzer)

### `SpanNamer.fromAttribute`

This is the default strategy used by `TracingConfig.recommended(...)`.

```scala mdoc:silent
import doobie.otel4s.SpanNamer
import org.typelevel.otel4s.semconv.attributes.DbAttributes

val namer = SpanNamer.fromAttribute(DbAttributes.DbQuerySummary)
```

It checks extracted attributes for the given key.

- if the attribute exists, its value becomes the span name
- if it does not exist, it returns `None`
- if attribute extraction failed, it also returns `None`

When it returns `None`, the interpreter falls back to JDBC operation names.

### `SpanNamer.fromQueryMetadata`

This strategy builds names from [QueryAnalyzer.QueryMetadata](#sql-analysis-with-queryanalyzer).

Its precedence is:

1. `querySummary`
2. `operationName + collectionName`
3. `operationName + storedProcedureName`
4. `operationName`
5. `collectionName` or `storedProcedureName`

Use this when span names should come from SQL analysis rather than from doobie labels.

### Composing Naming Strategies

`SpanNamer` instances compose with `orElse`.

```scala mdoc:silent:reset
import doobie.otel4s.SpanNamer
import org.typelevel.otel4s.AttributeKey
import org.typelevel.otel4s.semconv.attributes.DbAttributes

val legacyLabelKey = AttributeKey[String]("db.query.legacy_label")

val namer =
  SpanNamer
    .fromAttribute(DbAttributes.DbQuerySummary)
    .orElse(SpanNamer.fromQueryMetadata)
    .orElse(SpanNamer.fromAttribute(legacyLabelKey))
    .orElse(SpanNamer.fromQueryLabel)
```

This lets you define naming precedence explicitly.

## Customizing the Extractor

`AttributesExtractor` turns the raw doobie label into typed attributes.

The built-ins are:

- `AttributesExtractor.json`
- `AttributesExtractor.plain(key)`

`plain(key)` stores the label as a single string attribute:

```scala mdoc:silent
import doobie.otel4s.AttributesExtractor
import org.typelevel.otel4s.AttributeKey

val extractor =
  AttributesExtractor.plain(AttributeKey[String]("db.query.label"))
```

This is useful when:

- you already use human-readable labels
- you want labels preserved as attributes
- you do not use the JSON-encoded helper syntax

Like `SpanNamer`, extractors compose with `orElse`.

```scala mdoc:silent:reset
import doobie.otel4s.AttributesExtractor
import org.typelevel.otel4s.AttributeKey

val legacyLabelKey = AttributeKey[String]("db.query.legacy_label")

val extractor =
  AttributesExtractor
    .json
    .orElse(AttributesExtractor.plain(legacyLabelKey))
```

This means:

- first try to decode a helper-produced JSON payload
- if that fails, keep the raw label as a fallback attribute

## Customizing `TracingConfig`

All behavior is configured through `TracingConfig`.

```scala mdoc:silent
import doobie.otel4s._
import org.typelevel.otel4s.{Attribute, Attributes}
import org.typelevel.otel4s.semconv.attributes.DbAttributes

val config =
  TracingConfig
    .recommended(
      dbSystemName = DbAttributes.DbSystemNameValue.Postgresql,
      dbNamespace = "app"
    )
    .withTracerScopeName("com.example.doobie")
    .withDefaultSpanName("db.exec")
    .withCaptureQuery(QueryCaptureConfig.disabled)
    .withAttributesExtractor(AttributesExtractor.json)
    .withQueryAnalyzer(QueryAnalyzer.noop)
    .withSpanNamer(SpanNamer.fromAttribute(DbAttributes.DbQuerySummary))
    .addConstAttributes(Attributes(Attribute("system.module", "payments")))
```

The main customization points are:

- `withTracerScopeName` - changes the tracer scope name used for spans created by doobie
- `withDefaultSpanName` - changes the initial span name used before JDBC operation naming is applied
- `withConstAttributes` - replaces the set of attributes added to every span
- `addConstAttributes` - appends more attributes to the existing constant attributes
- `withCaptureQuery` - changes how SQL text and parameters are recorded
- `withAttributesExtractor` - changes how doobie labels are decoded into attributes
- `withQueryAnalyzer` - changes how SQL text is analyzed into query metadata
- `withSpanNamer` - changes how the final span name is chosen

## New Syntax and Legacy Labels Together

When migrating existing code it is common to have both of these:

- new code using `queryWithSummary` or `queryWithAttributes`
- old code using raw doobie labels such as `queryWithLabel("find-user")`

In this case, use a composed extractor and a composed namer.

```scala mdoc:silent:reset
import doobie.otel4s._
import org.typelevel.otel4s.AttributeKey
import org.typelevel.otel4s.semconv.attributes.DbAttributes

val legacyLabelKey = AttributeKey[String]("db.query.legacy_label")

val config =
  TracingConfig
    .recommended(
      dbSystemName = DbAttributes.DbSystemNameValue.Postgresql,
      dbNamespace = "app"
    )
    .withAttributesExtractor(
      AttributesExtractor
        .json
        .orElse(AttributesExtractor.plain(legacyLabelKey))
    )
    .withSpanNamer(
      SpanNamer
        .fromAttribute(DbAttributes.DbQuerySummary)
        .orElse(SpanNamer.fromAttribute(legacyLabelKey))
    )
```

With this setup:

- helper-generated summaries still win
- raw legacy labels remain visible
- unlabeled queries still fall back to JDBC operation names

## SQL Analysis with `QueryAnalyzer`

`QueryAnalyzer` is a separate extension point for deriving structured metadata from SQL text.

It returns `QueryAnalyzer.QueryMetadata`, which may contain:

- `queryText`
- `operationName`
- `collectionName`
- `storedProcedureName`
- `querySummary`

Unlike `AttributesExtractor`, which works from labels, `QueryAnalyzer` works from the SQL statement itself.

```scala mdoc:silent:reset
import doobie.otel4s.QueryAnalyzer

val simpleAnalyzer =
  new QueryAnalyzer {
    def analyze(sql: String): Option[QueryAnalyzer.QueryMetadata] =
      if (sql.trim.toLowerCase.startsWith("select"))
        Some(
          QueryAnalyzer.QueryMetadata(
            queryText = Some(sql),
            operationName = Some("SELECT"),
            collectionName = None,
            storedProcedureName = None,
            querySummary = Some("SELECT")
          )
        )
      else None
  }
```

Analyzers also compose with `orElse`, so you can define a fallback chain.

> The default analyzer is `QueryAnalyzer.noop`, which does not produce any metadata. If you want SQL analysis, a common
> choice is [SqlQueryAnalyzer](#opentelemetry-java-instrumentation) from OpenTelemetry Java Instrumentation.

## Using `QueryAnalyzer` for Span Naming

To use SQL analysis for naming, pair the analyzer with `SpanNamer.fromQueryMetadata`.

```scala mdoc:silent
import doobie.otel4s._
import org.typelevel.otel4s.semconv.attributes.DbAttributes

val config =
  TracingConfig
    .recommended(
      dbSystemName = DbAttributes.DbSystemNameValue.Postgresql,
      dbNamespace = "app"
    )
    .withQueryAnalyzer(simpleAnalyzer)
    .withSpanNamer(
      SpanNamer
        .fromQueryMetadata
        .orElse(SpanNamer.fromAttribute(DbAttributes.DbQuerySummary))
    )
```

With this configuration:

- SQL-derived metadata is used first
- then label-derived `db.query.summary`
- finally JDBC operation names

## OpenTelemetry Java Instrumentation

If your application already depends on OpenTelemetry Java instrumentation, the `SqlQueryAnalyzer` can be adapted to
doobie's `QueryAnalyzer`.

This is useful when:

- you already use OpenTelemetry Java agent or instrumentation libraries elsewhere
- you want query summaries to match the logic used by OpenTelemetry Java instrumentation
- you want `db.query.summary`-based naming without hard-coding summaries on every query

Wrap the Java analyzer and convert `null` values into `Option`.

```scala mdoc:silent:reset
import doobie.otel4s._
import io.opentelemetry.instrumentation.api.incubator.semconv.db.{SqlDialect, SqlQuery, SqlQueryAnalyzer}
import org.typelevel.otel4s.semconv.attributes.DbAttributes

def fromOtelJava(delegate: SqlQueryAnalyzer): QueryAnalyzer =
  new QueryAnalyzer {
    private val dialect = SqlDialect.DOUBLE_QUOTES_ARE_STRING_LITERALS

    def analyze(sql: String): Option[QueryAnalyzer.QueryMetadata] =
      Option(delegate.analyzeWithSummary(sql, dialect)).map { q: SqlQuery =>
        QueryAnalyzer.QueryMetadata(
          queryText = Option(q.getQueryText),
          operationName = Option(q.getOperationName),
          collectionName = Option(q.getCollectionName),
          storedProcedureName = Option(q.getStoredProcedureName),
          querySummary = Option(q.getQuerySummary)
        )
      }
  }

val sqlQueryAnalyzer: SqlQueryAnalyzer = SqlQueryAnalyzer.create(true)

val config: TracingConfig =
  TracingConfig
    .recommended(
      dbSystemName = DbAttributes.DbSystemNameValue.Postgresql,
      dbNamespace = "app"
    )
    .withQueryAnalyzer(fromOtelJava(sqlQueryAnalyzer))
    .withSpanNamer(
      SpanNamer
        .fromQueryMetadata
        .orElse(SpanNamer.fromAttribute(DbAttributes.DbQuerySummary))
    )
```

### Naming Order

A common ordering is:

```scala mdoc:silent
SpanNamer
  .fromQueryMetadata
  .orElse(SpanNamer.fromAttribute(DbAttributes.DbQuerySummary))
```

This means:

- use the analyzer-derived summary first
- if the analyzer does not recognize the query, fall back to explicit query summaries attached via doobie syntax helpers

If you want manual summaries to override analyzer results, reverse the order.

## Caveats

- unlabeled queries are still traced; they just rely on SQL analysis or JDBC fallback naming
- `AttributesExtractor.json` only works for labels encoded as JSON attributes
- `QueryAnalyzer` does not currently add analyzed fields as span attributes by itself; it feeds naming through
  `SpanNamer`
- capturing parameters can expose secrets, tokens, emails, IDs, or other sensitive values
- high-cardinality span names are usually a bad idea; prefer summaries such as `SELECT users` over raw SQL
