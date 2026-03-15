// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.otel4s

import org.typelevel.otel4s.{Attribute, AttributeKey, Attributes}

class SpanNamerSuite extends munit.FunSuite {

  private val contextTemplate = SpanNamer.Context(
    rawLabel = "label",
    sql = "select 1",
    attributes = None,
    queryMetadata = None
  )

  test("fromQueryMetadata prefers querySummary") {
    val metadata = QueryAnalyzer.QueryMetadata(
      queryText = Some("select * from users"),
      operationName = Some("SELECT"),
      collectionName = Some("users"),
      storedProcedureName = None,
      querySummary = Some("summary")
    )

    val name = SpanNamer.fromQueryMetadata.spanName(contextWithQueryMetadata(Some(metadata)))

    assertEquals(name, Some("summary"))
  }

  test("fromQueryMetadata uses operation and collection when summary is missing") {
    val metadata = QueryAnalyzer.QueryMetadata(
      queryText = Some("select * from users"),
      operationName = Some("SELECT"),
      collectionName = Some("users"),
      storedProcedureName = None,
      querySummary = None
    )

    val name = SpanNamer.fromQueryMetadata.spanName(contextWithQueryMetadata(Some(metadata)))

    assertEquals(name, Some("SELECT users"))
  }

  test("fromQueryMetadata falls back to operation name") {
    val info = QueryAnalyzer.QueryMetadata(
      queryText = Some("select 1"),
      operationName = Some("SELECT"),
      collectionName = None,
      storedProcedureName = None,
      querySummary = None
    )

    val name = SpanNamer.fromQueryMetadata.spanName(contextWithQueryMetadata(Some(info)))

    assertEquals(name, Some("SELECT"))
  }

  test("fromQueryMetadata falls back to stored procedure when operation is missing") {
    val info = QueryAnalyzer.QueryMetadata(
      queryText = Some("call refresh_cache(?)"),
      operationName = None,
      collectionName = None,
      storedProcedureName = Some("refresh_cache"),
      querySummary = None
    )

    val name = SpanNamer.fromQueryMetadata.spanName(contextWithQueryMetadata(Some(info)))

    assertEquals(name, Some("refresh_cache"))
  }

  test("fromQueryMetadata returns none when queryMetadata is missing") {
    val name = SpanNamer.fromQueryMetadata.spanName(contextTemplate)
    assertEquals(name, None)
  }

  test("noop always returns none") {
    val context = SpanNamer.Context(
      rawLabel = "custom label",
      sql = "select * from users",
      attributes = Some(Attributes(Attribute("span.name", "value"))),
      queryMetadata = Some(
        QueryAnalyzer.QueryMetadata(
          queryText = Some("select * from users"),
          operationName = Some("SELECT"),
          collectionName = Some("users"),
          storedProcedureName = None,
          querySummary = Some("SELECT users")
        )
      )
    )

    val name = SpanNamer.noop.spanName(context)
    assertEquals(name, None)
  }

  test("fromQueryLabel uses non-empty label") {
    val name = SpanNamer.fromQueryLabel.spanName(contextWithRawLabel("read users"))
    assertEquals(name, Some("read users"))
  }

  test("fromQueryLabel returns none for unlabeled marker") {
    val name = SpanNamer.fromQueryLabel.spanName(contextWithRawLabel(org.typelevel.doobie.util.unlabeled))
    assertEquals(name, None)
  }

  test("fromQueryLabel returns none for empty label") {
    val name = SpanNamer.fromQueryLabel.spanName(contextWithRawLabel(""))
    assertEquals(name, None)
  }

  test("fromQueryLabel returns none for null label") {
    val name = SpanNamer.fromQueryLabel.spanName(contextWithRawLabel(null))
    assertEquals(name, None)
  }

  test("fromAttribute uses configured key when present") {
    val key = AttributeKey.string("span.name")
    val context = contextWithAttributes(
      Some(
        Attributes(
          Attribute("span.name", "summary from attribute"),
          Attribute("other", "ignored")
        )
      )
    )

    val name = SpanNamer.fromAttribute(key).spanName(context)
    assertEquals(name, Some("summary from attribute"))
  }

  test("fromAttribute returns none when attributes are missing") {
    val key = AttributeKey.string("span.name")
    val name = SpanNamer.fromAttribute(key).spanName(contextWithAttributes(None))
    assertEquals(name, None)
  }

  test("fromAttribute returns none when key is absent") {
    val key = AttributeKey.string("span.name")
    val context = contextWithAttributes(Some(Attributes(Attribute("different.key", "value"))))

    val name = SpanNamer.fromAttribute(key).spanName(context)
    assertEquals(name, None)
  }

  private def contextWithQueryMetadata(value: Option[QueryAnalyzer.QueryMetadata]): SpanNamer.Context =
    SpanNamer.Context(
      rawLabel = contextTemplate.rawLabel,
      sql = contextTemplate.sql,
      attributes = contextTemplate.attributes,
      queryMetadata = value
    )

  private def contextWithRawLabel(value: String): SpanNamer.Context =
    SpanNamer.Context(
      rawLabel = value,
      sql = contextTemplate.sql,
      attributes = contextTemplate.attributes,
      queryMetadata = contextTemplate.queryMetadata
    )

  private def contextWithAttributes(value: Option[Attributes]): SpanNamer.Context =
    SpanNamer.Context(
      rawLabel = contextTemplate.rawLabel,
      sql = contextTemplate.sql,
      attributes = value,
      queryMetadata = contextTemplate.queryMetadata
    )
}
