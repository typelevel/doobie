// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.scalatest

import cats.syntax.foldable._
import cats.instances.list._
import doobie.util.pretty._
import doobie.util.testing._

// Common formatting logic
private[scalatest] object formatting {

  def formatReport(
    args: AnalysisArgs,
    report: AnalysisReport
  ): Block = {
    val sql = args.cleanedSql
      .wrap(68)
      // SQL should use the default color
      .padLeft(Console.RESET.toString)
    val items = report.items.foldMap(formatItem)
    Block.fromString(args.header)
      .above(sql)
      .above(items)
  }

  private val formatItem: AnalysisReport.Item => Block = {
    case AnalysisReport.Item(desc, None) =>
      Block.fromString(s"${Console.GREEN}✓${Console.RESET} $desc")
    case AnalysisReport.Item(desc, Some(err)) =>
      Block.fromString(s"${Console.RED}✕${Console.RESET} $desc")
        // No color for error details - ScalaTest paints each line of failure
        // red by default.
        .above(err.wrap(66).padLeft("  "))
  }
}
