package doobie.refined

import org.specs2.mutable.Specification
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto._
import eu.timepit.refined._
import doobie.imports._

object refinedtypes extends Specification {

  val xa = DriverManagerTransactor[IOLite](
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

  case class Point(x: Int, y: Int)
  case class Quadrant1()
  type PointInQuadrant1 = Point Refined Quadrant1

  implicit val PointComposite: Composite[Point] =
    Composite[(Int, Int)].xmap(
      (t: (Int,Int)) => new Point(t._1, t._2),
      (p: Point) => (p.x, p.y)
    )

  implicit val quadrant1Validate: Validate.Plain[Point, Quadrant1] =
    Validate.fromPredicate(p => p.x >= 0 && p.y >= 0, p => s"($p is in quadrant 1)", Quadrant1())

  "Composite" should {
    "exist for refined types" in {
      Composite[PointInQuadrant1]

      true
    }
  }

  "Query" should {
    "return a refined type when conversion is possible" in {
      val pInt: PositiveInt =
        Query[Unit,PositiveInt]("select 123", None).unique(()).transact(xa).unsafePerformIO

      true
    }

    "throw an IllegalArgumentException if value does not fit the refinement-type " in {
      illegalArgumentExceptionCaught_?(
        Query[Unit, PositiveInt]("select -1", None).unique(()).transact(xa).unsafePerformIO
      )
    }

    "return a refined product-type when conversion is possible" in {
      val pInQ1: PointInQuadrant1 =
        Query[Unit,PointInQuadrant1]("select 1, 1", None).unique(()).transact(xa).unsafePerformIO

      true
    }

    "throw an IllegalArgumentException if object does not fit the refinement-type " in {
      illegalArgumentExceptionCaught_?(
        Query[Unit, PointInQuadrant1]("select -1, 1", None).unique(()).transact(xa).unsafePerformIO
      )
    }
  }

  private[this] def illegalArgumentExceptionCaught_?(query: => Unit): Boolean = try {
      query
      false
    }
    catch {
      case e: IllegalArgumentException => true
      case _: Throwable => false
    }

}
