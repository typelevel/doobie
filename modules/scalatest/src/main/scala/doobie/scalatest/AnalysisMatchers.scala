// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.scalatest

import cats.effect.{ Effect, IO }
import doobie.util.testing._
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest.matchers.{LazyArg, MatchResult, Matcher}
import org.scalatest.matchers.dsl.MatcherFactory2

import scala.reflect.ClassTag

/**
  * Provides matcher syntax for query checking:
  *
  * {{{
  * sql"select 1".query[Int] must typecheck
  * }}}
  */
trait AnalysisMatchers[F[_]] extends CheckerBase[F] {

  val typecheck: MatcherFactory2[Any, Analyzable, ClassTag] =
    new MatcherFactory2[Any, Analyzable, ClassTag] {
      override def matcher[T: Analyzable: ClassTag]: Matcher[T] =
        Matcher(matchTypecheck[T](_))
    }

  private def matchTypecheck[T](t: T)(
    implicit analyzable: Analyzable[T]
  ): MatchResult = {
    val args = analyzable.unpack(t)
    val report = analyzeIO(args, transactor).unsafeRunSync

    MatchResult(
      report.succeeded,
      s"${args.typeName} failed to typecheck:\n{0}",
      s"${args.typeName} did typecheck:\n{0}",
      Vector(
        // Avoid formatting if the check performed as expected
        LazyArg(()) { _ =>
          formatReport(args, report, colors).padLeft("  ").toString
        }
      )
    )
  }
}

trait IOAnalysisMatchers extends AnalysisMatchers[IO] {
  override val M: Effect[IO] = implicitly
}

trait TaskAnalysisMatchers extends AnalysisMatchers[Task] {
  implicit val scheduler: Scheduler = Scheduler.global
  override val M: Effect[Task] = Task.catsEffect
}