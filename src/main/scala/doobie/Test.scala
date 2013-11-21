package doobie

import scalaz._
import Scalaz._
import scalaz.concurrent._
import scalaz.stream._
import scalaz.stream.Process._

object Test {

  val converter: Task[Vector[String]] =
    io.linesR("/usr/local/dict/words")
      .filter(_.length > 10) 
      .flatMap(s => if (s.distinct.length == s.length) emitO(s) else tell("."))
      .drainW(io.stdOut)
      .take(20)
      .runLog
      .map(_.toVector) // straighten the types out

  def tmain: Task[Unit] =
    converter >>= (_.traverse_(putStrLn))

  def putStrLn(s: String): Task[Unit] =
    Task.delay(println(s))

  def main(args: Array[String]) =
    tmain.run

}

