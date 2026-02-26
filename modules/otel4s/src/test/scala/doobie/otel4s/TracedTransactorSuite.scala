// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import cats.effect.IO
import doobie.Transactor
import doobie.Update
import doobie.otel4s.syntax.fragment.*
import doobie.syntax.all.*
import io.opentelemetry.api.trace.{SpanKind, StatusCode}
import io.opentelemetry.sdk.resources.Resource as OTelResource
import io.opentelemetry.sdk.trace.data.{EventData, SpanData, StatusData}
import munit.TestOptions
import org.typelevel.otel4s.oteljava.AttributeConverters.*
import org.typelevel.otel4s.oteljava.testkit.trace.TracesTestkit
import org.typelevel.otel4s.oteljava.testkit.{InstrumentationScope, TelemetryResource}
import org.typelevel.otel4s.semconv.attributes.{DbAttributes, ErrorAttributes, ExceptionAttributes}
import org.typelevel.otel4s.trace.TracerProvider
import org.typelevel.otel4s.{Attribute, Attributes}

import scala.jdk.CollectionConverters.*

class TracedTransactorSuite extends munit.CatsEffectSuite {
  import QueryCaptureConfig.{QueryParametersPolicy, QueryTextPolicy}

  private val xa = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:otel4s;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  private val telemetryResource = TelemetryResource(Attributes.empty)
  private val doobieInstrumentationScope = InstrumentationScope(
    name = "doobie",
    version = Some(doobie.buildinfo.version),
    schemaUrl = None,
    attributes = Attributes.empty
  )

