// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect.IO
import doobie._, doobie.implicits._
import doobie.postgres.enums._
import doobie.postgres.implicits._
import doobie.util.analysis.{ColumnTypeWarning, ColumnTypeError}

import java.time.{Instant, OffsetDateTime, LocalDate, LocalDateTime, LocalTime}
import scala.concurrent.ExecutionContext


class CheckSuite extends munit.FunSuite {

  import cats.effect.unsafe.implicits.global

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  test("pgEnumString Read and Write typechecks") {
    successRead[MyEnum](sql"select 'foo' :: myenum")
    successWrite[MyEnum](MyEnum.Foo, "myenum")
  }

  test("OffsetDateTime Read and Write typechecks") {
    val t = OffsetDateTime.parse("2019-02-13T22:03:21.000+08")
    successRead[OffsetDateTime](sql"SELECT '2019-02-13T22:03:21.000' :: TIMESTAMPTZ")
    successWrite[OffsetDateTime](t, "TIMESTAMPTZ")

    successReadUnfortunately[OffsetDateTime](sql"SELECT '2019-02-13T22:03:21.000' :: TIMESTAMP")
    successWriteUnfortunately[OffsetDateTime](t, "TIMESTAMP")

    warnRead[OffsetDateTime](sql"SELECT '2019-02-13T22:03:21.000' :: TEXT")
    warnRead[OffsetDateTime](sql"SELECT '03:21' :: TIME")
    warnRead[OffsetDateTime](sql"SELECT '03:21' :: TIMETZ")
    warnRead[OffsetDateTime](sql"SELECT '2019-02-13' :: DATE")

    successWriteUnfortunately[OffsetDateTime](t, "TEXT")
    successWriteUnfortunately[OffsetDateTime](t, "TIME")
    successWriteUnfortunately[OffsetDateTime](t, "TIMETZ")
    successWriteUnfortunately[OffsetDateTime](t, "DATE")

    failedRead[OffsetDateTime](sql"SELECT '123' :: BYTEA")
    successWriteUnfortunately[OffsetDateTime](t, "BYTEA")
  }

  test("Instant Read and Write typechecks") {
    val t = Instant.parse("2019-02-13T22:03:21.000Z")
    successRead[Instant](sql"SELECT '2019-02-13T22:03:21.000' :: TIMESTAMPTZ")
    successWrite[Instant](t, "TIMESTAMPTZ")

    successReadUnfortunately[Instant](sql"SELECT '2019-02-13T22:03:21.000' :: TIMESTAMP")
    successWriteUnfortunately[Instant](t, "TIMESTAMP")

    warnRead[Instant](sql"SELECT '2019-02-13T22:03:21.000' :: TEXT")
    warnRead[Instant](sql"SELECT '03:21' :: TIME")
    warnRead[Instant](sql"SELECT '03:21' :: TIMETZ")
    warnRead[Instant](sql"SELECT '2019-02-13' :: DATE")

    successWriteUnfortunately[Instant](t, "TEXT")
    successWriteUnfortunately[Instant](t, "TIME")
    successWriteUnfortunately[Instant](t, "TIMETZ")
    successWriteUnfortunately[Instant](t, "DATE")

    failedRead[Instant](sql"SELECT '123' :: BYTEA")
    successWriteUnfortunately[Instant](t, "BYTEA")
  }

  test("LocalDateTime Read and Write typechecks") {
    val t = LocalDateTime.parse("2019-02-13T22:03:21.051")
    successRead[LocalDateTime](sql"SELECT '2019-02-13T22:03:21.051' :: TIMESTAMP")
    successWrite[LocalDateTime](t, "TIMESTAMP")

    successReadUnfortunately[LocalDateTime](sql"SELECT '2019-02-13T22:03:21.051' :: TIMESTAMPTZ")
    successWriteUnfortunately[LocalDateTime](t, "TIMESTAMPTZ")

    warnRead[LocalDateTime](sql"SELECT '2019-02-13T22:03:21.051' :: TEXT")
    warnRead[LocalDateTime](sql"SELECT '03:21' :: TIME")
    warnRead[LocalDateTime](sql"SELECT '03:21' :: TIMETZ")
    warnRead[LocalDateTime](sql"SELECT '2019-02-13' :: DATE")

    successWriteUnfortunately[LocalDateTime](t, "TEXT")
    successWriteUnfortunately[LocalDateTime](t, "TIME")
    successWriteUnfortunately[LocalDateTime](t, "TIMETZ")
    successWriteUnfortunately[LocalDateTime](t, "DATE")

    failedRead[LocalDateTime](sql"SELECT '123' :: BYTEA")
    successWriteUnfortunately[LocalDateTime](t, "BYTEA")
  }

