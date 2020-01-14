// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package util

import shapeless._, shapeless.record._
import org.specs2.mutable.Specification
import com.github.ghik.silencer.silent


class writespec extends Specification {

  case class Woozle(a: (String, Int), b: Int :: String :: HNil, c: Boolean)

  case class LenStr1(n: Int, s: String)

  case class LenStr2(n: Int, s: String)
  object LenStr2 {
    implicit val LenStrMeta: Meta[LenStr2] =
      Meta[String].timap(s => LenStr2(s.length, s))(_.s)
  }

  "Write" should {

    "exist for some fancy types" in {
      util.Write[Int]
      util.Write[(Int, Int)]
      util.Write[(Int, Int, String)]
      util.Write[(Int, (Int, String))]
      util.Write[Woozle]

      // https://github.com/tpolecat/doobie/pull/126 was reverted because these
      // derivations were failing with SOE
      util.Write[(Woozle, String)]
      util.Write[(Int, Woozle :: Woozle :: String :: HNil)]

      true
    }

    "exist for Unit" in {
      util.Write[Unit]
      util.Write[(Int, Unit)].length must_== 1
    }

    "exist for shapeless record types" in {

      @silent type DL = (Double, Long)
      type A  = Record.`'foo -> Int, 'bar -> String, 'baz -> DL, 'quz -> Woozle`.T

      util.Write[A]
      util.Write[(A, A)]

      true
    }

    "exist for option of some fancy types" in {
      util.Write[Option[Int]]
      util.Write[Option[(Int, Int)]]
      util.Write[Option[(Int, Int, String)]]
      util.Write[Option[(Int, (Int, String))]]
      util.Write[Option[(Int, Option[(Int, String)])]]
      util.Write[Option[Woozle]]
      util.Write[Option[(Woozle, String)]]
      util.Write[Option[(Int, Woozle :: Woozle :: String :: HNil)]]
      true
    }

    "exist for option of Unit" in {
      util.Write[Option[Unit]]
      util.Write[Option[(Int, Unit)]].length must_== 1
    }

    // This doesn't work because of the issue with tagged element as HList members.
    // "exist for option of shapeless record types" in {
    //
    //   type DL = (Double, Long)
    //   type A  = Record.`'foo -> Int, 'bar -> String, 'baz -> DL, 'quz -> Woozle`.T
    //
    //   Write[Option[A]]
    //   Write[Option[(A, A)]]
    //   Write[Option[(A, Option[A])]]
    //
    //   true
    // }

    "select multi-column instance by default" in {
      util.Write[LenStr1].length must_== 2
    }

    "select 1-column instance when available" in {
      util.Write[LenStr2].length must_== 1
    }

    // "work for products of ludicrous size (128)" in {
    //   Write[
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int ::
    //     Int :: Int :: Int :: Int :: Int :: Int :: Int :: Int :: HNil]
    //   true
    // }

    // "work for specific example from #217" in {
    //   trait LocalDateTime
    //   implicit val MetaLocalDateTime: Put[LocalDateTime] = null
    //   Write[
    //     Option[String] :: Option[String] :: Option[String] :: Option[LocalDateTime] ::
    //     Option[String] :: Option[String] :: Option[String] :: Option[String] ::
    //     Option[String] :: Option[String] :: Option[String] :: Option[String] ::
    //     Option[String] :: Option[String] :: Option[String] :: Option[String] ::
    //     Option[String] :: Option[String] :: Option[String] :: Option[String] ::
    //     Option[String] :: Option[String] :: Option[LocalDateTime] :: Option[LocalDateTime] ::
    //     Option[LocalDateTime] :: Option[LocalDateTime] :: Option[String] :: Option[String] ::
    //     Option[String] :: Option[String] :: Option[String] :: Option[String] ::
    //     Option[String] :: Option[String] :: Option[String] :: Option[String] ::
    //     Option[String] :: Option[String] :: Option[String] :: Option[String] ::
    //     Option[String] :: Option[String] :: Option[String] :: Option[String] ::
    //     Option[String] :: Option[String] :: Option[String] :: Option[BigDecimal] ::
    //     Option[String] :: Option[BigDecimal] :: Option[BigDecimal] ::
    //     Option[String] :: Option[String] :: Option[String] :: Option[String] ::
    //     Option[String] :: Option[String] :: Option[String] :: Option[String] ::
    //     Option[String] :: Option[String] :: Option[BigDecimal] :: Option[BigDecimal] ::
    //     Option[String] :: Option[String] :: Option[String] :: Option[String] ::
    //     Option[String] :: Option[String] :: Option[String] :: Option[String] ::
    //     Option[String] :: Option[String] :: Option[BigDecimal] :: Option[BigDecimal] ::
    //     Option[String] :: HNil]
    //   true
    // }

  }

}
