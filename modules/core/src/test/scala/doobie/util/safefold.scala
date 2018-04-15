// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.instances.list._
import doobie.util.safefold.{SafeFold => S}
import org.specs2.mutable.Specification

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object safefoldspec extends Specification {

  val base: Int => List[Int] = List(_)
  val baseS = S.opaque(base)
  val product2 = baseS.product(baseS)
  val product3 = baseS.product(baseS).product(baseS)
    .contramap[(Int, Int, Int)] { case (a, b, c) => ((a, b), c) }

  "SafeFold" >> {
    "opaque" in {
      baseS(1).combineAll must_== base(1)
    }

    "contramap" in {
      val f: String => Int = _.length
      baseS.contramap(f)("abc").combineAll must_== base(3)
    }

    "product" in {
      product2((1, 2)).combineAll must_== (base(1) ++ base(2))
    }

    "suspend" in {
      S.suspend(baseS)(1).combineAll must_== base(1)
    }

    "suspend (laziness)"in {
      @SuppressWarnings(Array("org.wartremover.warts.Var"))
      var counter = 0
      val s = S.suspend {
        counter += 1
        baseS
      }
      counter must_== 0
      s(1).combineAll
      counter must_== 1
    }

    "asFunction1" in {
      // This doesn't change semantics, but can break stack safety.
      S.fromFunction1(baseS.asFunction1) must beTheSameAs(baseS)
    }

    "asEffectFunction2" in {
      val s2 = S.opaque[(Int, Int), Unit](_ => ())
      S.fromFunction2(s2.asEffectFunction2) must beTheSameAs(s2)
    }

    "asEffectFunction3" in {
      val s3 = S.opaque[(Int, Int, Int), Unit](_ => ())
      S.fromFunction3(s3.asEffectFunction3) must beTheSameAs(s3)
    }
  }
}
