package doobie
package util

import scalaz._
import Scalaz._

import argonaut._
import Argonaut._

object TreeInstances {

  implicit def treeEncode[A: EncodeJson]: EncodeJson[Tree[A]] =
    EncodeJson(t => (t.rootLabel, t.subForest).asJson)

  implicit def treeDecode[A: DecodeJson]: DecodeJson[Tree[A]] =
    DecodeJson(c =>
      for {
        rootLabel <- (c =\ 0).as[A]
        subForest <- (c =\ 1).as[Stream[Tree[A]]]
      } yield Tree.node(rootLabel, subForest)
    )

}