  test("LocalDate Read and Write typechecks") {
    val t = LocalDate.parse("2015-02-23")
    successRead[LocalDate](sql"SELECT '2015-02-23' :: DATE")
    successWrite[LocalDate](t, "DATE")

    warnRead[LocalDate](sql"SELECT '2015-02-23T01:23:13.000' :: TIMESTAMP")
    warnRead[LocalDate](sql"SELECT '2015-02-23T01:23:13.000Z' :: TIMESTAMPTZ")
    warnRead[LocalDate](sql"SELECT '2015-02-23' :: TEXT")
    failedRead[LocalDate](sql"SELECT '03:21' :: TIME")
    failedRead[LocalDate](sql"SELECT '03:21' :: TIMETZ")

    successWriteUnfortunately[LocalDate](t, "TEXT")
    successWriteUnfortunately[LocalDate](t, "TIME")
    successWriteUnfortunately[LocalDate](t, "TIMETZ")
    successWriteUnfortunately[LocalDate](t, "DATE")

    failedRead[LocalDate](sql"SELECT '123' :: BYTEA")
    successWriteUnfortunately[LocalDate](t, "BYTEA")
  }

  test("LocalTime Read and Write typechecks") {
    val t = LocalTime.parse("23:13")
    successRead[LocalTime](sql"SELECT '23:13' :: TIME")
    successWrite[LocalTime](t, "TIME")

    warnRead[LocalTime](sql"SELECT '2015-02-23T01:23:13.000' :: TIMESTAMP")
    warnRead[LocalTime](sql"SELECT '2015-02-23T01:23:13.000Z' :: TIMESTAMPTZ")
    warnRead[LocalTime](sql"SELECT '2015-02-23' :: TEXT")
    failedRead[LocalTime](sql"SELECT '2015-02-23' :: DATE")

    successWriteUnfortunately[LocalTime](t, "TEXT")
    successWriteUnfortunately[LocalTime](t, "TIME")
    successWriteUnfortunately[LocalTime](t, "TIMETZ")
    successWriteUnfortunately[LocalTime](t, "DATE")

    failedRead[LocalTime](sql"SELECT '123' :: BYTEA")
    successWriteUnfortunately[LocalTime](t, "BYTEA")
  }

  private def successRead[A: Read](frag: Fragment): Unit = {
    val analysisResult = frag.query[A].analysis.transact(xa).unsafeRunSync()
    assertEquals(analysisResult.columnAlignmentErrors, Nil)
  }

  private def successWrite[A: Put](value: A, dbType: String): Unit = {
    val frag = sql"SELECT $value :: " ++ Fragment.const(dbType)
    val analysisResult = frag.update.analysis.transact(xa).unsafeRunSync()
    assertEquals(analysisResult.columnAlignmentErrors, Nil)
  }

  private def warnRead[A: Read](frag: Fragment): Unit = {
    val analysisResult = frag.query[A].analysis.transact(xa).unsafeRunSync()
    val errorClasses = analysisResult.columnAlignmentErrors.map(_.getClass)
    assertEquals(errorClasses, List(classOf[ColumnTypeWarning]))
  }

  private def failedRead[A: Read](frag: Fragment): Unit = {
    val analysisResult = frag.query[A].analysis.transact(xa).unsafeRunSync()
    val errorClasses = analysisResult.columnAlignmentErrors.map(_.getClass)
    assertEquals(errorClasses, List(classOf[ColumnTypeError]))
  }

  private def successWriteUnfortunately[A: Put](value: A, dbType: String): Unit = successWrite(value, dbType)

  // Some DB types really shouldn't type check but driver is too lenient
  private def successReadUnfortunately[A: Read](frag: Fragment): Unit = successRead(frag)

}