  testkitTest("record db operation name for prepared statements") { testkit =>
    val expected = List(
      Span(
        name = "executeQuery",
        attributes = Attributes(
          DbAttributes.DbOperationName("executeQuery")
        )
      )
    )

    for {
      tx <- testkit.tracedTransactor(tracedConfig())
      _ <- sql"select 1".query[Int].unique.transact(tx)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  testkitTest("record constant attributes") { testkit =>
    val attributes = Attributes(
      Attribute("a", 1L),
      Attribute("b", "b")
    )

    val expected = List(
      Span(
        name = "executeQuery",
        attributes = Attributes(
          DbAttributes.DbOperationName("executeQuery")
        ) ++ attributes
      )
    )

    for {
      tx <- testkit.tracedTransactor(tracedConfig(constAttributes = attributes))
      _ <- sql"select 1".query[Int].unique.transact(tx)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  testkitTest("record db operation outcome in case of an error") { testkit =>
    val expected = List(
      Span(
        name = "executeQuery",
        attributes = Attributes(
          ErrorAttributes.ErrorType("org.h2.jdbc.JdbcSQLDataException"),
          DbAttributes.DbResponseStatusCode("22018"),
          DbAttributes.DbOperationName("executeQuery")
        ),
        status = StatusData.create(StatusCode.ERROR, "Data conversion error converting \"text\" [22018-240]"),
        events = List(
          Event(
            name = "exception",
            attributes = Attributes(
              ExceptionAttributes.ExceptionType("org.h2.jdbc.JdbcSQLDataException"),
              ExceptionAttributes.ExceptionMessage("Data conversion error converting \"text\" [22018-240]")
            )
          )
        )
      )
    )

    for {
      tx <- testkit.tracedTransactor(tracedConfig())
      _ <- sql"select 'text'".query[Int].unique.transact(tx).attempt.assert(_.isLeft)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  testkitTest("properly trace operations that happen within the same transaction") { testkit =>
    val expected = List(
      Span(
        name = "executeQuery",
        attributes = Attributes(
          DbAttributes.DbQueryText("select 1"),
          DbAttributes.DbOperationName("executeQuery")
        )
      ),
      Span(
        name = "executeQuery",
        attributes = Attributes(
          DbAttributes.DbQueryText("select 2"),
          DbAttributes.DbOperationName("executeQuery")
        )
      )
    )

    val config = tracedConfig(
      captureQuery = QueryCaptureConfig(
        queryTextPolicy = QueryTextPolicy.Always,
        queryParametersPolicy = QueryParametersPolicy.None
      )
    )

    for {
      tx <- testkit.tracedTransactor(config)
      _ <- (for {
        _ <- sql"select 1".query[Int].unique
        _ <- sql"select 2".query[Int].unique
      } yield ()).transact(tx)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  testkitTest("capture query text and parameters when enabled") { testkit =>
    val expected = List(
      Span(
        name = "executeQuery",
        attributes = Attributes(
          DbAttributes.DbQueryText("select ?"),
          DbAttributes.DbOperationName("executeQuery"),
          Attribute("db.query.parameter.0", "1")
        )
      )
    )

    val config = tracedConfig(
      captureQuery = QueryCaptureConfig(
        queryTextPolicy = QueryTextPolicy.Always,
        queryParametersPolicy = QueryParametersPolicy.All
      )
    )

    for {
      tx <- testkit.tracedTransactor(config)
      _ <- sql"select ${1}".query[Int].unique.transact(tx)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  testkitTest("record db.operation.batch.size for batch operations") { testkit =>
    val expected = List(
      Span(
        name = "executeUpdate",
        attributes = Attributes(
          DbAttributes.DbQueryText("CREATE LOCAL TEMPORARY TABLE TEST_BATCH (int_value INT)"),
          DbAttributes.DbOperationName("executeUpdate")
        )
      ),
      Span(
        name = "executeBatch",
        attributes = Attributes(
          DbAttributes.DbQueryText("insert into TEST_BATCH (int_value) values (?)"),
          DbAttributes.DbOperationBatchSize(3L),
          DbAttributes.DbOperationName("executeBatch")
        )
      )
    )

    val config = tracedConfig(
      captureQuery = QueryCaptureConfig(
        queryTextPolicy = QueryTextPolicy.Always,
        queryParametersPolicy = QueryParametersPolicy.None
      )
    )

    for {
      tx <- testkit.tracedTransactor(config)
      _ <- (for {
        _ <- sql"CREATE LOCAL TEMPORARY TABLE TEST_BATCH (int_value INT)".update.run
        _ <- Update[Int]("insert into TEST_BATCH (int_value) values (?)")
          .updateMany(List(1, 2, 3))
      } yield ()).transact(tx)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  testkitTest("semconv config captures query text only for parameterized queries") { testkit =>
    val expected = List(
      Span(
        name = "executeQuery",
        attributes = Attributes(
          DbAttributes.DbSystemName(DbAttributes.DbSystemNameValue.Postgresql),
          DbAttributes.DbNamespace("doobie"),
          DbAttributes.DbQueryText("select ?"),
          DbAttributes.DbOperationName("executeQuery")
        )
      )
    )

    val config =
      TracingConfig.recommended(
        dbSystemName = DbAttributes.DbSystemNameValue.Postgresql,
        dbNamespace = "doobie"
      )

    for {
      tx <- testkit.tracedTransactor(config)
      _ <- sql"select ${1}".query[Int].unique.transact(tx)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  testkitTest("semconv config does not capture query text for non-parameterized queries") { testkit =>
    val expected = List(
      Span(
        name = "executeQuery",
        attributes = Attributes(
          DbAttributes.DbSystemName(DbAttributes.DbSystemNameValue.Postgresql),
          DbAttributes.DbNamespace("doobie"),
          DbAttributes.DbOperationName("executeQuery")
        )
      )
    )

    val config =
      TracingConfig.recommended(
        dbSystemName = DbAttributes.DbSystemNameValue.Postgresql,
        dbNamespace = "doobie"
      )

    for {
      tx <- testkit.tracedTransactor(config)
      _ <- sql"select 1".query[Int].unique.transact(tx)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  testkitTest("capture explicit summary with default parser and span namer") { testkit =>
    val summary = "summary via syntax"

    val expected = List(
      Span(
        name = summary,
        attributes = Attributes(
          DbAttributes.DbOperationName("executeQuery"),
          DbAttributes.DbQuerySummary(summary)
        )
      ),
      Span(
        name = summary,
        attributes = Attributes(
          DbAttributes.DbOperationName("executeUpdate"),
          DbAttributes.DbQuerySummary(summary)
        )
      )
    )

    for {
      tx <- testkit.tracedTransactor(tracedConfig())
      _ <- sql"select 1".queryWithSummary[Int](summary).unique.transact(tx)
      _ <- sql"CREATE LOCAL TEMPORARY TABLE TEST (int_value INT)".updateWithSummary(summary).run.transact(tx)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  testkitTest("capture explicit attributes with default parser") { testkit =>
    val attrs = Attributes(Attribute("test.attr", "ok"))

    val expected = List(
      Span(
        name = "executeQuery",
        attributes = Attributes(
          DbAttributes.DbOperationName("executeQuery")
        ) ++ attrs
      ),
      Span(
        name = "executeUpdate",
        attributes = Attributes(
          DbAttributes.DbOperationName("executeUpdate")
        ) ++ attrs
      )
    )

    for {
      tx <- testkit.tracedTransactor(tracedConfig())
      _ <- sql"select 1".queryWithAttributes[Int](attrs).unique.transact(tx)
      _ <- sql"CREATE LOCAL TEMPORARY TABLE TEST (int_value INT)".updateWithAttributes(attrs).run.transact(tx)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  testkitTest("use custom span namer") { testkit =>
    val config = tracedConfig().withSpanNamer(new SpanNamer {
      def spanName(context: SpanNamer.Context): Option[String] =
        context.attributes
          .flatMap(_.get(DbAttributes.DbQuerySummary).map(_.value))
          .map(summary => s"sql:$summary")
    })

    val summary = "named"
    val expected = List(
      Span(
        name = "sql:named",
        attributes = Attributes(
          DbAttributes.DbOperationName("executeQuery"),
          DbAttributes.DbQuerySummary(summary)
        )
      )
    )

    for {
      tx <- testkit.tracedTransactor(config)
      _ <- sql"select 1".queryWithSummary[Int](summary).unique.transact(tx)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  testkitTest("ignore plain fragment labels by default") { testkit =>
    val label = "some label"

    val expected = List(
      Span(
        name = "executeQuery",
        attributes = Attributes(
          DbAttributes.DbOperationName("executeQuery")
        )
      ),
      Span(
        name = "executeUpdate",
        attributes = Attributes(
          DbAttributes.DbOperationName("executeUpdate")
        )
      )
    )

    for {
      tx <- testkit.tracedTransactor(tracedConfig())
      _ <- sql"select 1".queryWithLabel[Int](label).unique.transact(tx)
      _ <- sql"CREATE LOCAL TEMPORARY TABLE TEST (int_value INT)".updateWithLabel(label).run.transact(tx)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  testkitTest("capture fragment attributes") { testkit =>
    val attrs = Attributes(Attribute("test.attr", "ok"))

    val expected = List(
      Span(
        name = "executeQuery",
        attributes = Attributes(
          DbAttributes.DbOperationName("executeQuery")
        ) ++ attrs
      ),
      Span(
        name = "executeUpdate",
        attributes = Attributes(
          DbAttributes.DbOperationName("executeUpdate")
        ) ++ attrs
      )
    )

    for {
      tx <- testkit.tracedTransactor(tracedConfig())
      _ <- sql"select 1".queryWithAttributes[Int](attrs).unique.transact(tx)
      _ <- sql"CREATE LOCAL TEMPORARY TABLE TEST (int_value INT)".updateWithAttributes(attrs).run.transact(tx)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  testkitTest("capture fragment attributes and use summary as span name") { testkit =>
    val summary = "the summary"
    val attrs = Attributes(
      DbAttributes.DbQuerySummary(summary),
      Attribute("test.attr", "ok")
    )

    val expected = List(
      Span(
        name = summary,
        attributes = Attributes(
          DbAttributes.DbOperationName("executeQuery")
        ) ++ attrs
      ),
      Span(
        name = summary,
        attributes = Attributes(
          DbAttributes.DbOperationName("executeUpdate")
        ) ++ attrs
      )
    )

    for {
      tx <- testkit.tracedTransactor(tracedConfig())
      _ <- sql"select 1".queryWithAttributes[Int](attrs).unique.transact(tx)
      _ <- sql"CREATE LOCAL TEMPORARY TABLE TEST (int_value INT)".updateWithAttributes(attrs).run.transact(tx)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  testkitTest("ignore plain fragment labels when parser cannot parse") { testkit =>
    val label = "some label"

    val expected = List(
      Span(
        name = "executeQuery",
        attributes = Attributes(
          DbAttributes.DbOperationName("executeQuery")
        )
      ),
      Span(
        name = "executeUpdate",
        attributes = Attributes(
          DbAttributes.DbOperationName("executeUpdate")
        )
      )
    )

    for {
      tx <- testkit.tracedTransactor(tracedConfig())
      _ <- sql"select 1".queryWithLabel[Int](label).unique.transact(tx)
      _ <- sql"CREATE LOCAL TEMPORARY TABLE TEST (int_value INT)".updateWithLabel(label).run.transact(tx)
      _ <- testkit.finishedSpans.assertEquals(expected)
    } yield ()
  }

  private def testkitTest(name: TestOptions)(f: Testkit => IO[Unit])(implicit loc: munit.Location): Unit =
    test(name) {
      TracesTestkit
        .builder[IO]
        .addTracerProviderCustomizer(_.setResource(OTelResource.empty()))
        .build
        .use(testkit => f(new Testkit(testkit)))
    }

  private def tracedConfig(
      constAttributes: Attributes = Attributes.empty,
      captureQuery: QueryCaptureConfig = QueryCaptureConfig.disabled
  ): TracingConfig =
    TracingConfig(
      tracerScopeName = "doobie",
      defaultSpanName = "doobie:exec",
      constAttributes = constAttributes,
      captureQuery = captureQuery
    )

  private class Testkit(testkit: TracesTestkit[IO]) {
    implicit val tracerProvider: TracerProvider[IO] = testkit.tracerProvider

    def tracedTransactor(config: TracingConfig): IO[Transactor[IO]] =
      TracedTransactor.create(xa, config, None)

    def finishedSpans: IO[List[Span]] =
      testkit.finishedSpans[SpanData].map(_.map(Span.from))
  }

  case class Event(
      name: String,
      attributes: Attributes
  )

  object Event {

    // exclude exception stacktrace from event attributes to simplify comparison
    def from(event: EventData): Event =
      Event(
        event.getName,
        event.getAttributes.toScala.filter(_.key != ExceptionAttributes.ExceptionStacktrace)
      )

  }

  case class Span(
      name: String,
      attributes: Attributes,
      resource: TelemetryResource = telemetryResource,
      scope: InstrumentationScope = doobieInstrumentationScope,
      kind: SpanKind = SpanKind.CLIENT,
      status: StatusData = StatusData.unset(),
      events: List[Event] = Nil
  )

  object Span {

    def from(spanData: SpanData): Span =
      Span(
        name = spanData.getName,
        attributes = spanData.getAttributes.toScala,
        resource = TelemetryResource(spanData.getResource),
        scope = InstrumentationScope(spanData.getInstrumentationScopeInfo),
        kind = spanData.getKind,
        status = spanData.getStatus,
        events = spanData.getEvents.asScala.toList.map(Event.from)
      )
  }

}
