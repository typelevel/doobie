#+cats
// relies on whenM, etc. so no cats for now
#-cats
#+scalaz
package doobie.example

import doobie.imports._
import doobie.postgres.imports._


import java.io.File

/**
 * Example of using the high-level Large Object API. See the Postgres JDBC driver doc and the
 * source in doobie.contrib.postgresql for more information.
 */
object PostgresLargeObject {

  val xa = Transactor.fromDriverManager[IOLite]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

  val prog: LargeObjectManagerIO[Long] =
    for {
      oid <- PHLOM.createLOFromFile(1024, new File("world.sql"))
      _   <- PHLOM.createFileFromLO(1024, oid, new File("world2.sql"))
      _   <- PHLOM.delete(oid)
    } yield oid

  val task: IOLite[Unit] =

    PHC.pgGetLargeObjectAPI(prog).transact(xa).flatMap { oid =>
      IOLite.primitive(Console.println("oid was " + oid))
    }

  def main(args: Array[String]): Unit =
    task.unsafePerformIO

}
#-scalaz
