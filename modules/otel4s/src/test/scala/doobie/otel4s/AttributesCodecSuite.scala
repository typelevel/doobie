// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import io.circe.Json
import io.circe.syntax.*
import org.typelevel.otel4s.{Attribute, Attributes}

class AttributesCodecSuite extends munit.FunSuite {

  import AttributesCodec.*

  test("encode scalar attributes to expected JSON") {
    val samples: List[(Attribute[?], Json)] = List(
      Attribute("bool", true) -> Json.obj(
        "key" := "bool",
        "value" := Json.obj("boolValue" := true)
      ),
      Attribute("double", 1.5d) -> Json.obj(
        "key" := "double",
        "value" := Json.obj("doubleValue" := 1.5d)
      ),
      Attribute("string", "value") -> Json.obj(
        "key" := "string",
        "value" := Json.obj("stringValue" := "value")
      ),
      Attribute("long", 42L) -> Json.obj(
        "key" := "long",
        "value" := Json.obj("longValue" := 42L)
      )
    )

    samples.foreach { case (attribute, expected) =>
      assertEquals(attributeEncoder(attribute), expected)
    }
  }

  test("encode sequence attributes to expected JSON") {
    val samples: Seq[(Attribute[?], Json)] = List(
      Attribute("bools", Seq(true, false)) -> Json.obj(
        "key" := "bools",
        "value" := Json.obj("boolArrayValue" := List(true, false))
      ),
      Attribute("doubles", Seq(1.5d, 2.5d)) -> Json.obj(
        "key" := "doubles",
        "value" := Json.obj("doubleArrayValue" := List(1.5d, 2.5d))
      ),
      Attribute("strings", Seq("a", "b")) -> Json.obj(
        "key" := "strings",
        "value" := Json.obj("stringArrayValue" := List("a", "b"))
      ),
      Attribute("longs", Seq(1L, 2L)) -> Json.obj(
        "key" := "longs",
        "value" := Json.obj("longArrayValue" := List(1L, 2L))
      )
    )

    samples.foreach { case (attribute, expected) =>
      assertEquals(attributeEncoder(attribute), expected)
    }
  }

  test("decode attribute JSON into expected attribute values") {
    val samples = List(
      Json.obj("key" := "bool", "value" := Json.obj("boolValue" := true)) -> Attribute("bool", true),
      Json.obj("key" := "double", "value" := Json.obj("doubleValue" := 1.5d)) -> Attribute("double", 1.5d),
      Json.obj("key" := "string", "value" := Json.obj("stringValue" := "value")) -> Attribute("string", "value"),
      Json.obj("key" := "long", "value" := Json.obj("longValue" := 42L)) -> Attribute("long", 42L),
      Json.obj(
        "key" := "bools",
        "value" := Json.obj("boolArrayValue" := Seq(true, false))
      ) -> Attribute("bools", Seq(true, false)),
      Json.obj(
        "key" := "doubles",
        "value" := Json.obj("doubleArrayValue" := List(1.5d, 2.5d))
      ) -> Attribute("doubles", Seq(1.5d, 2.5d)),
      Json.obj(
        "key" := "strings",
        "value" := Json.obj("stringArrayValue" := Seq("a", "b"))
      ) -> Attribute("strings", Seq("a", "b")),
      Json.obj(
        "key" := "longs",
        "value" := Json.obj("longArrayValue" := Seq(1L, 2L))
      ) -> Attribute("longs", Seq(1L, 2L))
    )

    samples.foreach { case (json, expected) =>
      assertEquals(json.as[Attribute[?]], Right(expected))
    }
  }

  test("encode and decode attributes as a list") {
    val attributes = Attributes(
      Attribute("a", 1L),
      Attribute("b", "b"),
      Attribute("c", Seq("x", "y"))
    )

    val decoded = attributes.asJson.as[Attributes]

    assertEquals(decoded.map(_.toList), Right(attributes.toList))
  }

  test("fail decoding when attribute value shape is unsupported") {
    val json = Json.obj(
      "key" := "bad",
      "value" := Json.obj(
        "boolValue" := true,
        "stringValue" := "oops"
      )
    )

    val decoded = json.as[Attribute[?]]

    decoded match {
      case Left(err)    => assert(err.message.contains("Unsupported attribute value shape"))
      case Right(value) => fail(s"Expected decoding failure but got $value")
    }
  }

}
