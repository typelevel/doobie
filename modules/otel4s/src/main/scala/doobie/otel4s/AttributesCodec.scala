// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import org.typelevel.otel4s.{Attribute, AttributeKey, AttributeType, Attributes}
import io.circe.*
import io.circe.syntax.*

private[doobie] object AttributesCodec {

  implicit val attributeEncoder: Encoder[Attribute[?]] =
    Encoder.instance { attribute =>
      Json.obj(
        "key" := attribute.key.name,
        "value" := encodeAttributeValue(attribute)
      )
    }

  implicit val attributesEncoder: Encoder[Attributes] =
    Encoder[List[Attribute[?]]].contramap(_.toList)

  implicit val attributeDecoder: Decoder[Attribute[?]] =
    Decoder.instance { c =>
      for {
        key <- c.get[String]("key")
        valueObj <- c.get[Json]("value")
        attr <- decodeAttributeValue(key, valueObj.hcursor)
      } yield attr
    }

  implicit val attributesDecoder: Decoder[Attributes] =
    Decoder.decodeList[Attribute[?]].map { attrs =>
      attrs.foldLeft(Attributes.empty)(_ + _)
    }

  private def encodeAttributeValue(attribute: Attribute[?]): Json = {
    def encode[A: Encoder](field: String): Json =
      Json.obj(field := attribute.value.asInstanceOf[A])

    attribute.key.`type` match {
      case AttributeType.Boolean    => encode[Boolean]("boolValue")
      case AttributeType.Double     => encode[Double]("doubleValue")
      case AttributeType.String     => encode[String]("stringValue")
      case AttributeType.Long       => encode[Long]("longValue")
      case AttributeType.BooleanSeq => encode[Seq[Boolean]]("boolArrayValue")
      case AttributeType.DoubleSeq  => encode[Seq[Double]]("doubleArrayValue")
      case AttributeType.StringSeq  => encode[Seq[String]]("stringArrayValue")
      case AttributeType.LongSeq    => encode[Seq[Long]]("longArrayValue")
    }
  }

  private def decodeAttributeValue(key: String, c: HCursor): Decoder.Result[Attribute[?]] = {
    val valueFields = c.keys.map(_.toList).getOrElse(Nil)

    def decode[A: Decoder](field: String)(implicit ks: AttributeKey.KeySelect[A]) =
      c.get[A](field).map(v => Attribute(key, v))

    valueFields match {
      case "boolValue" :: Nil        => decode[Boolean]("boolValue")
      case "doubleValue" :: Nil      => decode[Double]("doubleValue")
      case "stringValue" :: Nil      => decode[String]("stringValue")
      case "longValue" :: Nil        => decode[Long]("longValue")
      case "boolArrayValue" :: Nil   => decode[Seq[Boolean]]("boolArrayValue")
      case "doubleArrayValue" :: Nil => decode[Seq[Double]]("doubleArrayValue")
      case "stringArrayValue" :: Nil => decode[Seq[String]]("stringArrayValue")
      case "longArrayValue" :: Nil   => decode[Seq[Long]]("longArrayValue")
      case other                     =>
        Left(DecodingFailure(s"Unsupported attribute value shape for key '$key', fields=$other", c.history))
    }
  }

}
