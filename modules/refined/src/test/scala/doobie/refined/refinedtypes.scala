// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.refined

import cats.effect.IO
import cats.implicits._
import doobie._, doobie.implicits._
import doobie.refined.implicits._
import org.specs2.mutable.Specification
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined._
import doobie.util.invariant._

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object refinedtypes extends Specification {

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:refined;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  type PositiveInt = Int Refined Positive

  "Meta" should {
    "exist for refined types" in {
      Meta[PositiveInt]
      true
    }
  }

  final case class Point(x: Int, y: Int)
  final case class Quadrant1()
  type PointInQuadrant1 = Point Refined Quadrant1

  implicit val PointComposite: Write[Point] =
    Write[(Int, Int)].contramap((p: Point) => (p.x, p.y))

  implicit val quadrant1Validate: Validate.Plain[Point, Quadrant1] =
    Validate.fromPredicate(p => p.x >= 0 && p.y >= 0, p => s"($p is in quadrant 1)", Quadrant1())

  "Write" should {

    "exist for refined types" in {
      Write[PointInQuadrant1]
      true
    }

    "exist for Option of a refined type" in {
      Write[Option[PositiveInt]]
      true
    }

  }

  "Query" should {
    "return a refined type when conversion is possible" in {
      sql"select 123".query[PositiveInt].unique.transact(xa).void.unsafeRunSync
      true
    }

    "return an Option of a refined type when query returns null-value" in {
      sql"select NULL".query[Option[PositiveInt]].unique.transact(xa).void.unsafeRunSync
      true
    }

    "return an Option of a refined type when query returns a value and converion is possible" in {
      sql"select NULL".query[Option[PositiveInt]].unique.transact(xa).void.unsafeRunSync
      true
    }

    "save a None of a refined type" in {
      val none: Option[PositiveInt] = None
      insertOptionalPositiveInt(none)
      true
    }

    "save a Some of a refined type" in {
      val somePositiveInt: Option[PositiveInt] = refineV[Positive](5).right.toOption
      insertOptionalPositiveInt(somePositiveInt)
      true
    }

    def insertOptionalPositiveInt(v: Option[PositiveInt]) = {
      val queryRes = for {
        _  <- Update0(s"CREATE LOCAL TEMPORARY TABLE TEST (value INT)", None).run
        _  <- sql"INSERT INTO TEST VALUES ($v)".update.run
      } yield ()
      queryRes.transact(xa).unsafeRunSync
    }

    "throw an SecondaryValidationFailed if value does not fit the refinement-type " in {
      secondaryValidationFailedCaught_?(
       sql"select -1".query[PositiveInt].unique.transact(xa).void.unsafeRunSync
      )
    }

    "return a refined product-type when conversion is possible" in {
      sql"select 1, 1".query[PointInQuadrant1].unique.transact(xa).void.unsafeRunSync
      true
    }

    "throw an SecondaryValidationFailed if object does not fit the refinement-type " in {
      secondaryValidationFailedCaught_?(
        sql"select -1, 1".query[PointInQuadrant1].unique.transact(xa).void.unsafeRunSync
      )
    }
  }

  private[this] def secondaryValidationFailedCaught_?(query: => Unit): Boolean =
    try {
      query
      false
    } catch {
      case _: SecondaryValidationFailed[_] => true
      case _: Throwable => false
    }

}
