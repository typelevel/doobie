// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.circe

import cats.data.NonEmptyList
import cats.implicits._
import io.circe._
import io.circe.jawn._
import io.circe.syntax._
import doobie.util._
import org.postgresql.util.PGobject
import scala.reflect.runtime.universe.TypeTag

trait Instances {

  implicit val jsonPut: Put[Json] = 
    Put.Advanced.other[PGobject](
      NonEmptyList.of("json")
    ).tcontramap{a =>
      val o = new PGobject
      o.setType("json")
      o.setValue(a.noSpaces)
      o
    }

  implicit val jsonGet: Get[Json] =
    Get.Advanced.other[PGobject](
      NonEmptyList.of("json")
    ).tmap(a => 
      parse(a.getValue).leftMap[Json](e => throw e).merge
    )

  def pgEncoderPutT[A: Encoder: TypeTag]: Put[A] = 
    Put[Json].tcontramap(_.asJson)

  def pgEncoderPut[A: Encoder]: Put[A] =
    Put[Json].contramap(_.asJson)

  def pgDecoderGetT[A: Decoder: TypeTag]: Get[A] = 
    Get[Json].tmap(json => json.as[A].fold(throw _, identity))

  def pgDecoderGet[A: Decoder]: Get[A] = 
    Get[Json].map(json => json.as[A].fold(throw _, identity))

}