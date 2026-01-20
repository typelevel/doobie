// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import cats.effect.IO
import doobie.Transactor
import doobie.syntax.all.*
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.sdk.resources.Resource as OTelResource
import io.opentelemetry.sdk.trace.data.{EventData, SpanData}
import org.typelevel.otel4s.oteljava.testkit.{InstrumentationScope, TelemetryResource}
import org.typelevel.otel4s.{Attribute, Attributes}
import org.typelevel.otel4s.oteljava.testkit.trace.TracesTestkit
import org.typelevel.otel4s.semconv.attributes.{DbAttributes, ExceptionAttributes}
import org.typelevel.otel4s.trace.TracerProvider

class TracedTransactorSuite extends munit.CatsEffectSuite {

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
    version = None,
    schemaUrl = None,
    attributes = Attributes.empty
  )
  private val testInstrumentationScope = InstrumentationScope(
    name = "test",
    version = None,
    schemaUrl = None,
    attributes = Attributes.empty
  )

  test("properly link traces") {
    TracesTestkit.inMemory[IO](_.setResource(OTelResource.empty())).use { testkit =>
      implicit val tracerProvider: TracerProvider[IO] = testkit.tracerProvider

      val intValue = 1
      val stringValue = "string"

      val conn =
        for {
          _ <- sql"select 1".query[Int].unique
          _ <- sql"CREATE LOCAL TEMPORARY TABLE TEST (int_value INT)".updateWithLabel("create table").run
          _ <- sql"insert into TEST (int_value) values ($intValue)".updateWithLabel("update user info").run
        } yield ()

      def expected(traceId: String) = List(
        Span(
          name = "executeQuery",
          traceId = traceId,
          attributes = Attributes(
            Attribute("label", "unlabeled"),
            Attribute(DbAttributes.DbQueryText, "select 1"),
            Attribute(DbAttributes.DbOperationName, "executeQuery")
          )
        ),
        Span(
          name = "executeUpdate",
          traceId = traceId,
          attributes = Attributes(
            Attribute("label", "create table"),
            DbAttributes.DbQueryText("CREATE LOCAL TEMPORARY TABLE TEST (int_value INT)"),
            DbAttributes.DbOperationName("executeUpdate")
          )
        ),
        Span(
          name = "executeUpdate",
          traceId = traceId,
          attributes = Attributes(
            Attribute("db.query.parameter.0", "1"),
            Attribute("label", "update user info"),
            DbAttributes.DbQueryText("insert into TEST (int_value) values (?)"),
            DbAttributes.DbOperationName("executeUpdate")
          )
        ),
        Span(
          name = "executeQuery",
          traceId = traceId,
          attributes = Attributes(
            DbAttributes.DbResponseStatusCode("22018"),
            DbAttributes.DbQueryText("select ?"),
            Attribute("label", "unlabeled"),
            DbAttributes.DbOperationName("executeQuery"),
            Attribute("db.query.parameter.0", "string")
          ),
          events = List(
            Event(
              name = "exception",
              attributes = Attributes(
                ExceptionAttributes.ExceptionType("org.h2.jdbc.JdbcSQLDataException"),
                ExceptionAttributes.ExceptionMessage("Data conversion error converting \"string\" [22018-240]")
              )
            )
          )
        ),
        Span(
          name = "test",
          traceId = traceId,
          attributes = Attributes.empty,
          scope = testInstrumentationScope,
          kind = SpanKind.INTERNAL
        )
      )

      for {
        tx <- TracedTransactor.create(xa, TracedInterpreter.Config.default, None)
        tracer <- tracerProvider.get("test")
        traceId <- tracer.span("test").use { span =>
          for {
            _ <- conn.transact(tx).void
            _ <- sql"select $stringValue".query[Int].unique.transact(tx).attempt
          } yield span.context.traceIdHex
        }
        spans <- testkit.finishedSpans[SpanData].map(_.map(Span.from))
        _ = spans.foreach(println)
      } yield assertEquals(spans, expected(traceId))
    }

  }

  case class Event(
      name: String,
      attributes: Attributes
  )

  object Event {
    import org.typelevel.otel4s.oteljava.AttributeConverters.*

    // exclude exception stacktrace from event attributes to simplify comparison
    def from(event: EventData): Event =
      Event(
        event.getName,
        event.getAttributes.toScala.filter(_.key != ExceptionAttributes.ExceptionStacktrace)
      )

  }
  case class Span(
      name: String,
      traceId: String,
      attributes: Attributes,
      resource: TelemetryResource = telemetryResource,
      scope: InstrumentationScope = doobieInstrumentationScope,
      kind: SpanKind = SpanKind.CLIENT,
      events: List[Event] = Nil
  )

  object Span {
    import scala.jdk.CollectionConverters.*
    import org.typelevel.otel4s.oteljava.AttributeConverters.*

    def from(spanData: SpanData): Span =
      Span(
        name = spanData.getName,
        traceId = spanData.getTraceId,
        attributes = spanData.getAttributes.toScala,
        resource = TelemetryResource(spanData.getResource),
        scope = InstrumentationScope(spanData.getInstrumentationScopeInfo),
        kind = spanData.getKind,
        events = spanData.getEvents.asScala.toList.map(Event.from)
      )
  }

}
