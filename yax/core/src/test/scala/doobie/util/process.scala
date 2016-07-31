package doobie.util

import doobie.imports._
import doobie.util.process.repeatEvalChunks

import org.scalacheck.Prop.forAll

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.Predef._

object processspec extends Specification with ScalaCheck {

  "repeatEvalChunks must" >> {

    "yield the same result irrespective of chunk size" ! forAll { (n0: Int) =>
      val dataSize  = 1000
      val chunkSize = (n0 % dataSize).abs max 1
      val data      = Seq.fill(dataSize)(util.Random.nextInt)
      val fa = {
        var temp = data
        IOLite.primitive {
          val (h, t) = temp.splitAt(chunkSize)
          temp = t
          h
        }
      }
      val result = repeatEvalChunks(fa).runLog.unsafePerformIO
      result must_== data
    }

  }

}
