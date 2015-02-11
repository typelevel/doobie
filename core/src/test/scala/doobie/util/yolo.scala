package doobie.util

import scalaz._, Scalaz._, scalaz.concurrent.Task
import doobie.imports._
import doobie.util.yolo._
import org.specs2.mutable.Specification

object yolospec extends Specification {

  // Kind of a bogus test; just checking for compilation
  "YOLO checks" should {
    "compile for Query, Query0, Update, Update0" in {
      lazy val dontRun = {
        val y = new Yolo[Task](null); import y._
        Query0[Int]("", None).check
        Query[Int, Int]("", None).check
        Update0("", None).check
        Update[Int]("", None).check
      }
      true
    }
  }

}