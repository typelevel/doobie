// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.enums

import doobie.Meta
import doobie.postgres.implicits.*
import doobie.postgres.implicits.arrayOfEnum

// create type myenum as enum ('foo', 'bar') <-- part of setup
sealed trait MyEnum
object MyEnum {
  case object Foo extends MyEnum
  case object Bar extends MyEnum

  def fromStringUnsafe(s: String): MyEnum = s match {
    case "foo" => Foo
    case "bar" => Bar
  }

  def asString(e: MyEnum): String = e match {
    case Foo => "foo"
    case Bar => "bar"
  }

  private val typeName = "myenum"

  implicit val MyEnumMeta: Meta[MyEnum] =
    pgEnumString(
      typeName,
      fromStringUnsafe,
      asString
    )

  implicit val MyEnumArrayMeta: Meta[Array[MyEnum]] =
    arrayOfEnum[MyEnum](
      typeName,
      fromStringUnsafe,
      asString
    )

}
