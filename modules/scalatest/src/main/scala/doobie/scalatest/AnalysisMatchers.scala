// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.scalatest

import cats.effect.{ Effect, IO }
import doobie.scalatest.formatting.formatReport
import doobie.util.testing._
import org.scalatest.matchers.{ Matcher, MatchResult, LazyArg }

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
      def apply(t: T): MatchResult = {
        val args = analyzable.unpack(t)
        val report = analyzeIO(args, transactor).unsafeRunSync

        MatchResult(
          report.succeeded,
          s"${args.typeName} failed to typecheck:\n{}",
          s"${args.typeName} did typecheck:\n{}",
          Vector(
            // Avoid formatting if the check performed as expected
            LazyArg(()) { _ =>
              formatReport(args, report).padLeft("  ").toString
            }
          )
        )
      }
    }
}

trait IOAnalysisMatchers extends AnalysisMatchers[IO] {
  implicit val M: Effect[IO] = implicitly
}
