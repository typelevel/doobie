// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.util

import Predef.augmentString
import cats.effect.IO
import org.typelevel.doobie.syntax.all.*
import org.typelevel.doobie.testutils.*
import org.typelevel.doobie.util.transactor.Transactor

import scala.annotation.nowarn

trait ReadSuitePlatform { self: munit.CatsEffectSuite =>

  def xa: Transactor[IO]

  case class Woozle(a: (String, Int), b: Int *: String *: EmptyTuple, c: Boolean)

  sealed trait NoGetInstanceForThis
  case class CaseClassWithFieldWithoutGetInstance(a: String, b: NoGetInstanceForThis)

  test("Read should exist for some fancy types") {
    import org.typelevel.doobie.generic.auto.*

    Read[Woozle].void
    Read[(Woozle, String)].void
    Read[(Int, Woozle *: Woozle *: String *: EmptyTuple)].void
  }

  test("Read should exist for option of some fancy types") {
    import org.typelevel.doobie.generic.auto.*

    Read[Option[Woozle]].void
    Read[Option[(Woozle, String)]].void
    Read[Option[(Int, Woozle *: Woozle *: String *: EmptyTuple)]].void
  }

  test("Read should not exist for case class with field without Get instance") {
    val compileError = compileErrors("Read[CaseClassWithFieldWithoutGetInstance]")
    assert(
      compileError.contains(
        """Cannot find or construct a Read instance for type:
          |
          |  ReadSuitePlatform.this.CaseClassWithFieldWithoutGetInstance
          |
          |This can happen for a few reasons,""".stripMargin
      )
    )
  }

  test("derives") {
    case class Foo(a: String, b: Int) derives Read
    val _ = Foo("", 1)
  }

  test("Read error for a nested field shows the fields leading to it") {
    import org.typelevel.doobie.generic.auto.*
    assertContains(
      compileErrors("Read[(Int, Option[CaseClassWithFieldWithoutGetInstance])]"),
      """Cannot find or construct a Read instance for type:
        |
        |  ReadSuitePlatform.this.NoGetInstanceForThis
        |  (required by field b of ReadSuitePlatform.this.CaseClassWithFieldWithoutGetInstance)
        |  (required by field _2 of scala.Tuple2[scala.Int, scala.Option[ReadSuitePlatform.this.CaseClassWithFieldWithoutGetInstance]])
        |
        |This can happen for a few reasons,""".stripMargin
    )
  }: @nowarn("msg=.*(u|U)nused import.*")

  test("Read should not be derivable for recursive types") {
    import org.typelevel.doobie.generic.auto.*
    assertContains(compileErrors("Read[Recursive]"), "Cannot derive Read instance for recursive type")
    assertContains(compileErrors("Read.derived[Recursive]"), "Cannot derive Read instance for recursive type")
  }: @nowarn("msg=.*(u|U)nused import.*")

  test("Read instances in companion objects are preferred over automatic derivation") {
    import org.typelevel.doobie.generic.auto.*
    assert(Read[TestTypes.CustomReadWrite] eq TestTypes.CustomReadWrite.read)
  }

  test("Read.derived reports errors from automatically derived fields") {
    import org.typelevel.doobie.generic.auto.*
    assertContains(
      compileErrors("Read.derived[(Int, CaseClassWithFieldWithoutGetInstance)]"),
      """  ReadSuitePlatform.this.NoGetInstanceForThis
        |  (required by field b of ReadSuitePlatform.this.CaseClassWithFieldWithoutGetInstance)""".stripMargin
    )
  }: @nowarn("msg=.*(u|U)nused import.*")

  test("Get instance is preferred over deriving a case class") {
    import org.typelevel.doobie.generic.auto.*
    assertEquals(Read[TestTypes.CustomGetPut].length, 1)
    assertEquals(Read[Option[TestTypes.CustomGetPut]].length, 1)
  }

  test("Read.derived derives nested case classes only when automatic derivation is enabled") {
    assertContains(compileErrors("Read.derived[TestTypes.ComplexCaseClass]"), "Cannot find or construct")

    import org.typelevel.doobie.generic.auto.*
    assertEquals(Read.derived[TestTypes.ComplexCaseClass].length, 8)
  }

  test("Read can be derived for generic case classes") {
    case class Box[A](a: A, i: Int) derives Read
    assertEquals(Read[Option[Box[Option[String]]]].length, 2)
    sql"SELECT 'a', 1, 2".query[Box[(String, Int)]].unique.transact(xa).assertEquals(Box(("a", 1), 2))
  }

  test("Read exists for tuples with more than 22 elements") {
    // format: off
    type T23 = (Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, String)
    // format: on
    val expected: T23 = (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, "s")
    sql"SELECT 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 's'"
      .query[T23].unique.transact(xa).assertEquals(expected) *>
      sql"SELECT 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, NULL"
        .query[Option[T23]].unique.transact(xa).assertEquals(None)
  }
}

case class Recursive(a: Int, next: Option[Recursive])
