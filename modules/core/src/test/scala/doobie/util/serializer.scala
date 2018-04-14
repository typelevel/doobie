// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.Eval
import cats.instances.list._
import doobie.util.serializer.{Serializer => S}
import org.specs2.mutable.Specification

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object serializerspec extends Specification {

  val base: Int => List[Int] = List(_)
  val baseS = S.opaque(base)

  "Serializer" >> {
    "opaque" in {
      baseS.combineAll(1) must_== base(1)
    }

    "contramap" in {
      val f: String => Int = _.length
      baseS.contramap(f).combineAll("abc") must_== base(3)
    }

    "product" in {
      S.product(baseS, baseS).combineAll((1, 2)) must_== (base(1) ++ base(2))
    }

    "suspend" in {
      S.suspend(baseS).combineAll(1) must_== base(1)
    }

    "suspend (laziness)"in {
      @SuppressWarnings(Array("org.wartremover.warts.Var"))
      var counter = 0
      val s = S.suspend {
        counter += 1
        baseS
      }
      counter must_== 0
      s.combineAll(1)
      counter must_== 1
    }

    "asFunction" in {
      // This doesn't change semantics, but can break stack safety.
      S.fromFunction(baseS.asFunction) must beTheSameAs(baseS)
    }
  }
}
