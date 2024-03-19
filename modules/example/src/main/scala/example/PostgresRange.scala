// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package example

import cats.effect.{IO, IOApp}
import doobie._
import doobie.implicits._
import doobie.postgres.rangeimplicits._
import doobie.postgres.types.Range
import doobie.postgres.types.Range.{RangeBoundDecoder, RangeBoundEncoder}

import java.time.{LocalDate, LocalDateTime, OffsetDateTime}

object PostgresRange extends IOApp.Simple {

  private val xa = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver", url = "jdbc:postgresql:world", user = "postgres", password = "password", logHandler = None
  )

  private val int4rangeQuery = sql"select '[10, 20)'::int4range".query[Range[Int]]
  private val int8rangeQuery = sql"select '[10, 20)'::int8range".query[Range[Long]]
  private val numrangeQuery  = sql"select '[10.1, 20.2)'::numrange".query[Range[Double]] // or Float or BigDecimal
  private val daterangeQuery = sql"select '[2024-01-01,2024-02-02)'::daterange".query[Range[LocalDate]]
  private val tsrangeQuery   = sql"select '[2024-01-01 00:00:00,2024-02-02 00:00:00)'::tsrange".query[Range[LocalDateTime]]
  private val tstzrangeQuery = sql"select '[2024-01-01 00:00:00+00,2024-02-02 00:00:00+00)'::tstzrange".query[Range[OffsetDateTime]]

  // Custom range
  implicit val ByteRangeBoundEncoder: RangeBoundEncoder[Byte] = _.toString
  implicit val ByteRangeBoundDecoder: RangeBoundDecoder[Byte] = java.lang.Byte.parseByte
  implicit val ByteRangeMeta: Meta[Range[Byte]] = rangeMeta[Byte]("int4range")

  private val int4rangewWithByteBoundsQuery = sql"select '[-128, 127)'::int4range".query[Range[Byte]]

  def run: IO[Unit] =
    for {
      _ <- int4rangeQuery.to[List].transact(xa).flatTap(a => IO(println(a)))                // List(Range(Some(10),Some(20),[_,_)))
      _ <- int8rangeQuery.to[List].transact(xa).flatTap(a => IO(println(a)))                // List(Range(Some(10),Some(20),[_,_)))
      _ <- numrangeQuery.to[List].transact(xa).flatTap(a => IO(println(a)))                 // List(Range(Some(10.1),Some(20.2),[_,_)))
      _ <- daterangeQuery.to[List].transact(xa).flatTap(a => IO(println(a)))                // List(Range(Some(2024-01-01),Some(2024-02-02),[_,_)))
      _ <- tsrangeQuery.to[List].transact(xa).flatTap(a => IO(println(a)))                  // List(Range(Some(2024-01-01T00:00),Some(2024-02-02T00:00),[_,_)))
      _ <- tstzrangeQuery.to[List].transact(xa).flatTap(a => IO(println(a)))                // List(Range(Some(2024-01-01T02:00+02:00),Some(2024-02-02T02:00+02:00),[_,_)))
      _ <- int4rangewWithByteBoundsQuery.to[List].transact(xa).flatTap(a => IO(println(a))) // List(Range(Some(-128),Some(127),[_,_)))
    } yield ()
}
