// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.circe

import cats.effect.{ ContextShift, IO }
import doobie._
import doobie.implicits._
import io.circe.{Json, Encoder, Decoder}
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object pgjsonspec extends Specification {

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  def inOut[A: Write: Read](col: String, a: A) =
    for {
      _  <- Update0(s"CREATE TEMPORARY TABLE TEST (value $col)", None).run
      a0 <- Update[A](s"INSERT INTO TEST VALUES (?)", None).withUniqueGeneratedKeys[A]("value")(a)
    } yield a0

  @SuppressWarnings(Array("org.wartremover.warts.StringPlusAny"))
  def testInOut[A](col: String, a: A, t: Transactor[IO])(implicit m: Get[A], p: Put[A]) =
    s"Mapping for $col as ${m.typeStack}" >> {
      s"write+read $col as ${m.typeStack}" in {
        inOut(col, a).transact(t).attempt.unsafeRunSync must_== Right(a)
      }
      s"write+read $col as Option[${m.typeStack}] (Some)" in {
        inOut[Option[A]](col, Some(a)).transact(t).attempt.unsafeRunSync must_== Right(Some(a))
      }
      s"write+read $col as Option[${m.typeStack}] (None)" in {
        inOut[Option[A]](col, None).transact(t).attempt.unsafeRunSync must_== Right(None)
      }
    }

  {
    import doobie.postgres.circe.json.implicits._
    testInOut("json", Json.obj("something" -> Json.fromString("Yellow")), xa)
  }

  {
    import doobie.postgres.circe.jsonb.implicits._
    testInOut("jsonb", Json.obj("something" -> Json.fromString("Yellow")), xa)
  }


  // Explicit Type Checks

  "json" should {
    "check ok for read" in {
      import doobie.postgres.circe.json.implicits._

      val a = sql"select '{}' :: json".query[Json].analysis.transact(xa).unsafeRunSync
      a.columnTypeErrors must_== Nil
    }
    "check ok for write" in {
      import doobie.postgres.circe.json.implicits._
      val a = sql"select ${Json.obj()} :: json".query[Json].analysis.transact(xa).unsafeRunSync
      a.parameterTypeErrors must_== Nil
    }
  }

  "jsonb" should {
    "check ok for read" in {
      import doobie.postgres.circe.jsonb.implicits._
      val a = sql"select '{}' :: jsonb".query[Json].analysis.transact(xa).unsafeRunSync
      a.columnTypeErrors must_== Nil
    }

    "check ok for write" in {
      import doobie.postgres.circe.jsonb.implicits._
      val a = sql"select ${Json.obj()} :: jsonb".query[Json].analysis.transact(xa).unsafeRunSync
      a.parameterTypeErrors must_== Nil
    }
  }

  // Encoder / Decoders
  private final case class Foo(x: Json)
  private object Foo{
    import doobie.postgres.circe.json.implicits._
    implicit val fooEncoder: Encoder[Foo] = Encoder[Json].contramap(_.x)
    implicit val fooDecoder: Decoder[Foo] = Decoder[Json].map(Foo(_))
    implicit val fooGet : Get[Foo] = pgDecoderGetT[Foo]
    implicit val fooPut : Put[Foo] = pgEncoderPutT[Foo]
  }

  "fooGet" should {
    "check ok for read" in {
      val a = sql"select '{}' :: json".query[Foo].analysis.transact(xa).unsafeRunSync
      a.columnTypeErrors must_== Nil
    }
  }

  "fooPut" should {
    "check ok for write" in {
      val a = sql"select ${Foo(Json.obj())} :: json".query[Foo].analysis.transact(xa).unsafeRunSync
      a.parameterTypeErrors must_== Nil
    }
  }


}
