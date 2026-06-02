// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.otel4s

class QueryAnalyzerSuite extends munit.FunSuite {

  test("noop analyzer returns none") {
    val result = QueryAnalyzer.noop.analyze("select 1")
    assertEquals(result, None)
  }

  test("orElse uses fallback when primary returns none") {
    val primary = new QueryAnalyzer {
      def analyze(sql: String): Option[QueryAnalyzer.QueryMetadata] = None
    }
    val fallback = new QueryAnalyzer {
      def analyze(sql: String): Option[QueryAnalyzer.QueryMetadata] =
        Some(
          QueryAnalyzer.QueryMetadata(
            queryText = Some(sql),
            operationName = Some("SELECT"),
            collectionName = Some("users"),
            storedProcedureName = None,
            querySummary = Some("SELECT users")
          )
        )
    }

    val result = primary.orElse(fallback).analyze("select * from users")

    assertEquals(result.flatMap(_.querySummary), Some("SELECT users"))
  }

  test("orElse does not evaluate fallback when primary returns value") {
    var fallbackCalls = 0
    val primary = new QueryAnalyzer {
      def analyze(sql: String): Option[QueryAnalyzer.QueryMetadata] =
        Some(
          QueryAnalyzer.QueryMetadata(
            queryText = Some(sql),
            operationName = Some("PRIMARY"),
            collectionName = None,
            storedProcedureName = None,
            querySummary = Some("PRIMARY")
          )
        )
    }
    val fallback = new QueryAnalyzer {
      def analyze(sql: String): Option[QueryAnalyzer.QueryMetadata] = {
        fallbackCalls += 1
        None
      }
    }

    val result = primary.orElse(fallback).analyze("select 1")

    assertEquals(result.flatMap(_.querySummary), Some("PRIMARY"))
    assertEquals(fallbackCalls, 0)
  }

}
