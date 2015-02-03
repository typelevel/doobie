package doobie.example

import scalaz._, Scalaz._, scalaz.concurrent.Task

import doobie.imports._
import doobie.contrib.h2.h2transactor._

object H2TransactorExample {

  def tmain: Task[Unit] = 
    for {
      xa <- H2Transactor[Task]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
      _  <- FirstExample.examples.transact(xa)
      _  <- xa.dispose
    } yield ()

  def main(args: Array[String]) =
    tmain.run

}

