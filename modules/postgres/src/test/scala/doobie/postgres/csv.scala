// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect.IO
import cats.implicits._
import doobie._, doobie.implicits._
import doobie.postgres.implicits._
import org.specs2.mutable.Specification
import org.specs2.ScalaCheck
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object csvspec extends Specification with ScalaCheck {

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  implicit val byteListInstance: Csv[List[Byte]] =
    Csv[Array[Byte]].contramap(_.toArray)

  val create: ConnectionIO[Unit] =
    sql"""| CREATE TEMPORARY TABLE test (
          |  id serial,  -- just for ordering
          |   a text,    -- String
          |   b int2,    -- Short
          |   c int4,    -- Int
          |   d int8,    -- Long
          |   e float4,  -- Float
          |   f float8,  -- Double
          |   g numeric, -- BigDecimal
          |   h boolean  -- Boolean
          | ) ON COMMIT DELETE ROWS
          |""".stripMargin.update.run.void

  val insert: Fragment =
    sql"""| COPY test (a, b, c, d, e, f, g, h)
          | FROM STDIN
          | WITH (FORMAT csv)
          |""".stripMargin

  val selectAll: ConnectionIO[List[Row]] =
    sql"SELECT a, b, c, d, e, f, g, h FROM test ORDER BY id ASC".query[Row].to[List]

  final case class Row(
    a: Option[String],
    b: Option[Short],
    c: Option[Int],
    d: Option[Long],
    e: Option[Float],
    f: Option[Double],
    g: Option[BigDecimal],
    h: Option[Boolean],
  )

  val genRow: Gen[Row] =
    for {
      a <- arbitrary[Option[String]].map(_.map(_.replace("\u0000", ""))) // NUL is disallowed in PG
      b <- arbitrary[Option[Short]]
      c <- arbitrary[Option[Int]]
      d <- arbitrary[Option[Long]]
      e <- arbitrary[Option[Float]]
      f <- arbitrary[Option[Double]]
      g <- arbitrary[Option[BigDecimal]]
      h <- arbitrary[Option[Boolean]]
    } yield Row(a, b, c, d, e, f, g, h)

  val genRows: Gen[List[Row]] =
    Gen.choose(1,1).flatMap(Gen.listOfN(_, genRow))

  "copyIn" should {
    "correctly insert small batches of rows of base types" in forAll(genRows) { rs =>
      val rsʹ = (create *> insert.copyIn(rs) *> selectAll).transact(xa).unsafeRunSync
      rs must_=== rsʹ
    }
  }

}
