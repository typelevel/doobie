// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.enums

import doobie.Meta

// This is an enum type defined in another schema (See other_enum in test-db.sql)
sealed abstract class OtherEnum(val strValue: String)

object OtherEnum {
  case object A extends OtherEnum("a")

  case object B extends OtherEnum("b")

  private def fromStrUnsafe(s: String): OtherEnum = s match {
    case "a" => A
    case "b" => B
  }

  private val elementTypeNameUnqualified = "other_enum"
  private val elementTypeName = s""""other_schema"."$elementTypeNameUnqualified""""
  private val arrayTypeName = s""""other_schema"."_$elementTypeNameUnqualified""""

  implicit val arrayMeta: Meta[Array[OtherEnum]] =
    Meta.Advanced.array[String](
      elementTypeName,
      arrayTypeName,
      s"_$elementTypeNameUnqualified"
    ).timap(arr => arr.map(fromStrUnsafe))(arr => arr.map(_.strValue))
}
