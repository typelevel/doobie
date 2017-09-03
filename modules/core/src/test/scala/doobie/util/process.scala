package doobie
package util

import cats.effect.IO
import doobie.util.stream.repeatEvalChunks
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import scala.Predef._
import scala.util.Random

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Var"))
object processspec extends Specification with ScalaCheck {

  "repeatEvalChunks must" >> {

    "yield the same result irrespective of chunk size" ! forAll { (n0: Int) =>
      val dataSize  = 1000
      val chunkSize = (n0 % dataSize).abs max 1
      val data      = Seq.fill(dataSize)(Random.nextInt)
      val fa = {
        var temp = data
        IO {
          val (h, t) = temp.splitAt(chunkSize)
          temp = t
          h
        }
      }
      val result = repeatEvalChunks(fa).runLog.unsafeRunSync
      result must_== data
    }

  }

}
