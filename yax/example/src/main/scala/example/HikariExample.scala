package doobie.example


import doobie.imports._
import doobie.hikari.hikaritransactor._

object HikariExample {

  def tmain: IOLite[Unit] = 
    for {
      xa <- HikariTransactor[IOLite]("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
      _  <- FreeUsage.examples.transact(xa)
      _  <- xa.shutdown
    } yield ()

  def main(args: Array[String]): Unit =
    tmain.unsafePerformIO

}
