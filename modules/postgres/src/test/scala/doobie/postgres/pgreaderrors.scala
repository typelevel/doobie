// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect.{ ContextShift, Effect, IO }
import cats.effect.syntax.effect._
import cats.syntax.applicativeError._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.postgres.enums._
import doobie.util.invariant._
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext


trait pgreaderrorsspec[F[_]] extends Specification {

  implicit def E: Effect[F]
  implicit def contextShift: ContextShift[F]

  lazy val xa = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  implicit val MyEnumMetaOpt: Meta[MyEnum] = pgEnumStringOpt("myenum", {
    case "foo" => Some(MyEnum.Foo)
    case "bar" => Some(MyEnum.Bar)
    case _ => None
  }, {
    case MyEnum.Foo => "foo"
    case MyEnum.Bar => "bar"
  })
  implicit val MyScalaEnumMeta: Meta[MyScalaEnum.Value] = pgEnum(MyScalaEnum, "myenum")
  implicit val MyJavaEnumMeta: Meta[MyJavaEnum] = pgJavaEnum[MyJavaEnum]("myenum")

  "pgEnumStringOpt" in {
    val r = sql"select 'invalid'".query[MyEnum].unique.transact(xa).attempt.toIO.unsafeRunSync()
    r must_== Left(InvalidEnum[MyEnum]("invalid"))
  }

  "pgEnum" in {
    val r = sql"select 'invalid' :: myenum".query[MyScalaEnum.Value].unique.transact(xa).attempt.toIO.unsafeRunSync()
    r must_== Left(InvalidEnum[MyScalaEnum.Value]("invalid"))
  }

  "pgJavaEnum" in {
    val r = sql"select 'invalid' :: myenum".query[MyJavaEnum].unique.transact(xa).attempt.toIO.unsafeRunSync()
    r must_== Left(InvalidEnum[MyJavaEnum]("invalid"))
  }

}

class pgreaderrorsspecIO extends pgreaderrorsspec[IO] {
  implicit val E: Effect[IO] = IO.ioEffect
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
}
