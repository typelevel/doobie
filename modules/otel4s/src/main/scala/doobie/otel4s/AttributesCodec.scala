// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import org.typelevel.otel4s.{AnyValue, Attribute, AttributeKey, AttributeType, Attributes}
import io.circe.*
import io.circe.syntax.*
import scodec.bits.ByteVector

private[doobie] object AttributesCodec {

  implicit val anyValueEncoder: Encoder[AnyValue] =
    new Encoder[AnyValue] { self =>
      def apply(a: AnyValue): Json =
        a match {
          case string: AnyValue.StringValue       => Json.obj("string" := string.value)
          case boolean: AnyValue.BooleanValue     => Json.obj("boolean" := boolean.value)
          case long: AnyValue.LongValue           => Json.obj("long" := long.value)
          case double: AnyValue.DoubleValue       => Json.obj("double" := double.value)
          case byteArray: AnyValue.ByteArrayValue => Json.obj("bytes" := ByteVector(byteArray.value).toBase64)
          case seq: AnyValue.SeqValue             => Json.obj("seq" := seq.value.map(self.apply))
          case map: AnyValue.MapValue             => Json.obj("map" := map.value.view.mapValues(self.apply).toMap)
          case _: AnyValue.EmptyValue             => Json.obj("empty" := Json.Null)
        }
    }

  implicit val anyValueDecoder: Decoder[AnyValue] =
    new Decoder[AnyValue] { self =>
      implicit val ByteArrayDecoder: Decoder[Array[Byte]] =
        Decoder[String].emap { base64 =>
          ByteVector.fromHex(base64).toRight("Invalid base64 encoding for bytes value").map(_.toArray)
        }

      def apply(c: HCursor): Decoder.Result[AnyValue] = {
        val valueFields = c.keys.map(_.toList).getOrElse(Nil)

        def decode[A: Decoder](field: String, make: A => AnyValue) =
          c.get[A](field).map(v => make(v))

        valueFields match {
          case "string" :: Nil  => decode[String]("string", AnyValue.string)
          case "boolean" :: Nil => decode[Boolean]("boolean", AnyValue.boolean)
          case "double" :: Nil  => decode[Double]("double", AnyValue.double)
          case "long" :: Nil    => decode[Long]("long", AnyValue.long)
          case "bytes" :: Nil   => decode[Array[Byte]]("bytes", AnyValue.bytes)
          case "seq" :: Nil     =>
            decode[Seq[AnyValue]]("seq", AnyValue.seq)(using Decoder.decodeSeq(self))
          case "map" :: Nil =>
            decode[Map[String, AnyValue]]("map", AnyValue.map)(using Decoder.decodeMap(implicitly, self))
          case "empty" :: Nil =>
            Right(AnyValue.empty)
          case other =>
            Left(DecodingFailure(s"Unsupported AnyValue shape for key fields=$other", c.history))
        }
      }
    }

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
      case AttributeType.AnyValue   => encode[AnyValue]("anyValue")
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
      case "anyValue" :: Nil         => decode[AnyValue]("anyValue")
      case other                     =>
        Left(DecodingFailure(s"Unsupported attribute value shape for key '$key', fields=$other", c.history))
    }
  }

}
