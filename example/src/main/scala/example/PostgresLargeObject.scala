package doobie.example

import doobie.imports._
import doobie.contrib.postgresql.imports._

import java.io.File

import scalaz._, Scalaz._, scalaz.concurrent.Task

/** 
 * Example of using the high-level Large Object API. See the Postgres JDBC driver doc and the 
 * source in doobie.contrib.postgresql for more information.
 */
object PostgresLargeObject {

  val xa: Transactor[Task] = 
    DriverManagerTransactor("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

  val prog: LargeObjectManagerIO[Long] =
    for {
      oid <- PHLOM.createLOFromFile(1024, new File("world.sql"))
      _   <- PHLOM.createFileFromLO(1024, oid, new File("world2.sql"))
      _   <- PHLOM.delete(oid)
    } yield oid

  val task: Task[Unit] =
    PHC.pgGetLargeObjectAPI(prog).transact(xa) >>= { oid => 
      Task.delay(Console.println("oid was " + oid))
    }

  def main(args: Array[String]): Unit = 
    task.unsafePerformSync

}

