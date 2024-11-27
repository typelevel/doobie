// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package util

import cats.effect.IO
import doobie.util.stream.repeatEvalChunks
import munit.CatsEffectAssertions.MUnitCatsAssertionsForIOOps
import org.scalacheck.Prop.forAll
import org.scalacheck.effect.PropF

import scala.Predef.*
import scala.util.Random

class ProcessSuite extends munit.ScalaCheckSuite {
  
  test("repeatEvalChunks must yield the same result irrespective of chunk size") {
    PropF.forAllF { (n0: Int) =>
      val dataSize = 1000
      val chunkSize = (n0 % dataSize).abs max 1
      val data = Seq.fill(dataSize)(Random.nextInt())
      val fa = {
        var temp = data
        IO {
          val (h, t) = temp.splitAt(chunkSize)
          temp = t
          h
        }
      }
      repeatEvalChunks(fa).compile.toVector.assertEquals(data.toVector)
    }
  }

}
