// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.mysql

import java.time.ZoneOffset

import doobie._
import doobie.implicits._
import doobie.implicits.javasql._
import doobie.mysql.implicits._
import doobie.mysql.util.arbitraries.SQLArbitraries._
import doobie.mysql.util.arbitraries.TimeArbitraries._
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class TypesSuite extends munit.ScalaCheckSuite {
  import cats.effect.unsafe.implicits.global
  import MySQLTestTransactor.xa

  def inOut[A: Get : Put](col: String, a: A): ConnectionIO[A] = for {
      _ <- Update0(s"CREATE TEMPORARY TABLE test (value $col NOT NULL)", None).run
      _ <- Update[A](s"INSERT INTO test VALUES (?)", None).run(a)
      a0 <- Query0[A](s"SELECT value FROM test", None).unique
    } yield a0

  def inOutOpt[A: Get : Put](col: String, a: Option[A]): ConnectionIO[Option[A]] =
    for {
      _ <- Update0(s"CREATE TEMPORARY TABLE test (value $col)", None).run
      _ <- Update[Option[A]](s"INSERT INTO test VALUES (?)", None).run(a)
      a0 <- Query0[Option[A]](s"SELECT value FROM test", None).unique
    } yield a0

  def testInOut[A](col: String)(implicit m: Get[A], p: Put[A], arbitrary: Arbitrary[A]) = {
    testInOutWithCustomGen(col, arbitrary.arbitrary)
  }

  def testInOutNormalize[A](col: String)(f: A => A)(implicit m: Get[A], p: Put[A], arbitrary: Arbitrary[A]) = {
    testInOutWithCustomGen(col, arbitrary.arbitrary, skipNone = false, f)
  }

  def testInOutWithCustomGen[A](col: String, gen: Gen[A], skipNone: Boolean = false, expected: A => A = identity[A](_))(implicit m: Get[A], p: Put[A]) = {
    test(s"Mapping for $col as ${m.typeStack} - write+read $col as ${m.typeStack}") {
      forAll(gen) { (t: A) =>
        val actual = inOut(col, t).transact(xa).attempt.unsafeRunSync()
        assertEquals(actual.map(expected(_)), Right(expected(t)))
      }
    }
    test(s"Mapping for $col as ${m.typeStack} - write+read $col as Option[${m.typeStack}] (Some)") {
      forAll(gen) { (t: A) =>
        val actual = inOutOpt[A](col, Some(t)).transact(xa).attempt.unsafeRunSync()
        assertEquals(actual.map(_.map(expected(_))), Right(Some(expected(t))))
      }
    }
    if (!skipNone) {
      test(s"Mapping for $col as ${m.typeStack} - write+read $col as Option[${m.typeStack}] (None)") {
        assertEquals(inOutOpt[A](col, None).transact(xa).attempt.unsafeRunSync(), Right(None))
      }
    }
  }

  def skip(col: String, msg: String = "not yet implemented") =
    test(s"Mapping for $col ($msg)".ignore) {}

  testInOut[java.sql.Timestamp]("datetime(6)")
  testInOutNormalize[java.time.OffsetDateTime]("datetime(6)")(_.withOffsetSameInstant(ZoneOffset.UTC))
  testInOut[java.time.Instant]("datetime(6)")

  testInOut[java.time.LocalDateTime]("datetime(6)")
  testInOutWithCustomGen[java.time.LocalDateTime](
    "timestamp(6)",
    arbitraryLocalDateTimeTimestamp.arbitrary,
    skipNone = true // returns the current timestamp, lol
  )

  testInOut[java.sql.Date]("date")
  testInOut[java.time.LocalDate]("date")

  testInOut[java.sql.Time]("time")
  testInOut[java.time.LocalTime]("time(6)")
}
