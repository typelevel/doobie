package doobie.specs2

import doobie.util.pretty._
import doobie.specs2.util.{
  AnalysisReport,
  Analyzable,
  analyze,
  UnsafeTransactionSupport
}
import org.specs2.matcher.{ Expectable, Matcher, MatchResult }

object analysismatchers {

  /**
    * Provides matcher syntax for query checking:
    *
    * {{{
    * sql"select 1".query[Int] must typecheck
    * }}}
    *
    * Requires [[doobie.specs2.util.UnsafeTransactionSupport]] instance in scope
    * to work; the easiest way to get one is to implement
    * [[doobie.specs2.util.UnsafeTransactions]] or
    * [[doobie.specs2.analysisspec.Checker]].
    */
  trait AnalysisMatchers {
    def typecheck[T](
      implicit support: UnsafeTransactionSupport,
               analyzable: Analyzable[T]
    ): Matcher[T] =
      new Matcher[T] {
        def apply[S <: T](t: Expectable[S]): MatchResult[S] =
          reportToMatchResult(analyze(analyzable.unpack(t.value)), t)
      }

    private def reportToMatchResult[S](
      r: AnalysisReport,
      s: Expectable[S]
    ): MatchResult[S] = {
      // We aim to produce the same format the fragment version does.

      val items = r.items
        .map(itemToBlock)
        .foldLeft(Block.empty)(_ above _)

      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      val message =
        Block.fromString(r.header)
          .above(Block.fromString(""))
          .above(r.sql.wrap(80).padLeft("  "))
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
            Block.fromString(" x ").leftOf(e.wrap(80))
          )
      }
  }

  object AnalysisMatchers extends AnalysisMatchers
}
