// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.bench

import java.sql.{
  DriverManager,
  PreparedStatement
}
import cats.implicits._
import doobie.util._
import org.openjdk.jmh.annotations._

// Some huge structure

final case class L1(
  a: Int,
  b: Option[Int],
  c: Int,
  d: Option[Int],
  e: Int,
  f: Option[Int],
  g: Int,
  h: Option[Int],
  i: Int,
  j: Option[Int]
)

final case class L2(
  a: L1,
  b: L1,
  c: L1,
  d: L1,
  e: L1,
  f: L1,
  g: L1,
  h: L1,
  i: L1,
  j: L1
)

final case class L3(
  a: L2,
  b: L2,
  c: L2,
  d: L2,
  e: L2,
  f: L2,
  g: L2,
  h: L2,
  i: L2,
  j: L2
)

// We're measuring the raw performance of `Write` - everything else is noise.
object write {

  @State(Scope.Benchmark)
  val l3 = {
    val l1 = L1(42, Some(42), 42, Some(42), 42, Some(42), 42, Some(42), 42, Some(42))
    val l2 = L2(l1, l1, l1, l1, l1, l1, l1, l1, l1, l1)
    L3(l2, l2, l2, l2, l2, l2, l2, l2, l2, l2)
  }

  // We're measuring the raw performance of `Write` - everything else is noise.
  @State(Scope.Thread)
  val statementForL3 = DriverManager.getConnection("jdbc:postgresql:world", "postgres", "")
    .prepareStatement("values (" + List.fill(1000)("?").intercalate(",") + ")")
}

class write {

  import write._

  def doUnsafeSet[A: Write](a: A, ps: PreparedStatement)(n: Int): Int = {
    for {
      _ <- 0 to n
    } Write[A].unsafeSet(ps, 1, a)
    n
  }

  def doUnsafeSetNoStack[A: Write](
    a: A,
    ps: PreparedStatement
  )(n: Int): Int = {
    for {
      _ <- 0 to n
    } Write[A].unsafeSet(ps, 1, a, stackHeightLimit = 0)
    n
  }

  def doUnsafeSetOld[A: OldWrite](a: A, ps: PreparedStatement)(n: Int): Int = {
    for {
      _ <- 0 to n
    } OldWrite[A].unsafeSet(ps, 1, a)
    n
  }

  @Benchmark
  @OperationsPerInvocation(1000)
  def unsafeSet: Int = doUnsafeSet(l3, statementForL3)(1000)

  @Benchmark
  @OperationsPerInvocation(1000)
  def unsafeSetNoStack: Int = doUnsafeSetNoStack(l3, statementForL3)(1000)

  @Benchmark
  @OperationsPerInvocation(1000)
  def unsafeSetOld: Int = doUnsafeSetOld(l3, statementForL3)(1000)
}
