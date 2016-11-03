#+cats
package doobie.syntax

#+fs2
import fs2.interop.cats._
#-fs2
import cats.implicits._

import doobie.imports._
import org.specs2.mutable.Specification

/**
 * Type inference from `Free[ConnectionOp[_], A]` to `ConnectionIO[A]`
 * fails in old Scala versions due to SI-2712.
 *
 * It is fixed in Scala 2.12 under `-Ypartial-unification` flag.
 *
 * Related issues
 *  - [[https://issues.scala-lang.org/browse/SI-2712]]
 *  - [[https://github.com/tpolecat/doobie/issues/369]]
 *
 * TODO
 *  - Try Scala 2.11.9 with partial unification enabled
 *  - No cross paths after dropping Scala 2.10 support and merge into [[catchablespec]]
 */
object catchablepartialunificationspec extends Specification {

  "catchable syntax" should {

    "work on unaliased ConnectionIO" in {
      42.pure[ConnectionIO].map(_ + 1).attempt
      true
    }

  }

}
#-cats
