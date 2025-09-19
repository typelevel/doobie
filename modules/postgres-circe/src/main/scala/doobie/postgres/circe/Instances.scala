// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.circe

import cats.Show
import cats.data.NonEmptyList
import cats.syntax.all.*
import io.circe.*
import io.circe.jawn.*
import io.circe.syntax.*
import doobie.util.*
import org.postgresql.util.PGobject

object Instances {

  private implicit val showPGobject: Show[PGobject] = Show.show(_.getValue.take(250))

  trait JsonbInstances {
    implicit val jsonbPut: Put[Json] =
      Put.Advanced.other[PGobject](
        NonEmptyList.of("jsonb")
      ).tcontramap { a =>
        val o = new PGobject
        o.setType("jsonb")
        o.setValue(a.noSpaces)
        o
      }

    implicit val jsonbGet: Get[Json] =
      Get.Advanced.other[PGobject](
        NonEmptyList.of("jsonb")
      ).temap(a =>
        parse(a.getValue).leftMap(_.show))

    private implicit val showString: Show[Array[String]] = Show.show(_.mkString("Array(", ", ", ")"))

    implicit val arrayJsonGet: Get[Array[Json]] =
      Get.Advanced.array[String](NonEmptyList.of("jsonb[]", "_jsonb"))
        .temap(_.toList.traverse(jsonStr =>
          parse(jsonStr).leftMap(_.getMessage)).map(_.toArray))

    implicit val arrayJsonPut: Put[Array[Json]] =
      Put.Advanced.array[String](NonEmptyList.of("_jsonb"), "jsonb")
        .tcontramap(_.map(_.noSpaces))

    def pgEncoderPutT[A: Encoder]: Put[A] =
      Put[Json].tcontramap(_.asJson)

    def pgEncoderPut[A: Encoder]: Put[A] =
      Put[Json].contramap(_.asJson)

    def pgDecoderGetT[A: Decoder]: Get[A] =
      Get[Json].temap(json => json.as[A].leftMap(_.show))

    def pgDecoderGet[A: Decoder]: Get[A] =
      Get[Json].map(json => json.as[A].fold(throw _, identity))
  }

  trait JsonInstances {
    implicit val jsonPut: Put[Json] =
      Put.Advanced.other[PGobject](
        NonEmptyList.of("json")
      ).tcontramap { a =>
        val o = new PGobject
        o.setType("json")
        o.setValue(a.noSpaces)
        o
      }

    implicit val jsonGet: Get[Json] =
      Get.Advanced.other[PGobject](
        NonEmptyList.of("json")
      ).temap(a =>
        parse(a.getValue).leftMap(_.show))

    def pgEncoderPutT[A: Encoder]: Put[A] =
      Put[Json].tcontramap(_.asJson)

    def pgEncoderPut[A: Encoder]: Put[A] =
      Put[Json].contramap(_.asJson)

    def pgDecoderGetT[A: Decoder]: Get[A] =
      Get[Json].temap(json => json.as[A].leftMap(_.show))

    def pgDecoderGet[A: Decoder]: Get[A] =
      Get[Json].map(json => json.as[A].fold(throw _, identity))

  }

}
