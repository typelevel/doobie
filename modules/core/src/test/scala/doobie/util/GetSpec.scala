// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect._
import cats.effect.syntax.effect._
import cats.instances.int._
import cats.syntax.applicativeError._
import doobie._, doobie.implicits._
import doobie.enum.JdbcType.{ Array => _, _ }
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext
import shapeless.test._

object GetSpec {
  final case class Y(x: String) extends AnyVal
  final case class P(x: Int) extends AnyVal
}

class GetSpec extends Specification {
  import GetSpec._

  case class X(x: Int)
  case class Q(x: String)

  case class Z(i: Int, s: String)
  object S

  "Get" should {

    "exist for primitive types" in {
      Get[Int]
      Get[String]
      true
    }

    "be derived for unary products" in {
      Get[X]
      Get[Y]
      Get[P]
      Get[Q]
      true
    }

    "not be derived for non-unary products" in {
      illTyped("Get[Z]")
      illTyped("Get[(Int, Int)]")
      illTyped("Get[S.type]")
      true
    }
  }
}

final case class Foo(s: String)
final case class Bar(n: Int)


trait GetDBSpec[F[_]] extends Specification {

  implicit def E: Effect[F]
  def contextShift: ContextShift[F]
  def concurrent: Concurrent[F]
  
  lazy val xa = Transactor.fromDriverManager[F](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )(contextShift, concurrent)

  // Both of these will fail at runtime if called with a null value, we check that this is
  // avoided below.
  implicit def FooMeta: Get[Foo] = Get[String].map(s => Foo(s.toUpperCase))
  implicit def barMeta: Get[Bar] = Get[Int].temap(n => if (n == 0) Left("cannot be 0") else Right(Bar(n)))

  "Get" should {

    "not allow map to observe null on the read side (AnyRef)" in {
      val x = sql"select null".query[Option[Foo]].unique.transact(xa).toIO.unsafeRunSync()
      x must_== None
    }

    "read non-null value (AnyRef)" in {
      val x = sql"select 'abc'".query[Foo].unique.transact(xa).toIO.unsafeRunSync()
      x must_== Foo("ABC")
    }

    "error when reading a NULL into an unlifted Scala type (AnyRef)" in {
      def x = sql"select null".query[Foo].unique.transact(xa).attempt.toIO.unsafeRunSync()
      x must_== Left(doobie.util.invariant.NonNullableColumnRead(1, Char))
    }

    "not allow map to observe null on the read side (AnyVal)" in {
      val x = sql"select null".query[Option[Bar]].unique.transact(xa).toIO.unsafeRunSync()
      x must_== None
    }

    "read non-null value (AnyVal)" in {
      val x = sql"select 1".query[Bar].unique.transact(xa).toIO.unsafeRunSync()
      x must_== Bar(1)
    }

    "error when reading a NULL into an unlifted Scala type (AnyVal)" in {
      def x = sql"select null".query[Bar].unique.transact(xa).attempt.toIO.unsafeRunSync()
      x must_== Left(doobie.util.invariant.NonNullableColumnRead(1, Integer))
    }

    "error when reading an incorrect value" in {
      def x = sql"select 0".query[Bar].unique.transact(xa).attempt.toIO.unsafeRunSync()
      x must_== Left(doobie.util.invariant.InvalidValue[Int, Bar](0, "cannot be 0"))
    }

  }

}

object GetDBSpecIO extends GetDBSpec[IO] {
  implicit val E: Effect[IO] = IO.ioEffect
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  val concurrent: Concurrent[IO] = Concurrent[IO]
}
