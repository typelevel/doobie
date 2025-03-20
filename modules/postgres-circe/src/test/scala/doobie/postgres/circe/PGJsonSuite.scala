// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.circe

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import io.circe.{Decoder, Encoder, Json}

class PGJsonSuite extends munit.CatsEffectSuite {

  val xa = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql:world",
    user = "postgres",
    password = "password",
    logHandler = None
  )

  def inOut[A: Write: Read](col: String, a: A) =
    for {
      _ <- Update0(s"CREATE TEMPORARY TABLE TEST (value $col)", None).run
      a0 <- Update[A](s"INSERT INTO TEST VALUES (?)", None).withUniqueGeneratedKeys[A]("value")(a)
    } yield a0

  def testInOut[A](col: String, a: A, t: Transactor[IO])(implicit m: Get[A], p: Put[A]): Unit = {
    test(s"Mapping for $col as ${m.typeStack} - write+read $col as ${m.typeStack}") {
      inOut(col, a).transact(t).attempt.assertEquals(Right(a))
    }
    test(s"Mapping for $col as ${m.typeStack} - write+read $col as Option[${m.typeStack}] (Some)") {
      inOut[Option[A]](col, Some(a)).transact(t).attempt.assertEquals(Right(Some(a)))
    }
    test(s"Mapping for $col as ${m.typeStack} - write+read $col as Option[${m.typeStack}] (None)") {
      inOut[Option[A]](col, None).transact(t).attempt.assertEquals(Right(None))
    }
  }

  {
    import doobie.postgres.circe.json.implicits.*
    testInOut("json", Json.obj("something" -> Json.fromString("Yellow")), xa)
  }

  {
    import doobie.postgres.circe.jsonb.implicits.*
    testInOut("jsonb", Json.obj("something" -> Json.fromString("Yellow")), xa)
  }

  // Explicit Type Checks

  test("json should check ok for read") {
    import doobie.postgres.circe.json.implicits.*
    sql"select '{}' :: json".query[Json].analysis.transact(xa).map(_.columnTypeErrors).assertEquals(Nil)
  }
  test("json should check ok for write") {
    import doobie.postgres.circe.json.implicits.*
    sql"select '{}' :: jsonb".query[Json].analysis.transact(xa).map(_.parameterTypeErrors).assertEquals(Nil)
  }

  test("jsonb should check ok for read") {
    import doobie.postgres.circe.jsonb.implicits.*
    sql"select '{}' :: jsonb".query[Json].analysis.transact(xa).map(_.columnTypeErrors).assertEquals(Nil)
  }

  test("jsonb should check ok for write") {
    import doobie.postgres.circe.jsonb.implicits.*
    sql"select ${Json.obj()} :: jsonb".query[Json].analysis.transact(xa).map(_.parameterTypeErrors).assertEquals(Nil)
  }

  // Encoder / Decoders
  private case class Foo(x: Json)
  private object Foo {
    import doobie.postgres.circe.json.implicits.*
    implicit val fooEncoder: Encoder[Foo] = Encoder[Json].contramap(_.x)
    implicit val fooDecoder: Decoder[Foo] = Decoder[Json].map(Foo(_))
    implicit val fooGet: Get[Foo] = pgDecoderGetT[Foo]
    implicit val fooPut: Put[Foo] = pgEncoderPutT[Foo]
  }

  test("fooGet should check ok for read") {
    sql"select '{}' :: json".query[Foo].analysis.transact(xa).map(_.columnTypeErrors).assertEquals(Nil)
  }
  test("fooPut check ok for write") {
    sql"select ${Foo(Json.obj())} :: json".query[Foo].analysis.transact(xa).map(_.parameterTypeErrors).assertEquals(Nil)
  }

}
