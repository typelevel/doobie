// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.specs2

import cats.effect.{ Effect, IO }
import doobie.imports._
import doobie.util.analysis._
import doobie.util.testing._
import org.specs2.mutable.Specification
import org.specs2.specification.core.{ Fragment, Fragments }
import org.specs2.specification.create.{ FormattingFragments => Format }
import org.specs2.specification.dsl.Online._
import scala.reflect.runtime.universe.TypeTag

/**
 * Module with a mix-in trait for specifications that enables checking of doobie `Query` and `Update` values.
 * {{{
 * // An example specification, taken from the examples project.
 * object AnalysisTestSpec extends Specification with AnalysisSpec {
 *
 *   // The transactor to use for the tests.
 *   val transactor = Transactor.fromDriverManager[IO](
 *     "org.postgresql.Driver",
 *     "jdbc:postgresql:world",
 *     "postgres", ""
 *   )
 *
 *   // Now just mention the queries. Arguments are not used.
 *   check(MyDaoModule.findByNameAndAge(null, 0))
 *   check(MyDaoModule.allWoozles)
 *
 * }
 * }}}
 */
object analysisspec {

  @deprecated("Use IOChecker.", "0.4.2")
  type AnalysisSpec = IOChecker

  trait Checker[M[_]] extends CheckerBase[M] { this: Specification =>

    def check[A: Analyzable](a: A): Fragments =
      checkImpl(Analyzable.unpack(a))

    @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
    def checkOutput[A: TypeTag](q: Query0[A]): Fragments =
      checkImpl(AnalysisArgs(
        s"Query0[${typeName[A]}]", q.pos, q.sql, q.outputAnalysis
      ))

    @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
    def checkOutput[A: TypeTag, B: TypeTag](q: Query[A, B]) =
      checkImpl(AnalysisArgs(
        s"Query[${typeName[A]}, ${typeName[B]}]", q.pos, q.sql, q.outputAnalysis
      ))

    private def checkImpl(args: AnalysisArgs): Fragments =
      // continuesWith is necessary to make sure the query doesn't run too early
      s"${args.header}\n\n${args.cleanedSql.padLeft("  ").toString}\n" >> ok.continueWith {
        val report = analyzeIO(args, transactor).unsafeRunSync
        indentBlock(
          report.items.map { item =>
            item.description ! item.error.fold(ok) {
              err => ko(err.wrap(70).toString)
            }
          }
        )
      }

    private def indentBlock(fs: Seq[Fragment]): Fragments =
      // intersperse fragments with newlines, and indent them.
      // This differs from standard version (FragmentsDsl.fragmentsBlock()) in
      // that any failure gets properly indented, too.
      Fragments.empty
        .append(Format.t)
        .append(fs.flatMap(Seq(Format.br, _)))
        .append(Format.bt)
  }

  /** Implementation of Checker[IO] */
  trait IOChecker extends Checker[IO] { this: Specification =>
    val M: Effect[IO] = implicitly
  }
}
