// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.Eval
import doobie.util.serializer.{Serializer => S}
import org.specs2.mutable.Specification

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object serializerspec extends Specification {

  val base: Int => List[Any] = List(_)

  "Serializer" >> {
    "Opaque" in {
      S.Opaque(base)(1) must_== base(1)
    }

    "Contramap" in {
      val f: String => Int = _.length
      S.Contramap(f, base)("abc") must_== base(3)
    }

    "Contramap2" in {
      def f(a: Int) = (a, a + 1)
      S.Contramap2(f, base, base)(1) must_== (base(1) ++ base(2))
    }

    "Suspend" in {
      S.Suspend(Eval.later(base))(1) must_== base(1)
    }
  }
}
