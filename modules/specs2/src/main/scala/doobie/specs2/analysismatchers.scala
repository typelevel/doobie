package doobie.specs2

import cats.effect.{ Effect, IO }
import cats.instances.list._
import cats.syntax.foldable._
import doobie.util.pretty._
import doobie.util.testing.{
  AnalysisReport,
  Analyzable,
  analyzeIO,
  CheckerBase
}
import org.specs2.matcher.{ Expectable, Matcher, MatchResult }

object analysismatchers {

  /**
    * Provides matcher syntax for query checking:
    *
    * {{{
    * sql"select 1".query[Int] must typecheck
    * }}}
    */
  trait AnalysisMatchers[F[_]] extends CheckerBase[F] {

    def typecheck[T](implicit analyzable: Analyzable[T]): Matcher[T] =
      new Matcher[T] {
        def apply[S <: T](t: Expectable[S]): MatchResult[S] = {
          val report = analyzeIO(
            analyzable.unpack(t.value),
            transactor
          ).unsafeRunSync
          reportToMatchResult(report, t)
        }
      }

    private def reportToMatchResult[S](
      r: AnalysisReport,
      s: Expectable[S]
    ): MatchResult[S] = {
      // We aim to produce the same format the fragment version does.

      val items = r.items.foldMap(itemToBlock)

      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      val message =
        Block.fromString(r.header)
          .above(Block.fromString(""))
          .above(r.sql.wrap(70).padLeft("  "))
          .above(Block.fromString(""))
          .above(items)
          .toString

      Matcher.result(r.succeeded, message, message, s)
    }

    private def itemToBlock(item: AnalysisReport.Item): Block =
      item.error match {
        case None =>
          Block.fromString(s"+ ${item.description}")
        case Some(e) =>
          Block.fromString(s"x ${item.description}").above(
            Block.fromString(" x ").leftOf(e.wrap(70))
          )
      }
  }

  trait IOAnalysisMatchers extends AnalysisMatchers[IO] {
    implicit val M: Effect[IO] = implicitly
  }
}
