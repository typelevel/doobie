// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.mysql

import java.time.OffsetTime
import java.time.{LocalDate, LocalDateTime, LocalTime, OffsetDateTime}

import doobie._
import doobie.implicits._
import doobie.mysql.implicits._
import doobie.util.analysis.{ColumnTypeError, ColumnTypeWarning}

class CheckSuite extends munit.FunSuite {
  import cats.effect.unsafe.implicits.global
  import MySQLTestTransactor.xa

  test("OffsetDateTime Read typechecks") {
    successRead[Option[OffsetDateTime]](sql"SELECT CAST('2019-02-13 22:03:21.051' AS DATETIME)")

    warnRead[Option[OffsetDateTime]](sql"SELECT '2019-02-13 22:03:21.051'")
    warnRead[Option[OffsetDateTime]](sql"SELECT CAST('03:21' AS TIME)")
    warnRead[Option[OffsetDateTime]](sql"SELECT CAST('2019-02-13' AS DATE)")
    failedRead[Option[OffsetDateTime]](sql"SELECT 123")
  }

  test("LocalDateTime Read typechecks") {
    successRead[Option[LocalDateTime]](sql"SELECT CAST('2019-02-13 22:03:21.051' AS DATETIME)")

    warnRead[Option[LocalDateTime]](sql"SELECT '2019-02-13 22:03:21.051'")
    warnRead[Option[LocalDateTime]](sql"SELECT CAST('03:21' AS TIME)")
    warnRead[Option[LocalDateTime]](sql"SELECT CAST('2019-02-13' AS DATE)")
    failedRead[Option[LocalDateTime]](sql"SELECT 123")
  }

  test("LocalDate Read typechecks") {
    successRead[Option[LocalDate]](sql"SELECT CAST('2015-02-23' AS DATE)")

    warnRead[Option[LocalDate]](sql"SELECT CAST('2019-02-13 22:03:21.051' AS DATETIME)")
    warnRead[Option[LocalDate]](sql"SELECT CAST('03:21' AS TIME)")
    warnRead[Option[LocalDate]](sql"SELECT '2015-02-23'")
    failedRead[Option[LocalDate]](sql"SELECT 123")
  }

  test("LocalTime Read typechecks") {
    successRead[Option[LocalTime]](sql"SELECT CAST('03:21' AS TIME)")

    warnRead[Option[LocalTime]](sql"SELECT CAST('2019-02-13 22:03:21.051' AS DATETIME)")
    warnRead[Option[LocalTime]](sql"SELECT CAST('2015-02-23' AS DATE)")
    failedRead[Option[LocalTime]](sql"SELECT '03:21'")
    failedRead[Option[LocalTime]](sql"SELECT 123")
  }

  test("OffsetTime Read typechecks") {
    successRead[Option[OffsetTime]](sql"SELECT CAST('2019-02-13 22:03:21.051' AS DATETIME)")

    warnRead[Option[OffsetTime]](sql"SELECT CAST('03:21' AS TIME)")
    warnRead[Option[OffsetTime]](sql"SELECT CAST('2015-02-23' AS DATE)")
    failedRead[Option[OffsetTime]](sql"SELECT '03:21'")
    failedRead[Option[OffsetTime]](sql"SELECT 123")
  }

  private def successRead[A: Read](frag: Fragment): Unit = {
    val analysisResult = frag.query[A].analysis.transact(xa).unsafeRunSync()
    assertEquals(analysisResult.columnAlignmentErrors, Nil)

    val result = frag.query[A].unique.transact(xa).attempt.unsafeRunSync()
    assert(result.isRight)
  }

  private def warnRead[A: Read](frag: Fragment): Unit = {
    val analysisResult = frag.query[A].analysis.transact(xa).unsafeRunSync()
    val errorClasses = analysisResult.columnAlignmentErrors.map(_.getClass)
    assertEquals(errorClasses, List(classOf[ColumnTypeWarning]))

    val result = frag.query[A].unique.transact(xa).attempt.unsafeRunSync()
    assert(result.isRight)
  }

  private def failedRead[A: Read](frag: Fragment): Unit = {
    val analysisResult = frag.query[A].analysis.transact(xa).unsafeRunSync()
    val errorClasses = analysisResult.columnAlignmentErrors.map(_.getClass)
    assertEquals(errorClasses, List(classOf[ColumnTypeError]))

    val result = frag.query[A].unique.transact(xa).attempt.unsafeRunSync()
    assert(result.isLeft)
  }

}
